package decirational

import (
	"fmt"
	"strconv"
)

// Lexer[T] tokenizes an expression string: every structural character (one
// of "()[]|+-*/%^") is always its own token, and every maximal run of number
// characters (digits, '.', '{', '}') becomes one operand token.
type Lexer[T CustomInteger[T]] struct {
	fromInt32 func(int32) T
	parseInt  func(string) (T, error)
}

// NewLexer builds a Lexer that constructs integer operands via fromInt32
// (for literals that fit a 32-bit int) and parseInt (for larger literals and
// as the fallback used to build the numerator/denominator of a rational
// literal).
func NewLexer[T CustomInteger[T]](fromInt32 func(int32) T, parseInt func(string) (T, error)) Lexer[T] {
	return Lexer[T]{fromInt32, parseInt}
}

func isNumberChar(c byte) bool {
	return isDigit(c) || isDecimalPoint(c) || isCyclicBeginChar(c) || isCyclicEndChar(c)
}

func (l Lexer[T]) parseOperand(value string) (Operand[T], error) {
	if n, err := strconv.ParseInt(value, 10, 32); err == nil {
		return Operand[T]{Type: OperandInteger, Large: l.fromInt32(int32(n))}, nil
	}
	if large, err := l.parseInt(value); err == nil {
		return Operand[T]{Type: OperandLargeInteger, Large: large}, nil
	}
	rat, err := ParseRational(value, l.parseInt)
	if err != nil {
		return Operand[T]{}, fmt.Errorf("%s is not a valid rational", value)
	}
	return Operand[T]{Type: OperandRational, Rat: rat}, nil
}

// GetTokens tokenizes expression, returning an empty slice for a nil/blank input.
func (l Lexer[T]) GetTokens(expression string) ([]Token, error) {
	expression = stripWhitespace(expression)
	if expression == "" {
		return []Token{}, nil
	}
	tokens := make([]Token, 0, len(expression))
	i := 0
	for i < len(expression) {
		c := expression[i]
		switch c {
		case '(':
			tokens = append(tokens, LeftParen)
			i++
		case ')':
			tokens = append(tokens, RightParen)
			i++
		case '[':
			tokens = append(tokens, LeftFloor)
			i++
		case ']':
			tokens = append(tokens, RightFloor)
			i++
		case '|':
			tokens = append(tokens, AbsoluteBar)
			i++
		case '+':
			tokens = append(tokens, OpPlus)
			i++
		case '-':
			tokens = append(tokens, OpMinus)
			i++
		case '*':
			tokens = append(tokens, OpMultiplication)
			i++
		case '/':
			if i+1 < len(expression) && expression[i+1] == '/' {
				tokens = append(tokens, OpIntegerDivision)
				i += 2
			} else {
				tokens = append(tokens, OpDivision)
				i++
			}
		case '%':
			tokens = append(tokens, OpModulo)
			i++
		case '^':
			tokens = append(tokens, OpPower)
			i++
		default:
			if !isNumberChar(c) {
				return nil, fmt.Errorf("illegal character %c in expression: %s", c, expression)
			}
			j := i
			for j < len(expression) && isNumberChar(expression[j]) {
				j++
			}
			operand, err := l.parseOperand(expression[i:j])
			if err != nil {
				return nil, err
			}
			tokens = append(tokens, operand)
			i = j
		}
	}
	return tokens, nil
}
