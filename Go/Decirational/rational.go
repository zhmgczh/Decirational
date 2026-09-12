package decirational

import (
	"fmt"
	"math"
	"strings"
)

// Rational[T] is an exact fraction over a CustomInteger backend.
type Rational[T CustomInteger[T]] struct {
	numerator   T
	denominator T
}

func (r Rational[T]) GetNumerator() T   { return r.numerator }
func (r Rational[T]) GetDenominator() T { return r.denominator }

func (r Rational[T]) getOne() Rational[T] {
	one := r.denominator.Pow(0)
	return Rational[T]{one, one}
}

func (r Rational[T]) getFive() T {
	one := r.denominator.Pow(0)
	two := one.Plus(one)
	four := two.Plus(two)
	return four.Plus(one)
}

func (r Rational[T]) getTen() T {
	one := r.denominator.Pow(0)
	two := one.Plus(one)
	four := two.Plus(two)
	return four.Multiply(two).Plus(two)
}

func rationalReduced[T CustomInteger[T]](numerator, denominator T) Rational[T] {
	gcd := numerator.Gcd(denominator)
	if denominator.IsNegative() {
		numerator = numerator.Negate()
		denominator = denominator.Negate()
	}
	return Rational[T]{numerator.DivideBy(gcd), denominator.DivideBy(gcd)}
}

func rationalSignNormalized[T CustomInteger[T]](numerator, denominator T) Rational[T] {
	if denominator.IsNegative() {
		numerator = numerator.Negate()
		denominator = denominator.Negate()
	}
	return Rational[T]{numerator, denominator}
}

// NewRational builds a reduced, sign-normalized fraction, rejecting a zero denominator.
func NewRational[T CustomInteger[T]](numerator, denominator T) (Rational[T], error) {
	if denominator.IsZero() {
		return Rational[T]{}, fmt.Errorf("denominator cannot be zero")
	}
	return rationalReduced(numerator, denominator), nil
}

// NewRationalFromInteger builds the fraction integer/1.
func NewRationalFromInteger[T CustomInteger[T]](integer T) Rational[T] {
	return Rational[T]{integer, integer.Pow(0)}
}

// ParseRational parses a fraction ("3/4"), decimal ("0.5"), or repeating-decimal
// ("0.{3}", "1.5{6}") literal into a reduced Rational[T]. parseInt builds a T
// from a plain (unsigned, digits-only) decimal string - supplied explicitly
// since a bare CustomInteger[T] constraint gives no way to require one.
func ParseRational[T CustomInteger[T]](s string, parseInt func(string) (T, error)) (Rational[T], error) {
	s = stripWhitespace(s)
	if s == "" {
		return Rational[T]{}, fmt.Errorf("input is empty")
	}
	negative := false
	start := 0
	if isMinus(s[0]) {
		negative = true
		start = 1
	} else if isPlus(s[0]) {
		start = 1
	}
	if start >= len(s) || !isDigit(s[start]) {
		return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
	}
	fractionBar, decimalPoint, cyclicStart, cyclicEnd := -1, -1, -1, -1
	for i := start + 1; i < len(s); i++ {
		switch {
		case isFractionBar(s[i]):
			if fractionBar != -1 || decimalPoint != -1 {
				return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
			}
			fractionBar = i
		case isDecimalPoint(s[i]):
			if decimalPoint != -1 || fractionBar != -1 || i == len(s)-1 {
				return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
			}
			decimalPoint = i
		case isCyclicBeginChar(s[i]):
			if cyclicStart != -1 || decimalPoint == -1 {
				return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
			}
			cyclicStart = i
		case isCyclicEndChar(s[i]):
			if cyclicStart == -1 || cyclicEnd != -1 || i != len(s)-1 || i-cyclicStart == 1 {
				return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
			}
			cyclicEnd = i
		case !isDigit(s[i]):
			return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
		}
	}

	if decimalPoint == -1 {
		var numeratorStr, denominatorStr string
		if fractionBar == -1 {
			numeratorStr, denominatorStr = s[start:], "1"
		} else if fractionBar != len(s)-1 {
			numeratorStr, denominatorStr = s[start:fractionBar], s[fractionBar+1:]
		} else {
			return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
		}
		numerator, err := parseInt(numeratorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		denominator, err := parseInt(denominatorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		if denominator.IsZero() {
			return Rational[T]{}, fmt.Errorf("denominator cannot be zero")
		}
		if negative {
			numerator = numerator.Negate()
		}
		return rationalReduced(numerator, denominator), nil
	} else if cyclicStart == -1 {
		numeratorStr := s[start:decimalPoint] + s[decimalPoint+1:]
		denominatorStr := "1" + strings.Repeat("0", len(s)-decimalPoint-1)
		numerator, err := parseInt(numeratorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		denominator, err := parseInt(denominatorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		if negative {
			numerator = numerator.Negate()
		}
		return rationalReduced(numerator, denominator), nil
	} else if cyclicEnd != -1 {
		finiteNumeratorStr := s[start:decimalPoint] + s[decimalPoint+1:cyclicStart]
		finiteDenominatorStr := "1" + strings.Repeat("0", cyclicStart-decimalPoint-1)
		cyclicNumeratorStr := s[cyclicStart+1 : cyclicEnd]
		cyclicDenominatorStr := strings.Repeat("9", cyclicEnd-cyclicStart-1) + strings.Repeat("0", cyclicStart-decimalPoint-1)
		finiteNumerator, err := parseInt(finiteNumeratorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		finiteDenominator, err := parseInt(finiteDenominatorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		cyclicNumerator, err := parseInt(cyclicNumeratorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		cyclicDenominator, err := parseInt(cyclicDenominatorStr)
		if err != nil {
			return Rational[T]{}, fmt.Errorf("cannot instantiate a rational from the given integer type: %w", err)
		}
		finite := rationalReduced(finiteNumerator, finiteDenominator)
		cyclic := rationalReduced(cyclicNumerator, cyclicDenominator)
		result := finite.Plus(cyclic)
		if negative {
			result = result.Negate()
		}
		return result, nil
	}
	return Rational[T]{}, fmt.Errorf("the rational string does not have the right format")
}

func (r Rational[T]) String() string {
	if r.denominator.IsOne() {
		return r.numerator.String()
	}
	return r.ToFractionString()
}

func (r Rational[T]) ToFractionString() string {
	return r.numerator.String() + "/" + r.denominator.String()
}

func (r Rational[T]) ToMixedString() string {
	if r.denominator.IsOne() {
		return r.numerator.String()
	}
	wr := r.numerator.Abs().DivideByAndModulo(r.denominator)
	whole, remainder := wr[0], wr[1]
	var sb strings.Builder
	if r.numerator.IsNegative() {
		sb.WriteByte('-')
	}
	if !whole.IsZero() {
		sb.WriteString(whole.String())
		sb.WriteByte(' ')
	}
	sb.WriteString(remainder.String())
	sb.WriteByte('/')
	sb.WriteString(r.denominator.String())
	return sb.String()
}

func (r Rational[T]) ToDecimalString() string {
	ten := r.getTen()
	qr := r.numerator.Abs().DivideByAndModulo(r.denominator)
	wholeInteger, remainder := qr[0], qr[1]
	var decimal strings.Builder
	if r.numerator.IsNegative() {
		decimal.WriteByte('-')
	}
	decimal.WriteString(wholeInteger.String())
	if !remainder.IsZero() {
		decimal.WriteByte('.')
	}
	remainderSeen := map[string]int{remainder.String(): decimal.Len()}
	startingCyclic := -1
	for !remainder.IsZero() && startingCyclic == -1 {
		remainder = remainder.Multiply(ten)
		qr = remainder.DivideByAndModulo(r.denominator)
		digit := qr[0]
		remainder = qr[1]
		decimal.WriteByte(digit.String()[0])
		key := remainder.String()
		if pos, ok := remainderSeen[key]; ok {
			startingCyclic = pos
		}
		remainderSeen[key] = decimal.Len()
	}
	result := decimal.String()
	if startingCyclic != -1 {
		result = result[:startingCyclic] + string(rune(cyclicBegin)) + result[startingCyclic:] + string(rune(cyclicEnd))
	}
	return result
}

func (r Rational[T]) ToTruncateDecimalString(roundTo int32) string {
	// Checked up front, like the math.MinInt32 guards in
	// ToRoundDecimalString and ToCeilDecimalString: math.MinInt32 has no
	// positive counterpart ("the minimum representable precision" isn't
	// representable), so there is nothing meaningful to truncate to and
	// every port rejects it. Checking it before negating anything (rather
	// than negating first and relying on how that happens to overflow)
	// means the rest of this function can just negate roundTo normally.
	if roundTo == math.MinInt32 {
		panic("cannot round to the minimum representable precision")
	}
	ten := r.getTen()
	qr := r.numerator.Abs().DivideByAndModulo(r.denominator)
	wholeInteger := qr[0]
	wholeIntegerStr := wholeInteger.String()
	sign := ""
	if r.numerator.IsNegative() {
		sign = "-"
	}
	negRoundTo := -roundTo
	if len(wholeIntegerStr) <= int(negRoundTo) {
		return sign + "0"
	} else if roundTo < 0 {
		shiftBase := ten.Pow(negRoundTo)
		result := wholeInteger.DivideBy(shiftBase).Multiply(shiftBase)
		return sign + result.String()
	} else if roundTo == 0 {
		return sign + wholeIntegerStr
	}
	remainder := qr[1]
	var decimal strings.Builder
	decimal.WriteString(sign)
	decimal.WriteString(wholeIntegerStr)
	if !remainder.IsZero() {
		decimal.WriteByte('.')
	}
	for i := int32(0); i < roundTo && !remainder.IsZero(); i++ {
		remainder = remainder.Multiply(ten)
		qr = remainder.DivideByAndModulo(r.denominator)
		digit := qr[0]
		remainder = qr[1]
		decimal.WriteByte(digit.String()[0])
	}
	return decimal.String()
}

func (r Rational[T]) ToRoundDecimalString(roundTo int32) string {
	if roundTo == math.MinInt32 {
		panic("cannot round to the minimum representable precision")
	}
	five := Rational[T]{r.getFive(), r.denominator.Pow(0)}
	ten := Rational[T]{r.getTen(), r.denominator.Pow(0)}
	shiftBase := ten.Pow(-roundTo - 1)
	delta := five.Multiply(shiftBase)
	var temp Rational[T]
	if r.IsNegative() {
		temp = r.Minus(delta)
	} else {
		temp = r.Plus(delta)
	}
	return temp.ToTruncateDecimalString(roundTo)
}

func (r Rational[T]) ToCeilDecimalString(roundTo int32) string {
	if r.IsNegative() {
		return "-" + r.Negate().ToFloorDecimalString(roundTo)
	}
	if roundTo == math.MinInt32 {
		panic("cannot round to the minimum representable precision")
	}
	ten := Rational[T]{r.getTen(), r.denominator.Pow(0)}
	if r.Multiply(ten.Pow(roundTo)).IsInteger() {
		return r.ToTruncateDecimalString(roundTo)
	}
	shiftBase := ten.Pow(-roundTo)
	temp := r.Plus(shiftBase)
	return temp.ToTruncateDecimalString(roundTo)
}

func (r Rational[T]) ToFloorDecimalString(roundTo int32) string {
	if r.IsNegative() {
		return "-" + r.Negate().ToCeilDecimalString(roundTo)
	}
	return r.ToTruncateDecimalString(roundTo)
}

func (r Rational[T]) IsInteger() bool  { return r.denominator.IsOne() }
func (r Rational[T]) IsZero() bool     { return r.numerator.IsZero() }
func (r Rational[T]) IsPositive() bool { return r.numerator.IsPositive() }
func (r Rational[T]) IsNegative() bool { return r.numerator.IsNegative() }

func (r Rational[T]) Negate() Rational[T] { return Rational[T]{r.numerator.Negate(), r.denominator} }
func (r Rational[T]) Abs() Rational[T]    { return Rational[T]{r.numerator.Abs(), r.denominator} }

func (r Rational[T]) Reciprocal() Rational[T] {
	if r.IsZero() {
		panic("cannot get the reciprocal of zero")
	}
	return rationalSignNormalized(r.denominator, r.numerator)
}

func (r Rational[T]) reciprocalUnsafe() Rational[T] {
	return rationalSignNormalized(r.denominator, r.numerator)
}

func (r Rational[T]) GetNumeratorAbs() T   { return r.numerator.Abs() }
func (r Rational[T]) GetDenominatorAbs() T { return r.denominator.Abs() }

func (r Rational[T]) Compare(other Rational[T]) int {
	gcd := r.denominator.Gcd(other.denominator)
	left := r.numerator.Multiply(other.denominator.DivideBy(gcd))
	right := other.numerator.Multiply(r.denominator.DivideBy(gcd))
	return left.Compare(right)
}

func (r Rational[T]) Equals(other Rational[T]) bool {
	return r.numerator.Equals(other.numerator) && r.denominator.Equals(other.denominator)
}

func (r Rational[T]) Plus(other Rational[T]) Rational[T] {
	gcd := r.denominator.Gcd(other.denominator)
	m1 := other.denominator.DivideBy(gcd)
	m2 := r.denominator.DivideBy(gcd)
	numerator := r.numerator.Multiply(m1).Plus(other.numerator.Multiply(m2))
	denominator := gcd.Multiply(m1).Multiply(m2)
	return rationalReduced(numerator, denominator)
}

func (r Rational[T]) Minus(other Rational[T]) Rational[T] {
	gcd := r.denominator.Gcd(other.denominator)
	m1 := other.denominator.DivideBy(gcd)
	m2 := r.denominator.DivideBy(gcd)
	numerator := r.numerator.Multiply(m1).Minus(other.numerator.Multiply(m2))
	denominator := gcd.Multiply(m1).Multiply(m2)
	return rationalReduced(numerator, denominator)
}

func (r Rational[T]) Multiply(other Rational[T]) Rational[T] {
	gcd1 := r.denominator.Gcd(other.numerator)
	gcd2 := other.denominator.Gcd(r.numerator)
	denominator := r.denominator.DivideBy(gcd1).Multiply(other.denominator.DivideBy(gcd2))
	numerator := r.numerator.DivideBy(gcd2).Multiply(other.numerator.DivideBy(gcd1))
	return Rational[T]{numerator, denominator}
}

func (r Rational[T]) DivideBy(other Rational[T]) Rational[T] {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	return r.Multiply(other.reciprocalUnsafe())
}

func (r Rational[T]) Pow(exponent int32) Rational[T] {
	if exponent < 0 {
		if r.IsZero() {
			panic("exponent cannot be negative for zero")
		}
		if exponent == math.MinInt32 {
			return r.reciprocalUnsafe().Pow(math.MaxInt32).Multiply(r.reciprocalUnsafe())
		}
		return r.reciprocalUnsafe().Pow(-exponent)
	} else if exponent == 0 {
		return r.getOne()
	} else if exponent == 1 {
		return r
	}
	result := r.getOne()
	base := r
	power := exponent
	for {
		if power&1 == 1 {
			result = result.Multiply(base)
		}
		power >>= 1
		if power > 0 {
			base = base.Multiply(base)
		} else {
			break
		}
	}
	return result
}
