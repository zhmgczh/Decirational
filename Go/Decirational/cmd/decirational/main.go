// Command decirational is the Go counterpart of the Java Main entry point:
// a REPL that reads one arithmetic expression per line from standard input
// and prints its value.
package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"

	dec "github.com/zhmgczh/Decirational"
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
		err = run(dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger, format, precision, os.Stdin, os.Stdout)
	case "tight":
		err = run(dec.NewTightIntegerFromInt32, dec.ParseTightInteger, format, precision, os.Stdin, os.Stdout)
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

// run reads one arithmetic expression per line from in and writes its value
// (or an "Error: ..." line) to out, matching Java's Scanner/nextLine and
// Rust's BufRead::lines: an expression line is read in full no matter how
// long it is, bounded only by available memory.
//
// This deliberately does NOT use bufio.Scanner: its default Buffer caps a
// single line at bufio.MaxScanTokenSize (64 KiB), and exceeding that makes
// Scan() return false as if the input had simply ended - silently dropping
// the oversized line (and every line after it) with no error and an exit
// code of 0, unless the caller separately checks Scanner.Err(). Java and
// Rust have no such cap, so a long expression that they evaluate normally
// would previously vanish here.  bufio.Reader.ReadString has no analogous
// limit: it grows its buffer to fit whatever it reads.
func run[T dec.CustomInteger[T]](fromInt32 func(int32) T, parseInt func(string) (T, error), format string, precision int32, in io.Reader, out io.Writer) error {
	formatter, err := makeFormatter[T](format, precision)
	if err != nil {
		return err
	}
	lexer := dec.NewLexer[T](fromInt32, parseInt)
	parser := dec.NewParser[T]()
	reader := bufio.NewReader(in)
	for {
		line, readErr := reader.ReadString('\n')
		expression := strings.TrimRight(line, "\r\n")
		if strings.TrimSpace(expression) != "" {
			output, err := evalLine(lexer, parser, formatter, expression)
			if err != nil {
				fmt.Fprintln(out, "Error: "+err.Error())
			} else {
				fmt.Fprintln(out, output)
			}
		}
		if readErr != nil {
			if readErr == io.EOF {
				return nil
			}
			return readErr
		}
	}
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
