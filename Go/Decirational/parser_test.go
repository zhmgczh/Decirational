package decirational

import "testing"

func evalD(t *testing.T, expr string) string {
	t.Helper()
	lexer := NewLexer[TightInteger](NewTightIntegerFromInt32, ParseTightInteger)
	tokens, err := lexer.GetTokens(expr)
	if err != nil {
		t.Fatalf("GetTokens(%q): %v", expr, err)
	}
	parser := NewParser[TightInteger]()
	result, err := parser.Parse(tokens)
	if err != nil {
		t.Fatalf("Parse(%q): %v", expr, err)
	}
	return result.ToDecimalString()
}

func TestParserPrecedenceAndAssociativity(t *testing.T) {
	cases := map[string]string{
		"2+3*4":       "14",
		"(2+3)*4":     "20",
		"10%3":        "1",
		"2^10":        "1024",
		"2^-2":        "0.25",
		"2^3^2":       "512",
		"-2^2":        "-4",
		"(-2)^2":      "4",
		"((1+2)*3)^2": "81",
	}
	for expr, want := range cases {
		if got := evalD(t, expr); got != want {
			t.Errorf("eval(%q) = %s, want %s", expr, got, want)
		}
	}
}

func TestParserUnaryOperators(t *testing.T) {
	cases := map[string]string{
		"-3+5": "2", "+3-1": "2", "5++3": "8", "5--3": "8", "--5": "5", "5-+3": "2",
	}
	for expr, want := range cases {
		if got := evalD(t, expr); got != want {
			t.Errorf("eval(%q) = %s, want %s", expr, got, want)
		}
	}
}

func TestParserFloor(t *testing.T) {
	cases := map[string]string{
		"[3.7]": "3", "[-3.7]": "-4", "[5]": "5", "[-5]": "-5", "[0]": "0",
	}
	for expr, want := range cases {
		if got := evalD(t, expr); got != want {
			t.Errorf("eval(%q) = %s, want %s", expr, got, want)
		}
	}
}

func TestParserAbsoluteValue(t *testing.T) {
	cases := map[string]string{
		"|-5|": "5", "|5|": "5", "|3-10|": "7", "|3-10|*2": "14",
	}
	for expr, want := range cases {
		if got := evalD(t, expr); got != want {
			t.Errorf("eval(%q) = %s, want %s", expr, got, want)
		}
	}
}

func TestParserNestingFloorAbsolute(t *testing.T) {
	if got := evalD(t, "|[-7.5]|"); got != "8" {
		t.Errorf("absolute value of a floor: %s", got)
	}
	if got := evalD(t, "[|-7.5|]"); got != "7" {
		t.Errorf("floor of an absolute value: %s", got)
	}
}

func TestParserBigIntegers(t *testing.T) {
	if got := evalD(t, "123456789012345678901234567890*2"); got != "246913578024691357802469135780" {
		t.Errorf("big-integer multiplication: %s", got)
	}
}

func TestParserRationalLiterals(t *testing.T) {
	if got := evalD(t, "0.{3}"); got != "0.{3}" {
		t.Errorf("cyclic decimal literal passes through unchanged: %s", got)
	}
	if got := evalD(t, "100/7"); got != "14.{285714}" {
		t.Errorf("division producing a repeating decimal: %s", got)
	}
	if got := evalD(t, "1/3+1/6"); got != "0.5" {
		t.Errorf("fraction sum reduces to a terminating decimal: %s", got)
	}
}

func TestParserModulo(t *testing.T) {
	if got := evalD(t, "-10%3"); got != "-1" {
		t.Errorf("modulo follows the dividend's sign: %s", got)
	}
	lexer := NewLexer[TightInteger](NewTightIntegerFromInt32, ParseTightInteger)
	parser := NewParser[TightInteger]()
	tokens, _ := lexer.GetTokens("5%2.5")
	if _, err := parser.Parse(tokens); err == nil {
		t.Error("modulo of a non-integer should be rejected")
	}
}

func TestParserIntegerDivision(t *testing.T) {
	cases := map[string]string{
		"7//2":      "3",
		"-7//2":     "-3",
		"7//-2":     "-3",
		"-7//-2":    "3",
		"100//7":    "14",
		"100//3//2": "16", // left-associative
		"2+3//2":    "3",  // // binds tighter than +
		"2*3//2":    "3",  // same precedence as *, left to right
		"(1+2)//2":  "1",
	}
	for expr, want := range cases {
		if got := evalD(t, expr); got != want {
			t.Errorf("eval(%q) = %s, want %s", expr, got, want)
		}
	}
	// (a//b)*b + (a%b) == a, matching DivideByAndModulo's quotient/remainder pair.
	if got := evalD(t, "(-7//2)*2+(-7%2)"); got != "-7" {
		t.Errorf("integer division/modulo identity: got %s", got)
	}

	lexer := NewLexer[TightInteger](NewTightIntegerFromInt32, ParseTightInteger)
	evalErr := func(expr string) error {
		tokens, err := lexer.GetTokens(expr)
		if err != nil {
			return err
		}
		parser := NewParser[TightInteger]()
		_, err = parser.Parse(tokens)
		return err
	}
	if err := evalErr("7.5//2"); err == nil {
		t.Error("integer division of a non-integer should be rejected")
	}
	if err := evalErr("5//0"); err == nil {
		t.Error("integer division by zero should be rejected")
	}
}

func TestLexerIntegerDivisionSlashPairing(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "///")
	if len(tokens) != 2 || tokens[0] != Token(OpIntegerDivision) || tokens[1] != Token(OpDivision) {
		t.Errorf("odd run of '/' should greedily pair into // then a lone /: %v", tokens)
	}
	tokens = mustTokens(t, lexer, "////")
	if len(tokens) != 2 || tokens[0] != Token(OpIntegerDivision) || tokens[1] != Token(OpIntegerDivision) {
		t.Errorf("even run of '/' should greedily pair into // tokens: %v", tokens)
	}
}

func TestParserErrorCases(t *testing.T) {
	lexer := NewLexer[TightInteger](NewTightIntegerFromInt32, ParseTightInteger)
	evalErr := func(expr string) error {
		tokens, err := lexer.GetTokens(expr)
		if err != nil {
			return err
		}
		parser := NewParser[TightInteger]()
		_, err = parser.Parse(tokens)
		return err
	}
	exprs := []string{
		"1/0", "5/(2-2)", "2^2.5", "2^99999999999999999999",
		"2+", "(1+2", "()", "2(3)", "[5", "|5", "2#3",
	}
	for _, expr := range exprs {
		if err := evalErr(expr); err == nil {
			t.Errorf("expected an error evaluating %q", expr)
		}
	}
	parser := NewParser[TightInteger]()
	if _, err := parser.Parse(nil); err == nil {
		t.Error("empty token list should be rejected")
	}
}
