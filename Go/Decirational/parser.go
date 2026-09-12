package decirational

import (
	"fmt"
	"strconv"
)

// Parser[T] is a recursive-descent evaluator over a token stream. Implements
// this grammar:
//
//	expression := term (('+'|'-') term)*
//	term       := unary (('*'|'/'|'%') unary)*
//	unary      := ('+'|'-')* power
//	power      := primary ('^' unary)?              // right-associative
//	primary    := operand | '(' expression ')' | '[' expression ']' (floor)
//	            | '|' expression '|' (absolute value)
//
// maxExpressionDepth bounds recursion in parseUnary, the one point every
// recursive production in this grammar passes through at least once per
// level of nesting: directly for a chain of unary +/- (parseUnary calling
// itself), for bracket/floor/absolute-value nesting (via the
// expression->term->unary->power->primary chain that runs once per level
// before the next '(', '[' or '|'), and for right-associative '^' chains
// (parsePower calling parseUnary for its exponent, which can lead straight
// back into parsePower). A goroutine's stack grows dynamically but still has
// a ceiling - a deeply nested or chained expression overflows it with an
// uncatchable "fatal error: stack overflow" (not a panic - recover cannot
// intercept it, so the whole process dies) well before this limit, so this
// is checked well short of that: crafting a one-line expression this deep is
// trivial for an attacker, and no legitimate expression needs anywhere near
// it.
const maxExpressionDepth = 1000

type Parser[T CustomInteger[T]] struct {
	tokens []Token
	pos    int
	depth  int
}

// NewParser creates a Parser. The type parameter fixes which CustomInteger
// backend operand tokens are expected to carry.
func NewParser[T CustomInteger[T]]() *Parser[T] {
	return &Parser[T]{}
}

// Parse evaluates tokens to a single Rational[T], recovering any panic raised
// by the underlying arithmetic (e.g. division by zero) into a returned error,
// so callers only ever need to check one error value.
func (p *Parser[T]) Parse(tokens []Token) (result Rational[T], err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("%v", r)
		}
	}()
	if len(tokens) == 0 {
		return Rational[T]{}, fmt.Errorf("no tokens to parse")
	}
	p.tokens = tokens
	p.pos = 0
	p.depth = 0
	result, err = p.parseExpression()
	if err != nil {
		return Rational[T]{}, err
	}
	if p.pos != len(p.tokens) {
		return Rational[T]{}, fmt.Errorf("unexpected token: %v", p.tokens[p.pos])
	}
	return result, nil
}

func (p *Parser[T]) peek() Token {
	if p.pos < len(p.tokens) {
		return p.tokens[p.pos]
	}
	return nil
}

func (p *Parser[T]) advance() Token {
	t := p.tokens[p.pos]
	p.pos++
	return t
}

func (p *Parser[T]) check(expected Token) bool {
	t := p.peek()
	return t != nil && t == expected
}

func (p *Parser[T]) expect(expected Token) error {
	if !p.check(expected) {
		found := p.peek()
		if found == nil {
			return fmt.Errorf("expected %v but found end of input", expected)
		}
		return fmt.Errorf("expected %v but found %v", expected, found)
	}
	p.pos++
	return nil
}

func (p *Parser[T]) parseExpression() (Rational[T], error) {
	result, err := p.parseTerm()
	if err != nil {
		return Rational[T]{}, err
	}
	for p.check(OpPlus) || p.check(OpMinus) {
		operator := p.advance().(OperatorKind)
		right, err := p.parseTerm()
		if err != nil {
			return Rational[T]{}, err
		}
		if operator == OpPlus {
			result = result.Plus(right)
		} else {
			result = result.Minus(right)
		}
	}
	return result, nil
}

func (p *Parser[T]) parseTerm() (Rational[T], error) {
	result, err := p.parseUnary()
	if err != nil {
		return Rational[T]{}, err
	}
	for p.check(OpMultiplication) || p.check(OpDivision) || p.check(OpIntegerDivision) || p.check(OpModulo) {
		operator := p.advance().(OperatorKind)
		right, err := p.parseUnary()
		if err != nil {
			return Rational[T]{}, err
		}
		switch operator {
		case OpMultiplication:
			result = result.Multiply(right)
		case OpDivision:
			result = result.DivideBy(right)
		case OpIntegerDivision:
			result, err = integerDivideRational(result, right)
			if err != nil {
				return Rational[T]{}, err
			}
		case OpModulo:
			result, err = moduloRational(result, right)
			if err != nil {
				return Rational[T]{}, err
			}
		}
	}
	return result, nil
}

func integerDivideRational[T CustomInteger[T]](a, b Rational[T]) (Rational[T], error) {
	if !a.IsInteger() || !b.IsInteger() {
		return Rational[T]{}, fmt.Errorf("integer division is only supported between integers")
	}
	return NewRationalFromInteger(a.GetNumerator().DivideBy(b.GetNumerator())), nil
}

func moduloRational[T CustomInteger[T]](a, b Rational[T]) (Rational[T], error) {
	if !a.IsInteger() || !b.IsInteger() {
		return Rational[T]{}, fmt.Errorf("modulo is only supported between integers")
	}
	return NewRationalFromInteger(a.GetNumerator().Modulo(b.GetNumerator())), nil
}

func (p *Parser[T]) parseUnary() (Rational[T], error) {
	p.depth++
	defer func() { p.depth-- }()
	if p.depth > maxExpressionDepth {
		return Rational[T]{}, fmt.Errorf("expression nested too deeply (max depth %d)", maxExpressionDepth)
	}
	if p.check(OpPlus) {
		p.advance()
		return p.parseUnary()
	}
	if p.check(OpMinus) {
		p.advance()
		r, err := p.parseUnary()
		if err != nil {
			return Rational[T]{}, err
		}
		return r.Negate(), nil
	}
	return p.parsePower()
}

func (p *Parser[T]) parsePower() (Rational[T], error) {
	base, err := p.parsePrimary()
	if err != nil {
		return Rational[T]{}, err
	}
	if p.check(OpPower) {
		p.advance()
		exponent, err := p.parseUnary()
		if err != nil {
			return Rational[T]{}, err
		}
		e, err := toIntExponent(exponent)
		if err != nil {
			return Rational[T]{}, err
		}
		return base.Pow(e), nil
	}
	return base, nil
}

// toIntExponent parses the exponent as int32 specifically (not Go's native,
// wider int) so a value out of int32 range is rejected here rather than
// silently accepted and attempted (which previously hung/OOMed on an
// astronomically large computation for exactly such an input).
func toIntExponent[T CustomInteger[T]](exponent Rational[T]) (int32, error) {
	if !exponent.IsInteger() {
		return 0, fmt.Errorf("exponent must be an integer: %v", exponent)
	}
	n, err := strconv.ParseInt(exponent.GetNumerator().String(), 10, 32)
	if err != nil {
		return 0, fmt.Errorf("exponent out of range: %v", exponent)
	}
	return int32(n), nil
}

func (p *Parser[T]) parsePrimary() (Rational[T], error) {
	tok := p.peek()
	if tok == nil {
		return Rational[T]{}, fmt.Errorf("unexpected end of input")
	}
	if operand, ok := tok.(Operand[T]); ok {
		p.advance()
		return operandToRational(operand), nil
	}
	if tok == LeftParen {
		p.advance()
		result, err := p.parseExpression()
		if err != nil {
			return Rational[T]{}, err
		}
		if err := p.expect(RightParen); err != nil {
			return Rational[T]{}, err
		}
		return result, nil
	}
	if tok == LeftFloor {
		p.advance()
		result, err := p.parseExpression()
		if err != nil {
			return Rational[T]{}, err
		}
		if err := p.expect(RightFloor); err != nil {
			return Rational[T]{}, err
		}
		return NewRationalFromInteger(floorInteger(result)), nil
	}
	if tok == AbsoluteBar {
		p.advance()
		result, err := p.parseExpression()
		if err != nil {
			return Rational[T]{}, err
		}
		if err := p.expect(AbsoluteBar); err != nil {
			return Rational[T]{}, err
		}
		return result.Abs(), nil
	}
	return Rational[T]{}, fmt.Errorf("unexpected token: %v", tok)
}

func operandToRational[T CustomInteger[T]](o Operand[T]) Rational[T] {
	if o.Type == OperandRational {
		return o.Rat
	}
	return NewRationalFromInteger(o.Large)
}

func floorInteger[T CustomInteger[T]](r Rational[T]) T {
	numerator := r.GetNumerator()
	if r.IsInteger() {
		return numerator
	}
	denominator := r.GetDenominator()
	qr := numerator.DivideByAndModulo(denominator)
	quotient := qr[0]
	if numerator.IsNegative() {
		return quotient.Minus(denominator.Pow(0))
	}
	return quotient
}
