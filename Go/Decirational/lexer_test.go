package decirational

import "testing"

func newTestLexer() Lexer[TightInteger] {
	return NewLexer[TightInteger](NewTightIntegerFromInt32, ParseTightInteger)
}

func mustTokens(t *testing.T, lexer Lexer[TightInteger], expr string) []Token {
	t.Helper()
	tokens, err := lexer.GetTokens(expr)
	if err != nil {
		t.Fatalf("GetTokens(%q): %v", expr, err)
	}
	return tokens
}

func TestLexerEmptyInput(t *testing.T) {
	lexer := newTestLexer()
	if tokens, err := lexer.GetTokens(""); err != nil || len(tokens) != 0 {
		t.Errorf("empty expression yields no tokens: %v, %v", tokens, err)
	}
	if tokens, err := lexer.GetTokens("   "); err != nil || len(tokens) != 0 {
		t.Errorf("whitespace-only expression yields no tokens: %v, %v", tokens, err)
	}
}

func TestLexerWhitespaceStripped(t *testing.T) {
	lexer := newTestLexer()
	a := mustTokens(t, lexer, "1+2")
	b := mustTokens(t, lexer, " 1 + 2 ")
	if len(a) != len(b) {
		t.Fatalf("whitespace should not change token count: %d vs %d", len(a), len(b))
	}
	for i := range a {
		if a[i].TypeCode() != b[i].TypeCode() {
			t.Errorf("token %d differs after stripping whitespace", i)
		}
	}
}

func TestLexerBasicTokenization(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "12+3*4")
	if len(tokens) != 5 {
		t.Fatalf("12+3*4 should have 5 tokens, got %d", len(tokens))
	}
	if tokens[0].TypeCode() != 'i' {
		t.Error("first token is an integer operand")
	}
	if tokens[1] != Token(OpPlus) {
		t.Error("second token is PLUS")
	}
	if tokens[3] != Token(OpMultiplication) {
		t.Error("fourth token is MULTIPLICATION")
	}
}

func TestLexerIntegerBoundary(t *testing.T) {
	lexer := newTestLexer()
	if got := mustTokens(t, lexer, "2147483647")[0].TypeCode(); got != 'i' {
		t.Errorf("Integer.MAX_VALUE parses as plain integer, got type %c", got)
	}
	if got := mustTokens(t, lexer, "2147483648")[0].TypeCode(); got != 'l' {
		t.Errorf("Integer.MAX_VALUE+1 parses as large integer, got type %c", got)
	}
	if got := mustTokens(t, lexer, "99999999999999999999")[0].TypeCode(); got != 'l' {
		t.Errorf("20-digit literal parses as large integer, got type %c", got)
	}
}

func TestLexerDecimalAndCyclicLiterals(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "3.14")
	if len(tokens) != 1 || tokens[0].TypeCode() != 'r' {
		t.Errorf("decimal literal is a single rational token: %v", tokens)
	}
	tokens = mustTokens(t, lexer, "0.{3}")
	if len(tokens) != 1 || tokens[0].TypeCode() != 'r' {
		t.Errorf("cyclic decimal literal is a single rational token: %v", tokens)
	}
}

func TestLexerFractionBarSplitsTokens(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "1/2")
	if len(tokens) != 3 {
		t.Fatalf("1/2 should split into operand, DIVISION, operand: got %d tokens", len(tokens))
	}
	if tokens[1] != Token(OpDivision) {
		t.Error("middle token of 1/2 is DIVISION")
	}
}

func TestLexerStructuralTokens(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "([|1|])")
	if len(tokens) != 7 {
		t.Fatalf("([|1|]) should have 7 tokens, got %d", len(tokens))
	}
	wantCodes := []byte{'(', '[', '|', 'i', '|', ']', ')'}
	for i, want := range wantCodes {
		if tokens[i].TypeCode() != want {
			t.Errorf("token %d: got type %c, want %c", i, tokens[i].TypeCode(), want)
		}
	}
}

func TestLexerRepeatedOperatorChars(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "++")
	if len(tokens) != 2 || tokens[0] != Token(OpPlus) || tokens[1] != Token(OpPlus) {
		t.Errorf("++ should be two separate PLUS tokens: %v", tokens)
	}
}

func TestLexerOperandFollowedByParen(t *testing.T) {
	lexer := newTestLexer()
	tokens := mustTokens(t, lexer, "12(3)")
	if len(tokens) != 4 {
		t.Errorf("12(3) should split into operand, paren, operand, paren: got %d", len(tokens))
	}
}

func TestLexerIllegalCharacter(t *testing.T) {
	lexer := newTestLexer()
	for _, expr := range []string{"2#3", "5!"} {
		if _, err := lexer.GetTokens(expr); err == nil {
			t.Errorf("expected an error tokenizing %q", expr)
		}
	}
}
