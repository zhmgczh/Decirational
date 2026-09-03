// Command decirational is the Go counterpart of the Java Main entry point:
// a REPL that reads one arithmetic expression per line from standard input
// and prints its value.
package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"

	dec "decirational"
)

const usage = `Usage: decirational [--integer=decimal|tight] [--format=<format>] [--precision=N]

  --integer=decimal   use DecimalInteger for large integers (default)
  --integer=tight     use TightInteger for large integers

  --format=default    Rational's default string form (default): fraction, or plain integer when the denominator is 1
  --format=fraction   original (improper) fraction: always numerator/denominator
  --format=mixed      mixed number: whole part and proper fraction, e.g. 1 3/4
  --format=decimal    decimal expansion, with repeating digits in {}
  --format=truncate   decimal expansion truncated to --precision digits after the point
  --format=round      decimal expansion rounded to --precision digits after the point
  --format=ceil       decimal expansion rounded up (toward +infinity) to --precision digits after the point
  --format=floor      decimal expansion rounded down (toward -infinity) to --precision digits after the point

  --precision=N       digits after the decimal point for truncate/round/ceil/floor (default 0);
                       ignored by default/fraction/mixed/decimal. N may be negative to round to
                       tens, hundreds, etc. before the point.

Reads one arithmetic expression per line from standard input and prints its value.`

func main() {
	integerType := "decimal"
	format := "default"
	var precision int32 = 0

	for _, arg := range os.Args[1:] {
		switch {
		case arg == "--help" || arg == "-h":
			fmt.Println(usage)
			return
		case strings.HasPrefix(arg, "--integer="):
			integerType = strings.TrimPrefix(arg, "--integer=")
		case strings.HasPrefix(arg, "--format="):
			format = strings.TrimPrefix(arg, "--format=")
		case strings.HasPrefix(arg, "--precision="):
			value := strings.TrimPrefix(arg, "--precision=")
			// Parsed as int32 (not Go's native, wider int) so a value Java's
			// `Integer.parseInt`/Rust's `i32` parse would reject as out of
			// range is rejected here too, instead of silently accepted and
			// then attempted (which previously hung on an astronomically
			// large computation for exactly such an input).
			n, err := strconv.ParseInt(value, 10, 32)
			if err != nil {
				fail("Invalid --precision value: " + value)
			}
			precision = int32(n)
		default:
			fail("Unknown argument: " + arg)
		}
	}

	var err error
	switch integerType {
	case "decimal":
		err = run(dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger, format, precision)
	case "tight":
		err = run(dec.NewTightIntegerFromInt32, dec.ParseTightInteger, format, precision)
	default:
		err = fmt.Errorf("unknown integer type: %s (expected 'decimal' or 'tight')", integerType)
	}
	if err != nil {
		fail(err.Error())
	}
}

func fail(message string) {
	fmt.Fprintln(os.Stderr, message)
	fmt.Fprintln(os.Stderr, usage)
	os.Exit(1)
}

func run[T dec.CustomInteger[T]](fromInt32 func(int32) T, parseInt func(string) (T, error), format string, precision int32) error {
	formatter, err := makeFormatter[T](format, precision)
	if err != nil {
		return err
	}
	lexer := dec.NewLexer[T](fromInt32, parseInt)
	parser := dec.NewParser[T]()
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		expression := scanner.Text()
		if strings.TrimSpace(expression) == "" {
			continue
		}
		output, err := evalLine(lexer, parser, formatter, expression)
		if err != nil {
			fmt.Println("Error: " + err.Error())
			continue
		}
		fmt.Println(output)
	}
	return nil
}

// evalLine covers the same scope as Java's per-line try/catch: tokenizing,
// parsing, AND formatting. Parser.Parse already recovers panics raised
// during tokenizing/parsing on its own, but formatting a successfully-parsed
// result (e.g. an absurd --precision) can still panic, and that call sits
// outside Parser.Parse - so this needs its own recover too, or such a panic
// would crash the whole REPL instead of reporting one bad line and
// continuing, exactly as it did before this function existed.
func evalLine[T dec.CustomInteger[T]](lexer dec.Lexer[T], parser *dec.Parser[T], formatter func(dec.Rational[T]) string, expression string) (result string, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("%v", r)
		}
	}()
	tokens, err := lexer.GetTokens(expression)
	if err != nil {
		return "", err
	}
	value, err := parser.Parse(tokens)
	if err != nil {
		return "", err
	}
	return formatter(value), nil
}

func makeFormatter[T dec.CustomInteger[T]](format string, precision int32) (func(dec.Rational[T]) string, error) {
	switch format {
	case "default":
		return func(r dec.Rational[T]) string { return r.String() }, nil
	case "fraction":
		return func(r dec.Rational[T]) string { return r.ToFractionString() }, nil
	case "mixed":
		return func(r dec.Rational[T]) string { return r.ToMixedString() }, nil
	case "decimal":
		return func(r dec.Rational[T]) string { return r.ToDecimalString() }, nil
	case "truncate":
		return func(r dec.Rational[T]) string { return r.ToTruncateDecimalString(precision) }, nil
	case "round":
		return func(r dec.Rational[T]) string { return r.ToRoundDecimalString(precision) }, nil
	case "ceil":
		return func(r dec.Rational[T]) string { return r.ToCeilDecimalString(precision) }, nil
	case "floor":
		return func(r dec.Rational[T]) string { return r.ToFloorDecimalString(precision) }, nil
	default:
		return nil, fmt.Errorf("unknown format: %s (expected default, fraction, mixed, decimal, truncate, round, ceil, or floor)", format)
	}
}
