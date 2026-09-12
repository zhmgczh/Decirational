package decirational

import "fmt"

// Token is anything the lexer can emit and the parser can match against.
type Token interface {
	TypeCode() byte
}

// OperandType is the kind of a numeric literal token.
type OperandType byte

const (
	OperandInteger      OperandType = 'i'
	OperandLargeInteger OperandType = 'l'
	OperandRational     OperandType = 'r'
)

func (t OperandType) TypeCode() byte { return byte(t) }
func (t OperandType) String() string {
	switch t {
	case OperandInteger:
		return "INTEGER"
	case OperandLargeInteger:
		return "LARGE_INTEGER"
	case OperandRational:
		return "RATIONAL"
	default:
		return "?"
	}
}

// OperatorKind is a binary arithmetic operator.
type OperatorKind byte

const (
	OpPlus           OperatorKind = '+'
	OpMinus          OperatorKind = '-'
	OpMultiplication OperatorKind = '*'
	OpDivision       OperatorKind = '/'
	// OpIntegerDivision has no single lexable character of its own (its
	// syntax is the two-character "//"), so its underlying byte is just an
	// identity value, never looked up from input; String() below overrides
	// the display text to the real "//" syntax.
	OpIntegerDivision OperatorKind = '\\'
	OpModulo          OperatorKind = '%'
	OpPower           OperatorKind = '^'
)

func (o OperatorKind) TypeCode() byte { return byte(o) }
func (o OperatorKind) String() string {
	if o == OpIntegerDivision {
		return "//"
	}
	return fmt.Sprintf("%c", byte(o))
}

// ParenKind is a parenthesis token.
type ParenKind byte

const (
	LeftParen  ParenKind = '('
	RightParen ParenKind = ')'
)

func (p ParenKind) TypeCode() byte { return byte(p) }
func (p ParenKind) String() string { return fmt.Sprintf("%c", byte(p)) }

// FloorKind is a floor-bracket token.
type FloorKind byte

const (
	LeftFloor  FloorKind = '['
	RightFloor FloorKind = ']'
)

func (f FloorKind) TypeCode() byte { return byte(f) }
func (f FloorKind) String() string { return fmt.Sprintf("%c", byte(f)) }

// AbsoluteKind is the absolute-value bar token. The same bar character opens
// and closes an absolute-value group; which is which is resolved by the
// parser's grammar position.
type AbsoluteKind byte

const AbsoluteBar AbsoluteKind = '|'

func (a AbsoluteKind) TypeCode() byte { return byte(a) }
func (a AbsoluteKind) String() string { return fmt.Sprintf("%c", byte(a)) }

// Operand is a numeric literal token: a value is parsed as a plain machine
// integer when it fits, else as T, else as Rational[T].
type Operand[T CustomInteger[T]] struct {
	Type  OperandType
	Large T           // populated when Type is OperandInteger or OperandLargeInteger
	Rat   Rational[T] // populated when Type is OperandRational
}

func (o Operand[T]) TypeCode() byte { return o.Type.TypeCode() }
func (o Operand[T]) String() string {
	if o.Type == OperandRational {
		return fmt.Sprintf("%s(%s)", o.Type, o.Rat)
	}
	return fmt.Sprintf("%s(%s)", o.Type, o.Large)
}
