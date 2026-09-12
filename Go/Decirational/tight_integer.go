package decirational

import (
	"fmt"
	"strconv"
)

// TightInteger is an arbitrary-precision signed integer stored as base-2^32
// words, most significant first. Selectable via --integer=tight.
type TightInteger struct {
	negative bool
	words    []uint32
}

func tightIntegerUnsafe(words []uint32, negative bool) TightInteger {
	w := optimizeWords(words)
	return TightInteger{optimizeSignWords(negative, w), w}
}

var TightZero = tightIntegerUnsafe([]uint32{0}, false)
var TightOne = tightIntegerUnsafe([]uint32{1}, false)

var tightDigitConstants = [2]TightInteger{TightZero, TightOne}

// GetTightDigit returns the single-word TightInteger 0 or 1 for index.
func GetTightDigit(index int) (TightInteger, error) {
	if index < 0 || index > 1 {
		return TightInteger{}, fmt.Errorf("the index must be either 0 or 1")
	}
	return tightDigitConstants[index], nil
}

// NewTightInteger builds a TightInteger from base-2^32 words (most
// significant first) and a sign.
func NewTightInteger(words []uint32, negative bool) (TightInteger, error) {
	if len(words) == 0 {
		return TightInteger{}, fmt.Errorf("input word slice cannot be empty")
	}
	copied := make([]uint32, len(words))
	copy(copied, words)
	return tightIntegerUnsafe(copied, negative), nil
}

// NewTightIntegerFromInt32 builds a single-word TightInteger directly from a
// 32-bit integer, without going through decimal string parsing. Widens to
// int64 before negating so math.MinInt32 doesn't overflow.
func NewTightIntegerFromInt32(n int32) TightInteger {
	abs := reverseAbs64(int64(n))
	words := []uint32{uint32(abs)}
	return tightIntegerUnsafe(words, abs != int64(n))
}

// NewTightIntegerFromInt64 builds a TightInteger from a 64-bit integer via a
// decimal string round-trip.
func NewTightIntegerFromInt64(n int64) TightInteger {
	t, err := ParseTightInteger(strconv.FormatInt(n, 10))
	if err != nil {
		panic(err) // unreachable: strconv.FormatInt always yields a parseable integer
	}
	return t
}

// NewTightIntegerFromDecimal converts a DecimalInteger to a TightInteger.
func NewTightIntegerFromDecimal(d DecimalInteger) TightInteger {
	return d.ToTightInteger()
}

// ParseTightInteger parses a (possibly signed) decimal integer literal.
func ParseTightInteger(number string) (TightInteger, error) {
	d, err := ParseDecimalInteger(number)
	if err != nil {
		return TightInteger{}, err
	}
	return d.ToTightInteger(), nil
}

func (t TightInteger) String() string {
	return t.ToDecimalInteger().String()
}

// ToDecimalInteger converts this TightInteger to the base-10 representation.
func (t TightInteger) ToDecimalInteger() DecimalInteger {
	decimalLength := int(float64(len(t.words))*tightToDecimalLengthRatio+1) + 1
	digits := make([]byte, decimalLength)
	convertWordsToDigits(digits, t.words)
	return decimalIntegerUnsafe(digits, t.negative)
}

func (t TightInteger) IsZero() bool     { return len(t.words) == 1 && t.words[0] == 0 }
func (t TightInteger) IsOne() bool      { return !t.negative && len(t.words) == 1 && t.words[0] == 1 }
func (t TightInteger) IsUnitAbs() bool  { return len(t.words) == 1 && t.words[0] == 1 }
func (t TightInteger) IsPositive() bool { return !t.negative && !t.IsZero() }
func (t TightInteger) IsNegative() bool { return t.negative }

func (t TightInteger) Negate() TightInteger { return tightIntegerUnsafe(t.words, !t.negative) }
func (t TightInteger) Abs() TightInteger    { return tightIntegerUnsafe(t.words, false) }

func (t TightInteger) Compare(other TightInteger) int {
	if t.negative && !other.negative {
		return -1
	} else if !t.negative && other.negative {
		return 1
	}
	if len(t.words) < len(other.words) {
		return ifNeg(t.negative, 1, -1)
	} else if len(t.words) > len(other.words) {
		return ifNeg(t.negative, -1, 1)
	}
	for i := range t.words {
		if t.words[i] < other.words[i] {
			return ifNeg(t.negative, 1, -1)
		} else if t.words[i] > other.words[i] {
			return ifNeg(t.negative, -1, 1)
		}
	}
	return 0
}

func (t TightInteger) Equals(other TightInteger) bool {
	if t.negative != other.negative || len(t.words) != len(other.words) {
		return false
	}
	for i := range t.words {
		if t.words[i] != other.words[i] {
			return false
		}
	}
	return true
}

func (t TightInteger) plusRaw(other TightInteger) TightInteger {
	words := expandWords(t.words, max(len(t.words), len(other.words))+1)
	addWords(words, other.words)
	return tightIntegerUnsafe(words, t.negative)
}

func (t TightInteger) Plus(other TightInteger) TightInteger {
	if t.IsZero() {
		return other
	}
	if other.IsZero() {
		return t
	}
	if t.negative != other.negative {
		return t.minusRaw(other.Negate())
	}
	return t.plusRaw(other)
}

func (t TightInteger) minusRaw(other TightInteger) TightInteger {
	cmp := t.Abs().Compare(other.Abs())
	a, b := t, other
	negative := t.negative
	if cmp == 0 {
		return TightZero
	} else if cmp < 0 {
		a, b = other, t
		negative = !negative
	}
	words := append([]uint32(nil), a.words...)
	subtractWords(words, b.words)
	return tightIntegerUnsafe(words, negative)
}

func (t TightInteger) Minus(other TightInteger) TightInteger {
	if t.IsZero() {
		return other.Negate()
	}
	if other.IsZero() {
		return t
	}
	if t.negative != other.negative {
		return t.plusRaw(other.Negate())
	}
	return t.minusRaw(other)
}

func (t TightInteger) Multiply(other TightInteger) TightInteger {
	if t.IsZero() || other.IsZero() {
		return TightZero
	}
	if t.IsUnitAbs() {
		if t.IsPositive() {
			return other
		}
		return other.Negate()
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return t
		}
		return t.Negate()
	}
	words := make([]uint32, len(t.words)+len(other.words))
	multiplyWords(words, t.words, other.words)
	return tightIntegerUnsafe(words, t.negative != other.negative)
}

func (t TightInteger) MultiplyBase(times int32) TightInteger {
	if times < 0 {
		panic("multiplication times cannot be negative")
	} else if times == 0 {
		return t
	} else if t.IsZero() {
		return TightZero
	}
	words := make([]uint32, len(t.words)+int(times))
	copy(words, t.words)
	return tightIntegerUnsafe(words, t.negative)
}

func (t TightInteger) MultiplyBaseOnce() TightInteger { return t.MultiplyBase(1) }

func (t TightInteger) DivideByBase(times int32) TightInteger {
	if times < 0 {
		panic("division times cannot be negative")
	} else if times == 0 {
		return t
	} else if t.IsZero() || int(times) >= len(t.words) {
		return TightZero
	}
	words := make([]uint32, len(t.words)-int(times))
	copy(words, t.words)
	return tightIntegerUnsafe(words, t.negative)
}

func (t TightInteger) DivideByBaseOnce() TightInteger { return t.DivideByBase(1) }

func (t TightInteger) DivideBy(other TightInteger) TightInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if t.IsZero() {
		return TightZero
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return t
		}
		return t.Negate()
	}
	words := make([]uint32, len(t.words))
	divideWords(words, t.words, other.words)
	return tightIntegerUnsafe(words, t.negative != other.negative)
}

func (t TightInteger) Modulo(other TightInteger) TightInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if t.IsZero() || other.IsUnitAbs() {
		return TightZero
	}
	words := make([]uint32, len(other.words))
	moduloWords(words, t.words, other.words)
	return tightIntegerUnsafe(words, t.negative)
}

func (t TightInteger) DivideByAndModulo(other TightInteger) [2]TightInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if t.IsZero() {
		return [2]TightInteger{TightZero, TightZero}
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return [2]TightInteger{t, TightZero}
		}
		return [2]TightInteger{t.Negate(), TightZero}
	}
	quotient := make([]uint32, len(t.words))
	remainder := make([]uint32, len(other.words))
	divideAndModuloWords(quotient, remainder, t.words, other.words)
	return [2]TightInteger{
		tightIntegerUnsafe(quotient, t.negative != other.negative),
		tightIntegerUnsafe(remainder, t.negative),
	}
}

func (t TightInteger) Gcd(other TightInteger) TightInteger {
	if t.IsZero() {
		return other.Abs()
	}
	if other.IsZero() {
		return t.Abs()
	}
	if t.IsUnitAbs() || other.IsUnitAbs() {
		return TightOne
	}
	words := make([]uint32, min(len(t.words), len(other.words)))
	gcdWords(words, t.words, other.words)
	return tightIntegerUnsafe(words, false)
}

func (t TightInteger) Lcm(other TightInteger) TightInteger {
	return t.DivideBy(t.Gcd(other)).Multiply(other)
}

func (t TightInteger) Pow(exponent int32) TightInteger {
	if exponent < 0 {
		panic("exponent cannot be negative")
	} else if exponent == 0 {
		return TightOne
	} else if exponent == 1 {
		return t
	}
	result := TightOne
	base := t
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
