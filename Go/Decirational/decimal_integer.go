package decirational

import (
	"fmt"
	"strconv"
	"strings"
)

// decimalToTightLengthRatio / tightToDecimalLengthRatio mirror Java's
// Arithmetic.decimal_to_tight_length_ratio / tight_to_decimal_length_ratio:
// the number of base-2^32 words (resp. decimal digits) needed per decimal
// digit (resp. word), used to size scratch buffers before base conversion.
const decimalToTightLengthRatio = 0.10381288809031315 // log(10) / (32*log(2))
const tightToDecimalLengthRatio = 9.632959861247398   // 32*log10(2)

// DecimalInteger is an arbitrary-precision signed integer stored as base-10
// digits, most significant first. It is the Go counterpart of Java's
// DecimalInteger and is the default CustomInteger backend.
type DecimalInteger struct {
	negative bool
	digits   []byte
}

func decimalIntegerUnsafe(digits []byte, negative bool) DecimalInteger {
	d := optimizeDigits(digits)
	return DecimalInteger{optimizeSignDigits(negative, d), d}
}

var DecimalZero = decimalIntegerUnsafe([]byte{0}, false)
var DecimalOne = decimalIntegerUnsafe([]byte{1}, false)
var DecimalTwo = decimalIntegerUnsafe([]byte{2}, false)
var DecimalThree = decimalIntegerUnsafe([]byte{3}, false)
var DecimalFour = decimalIntegerUnsafe([]byte{4}, false)
var DecimalFive = decimalIntegerUnsafe([]byte{5}, false)
var DecimalSix = decimalIntegerUnsafe([]byte{6}, false)
var DecimalSeven = decimalIntegerUnsafe([]byte{7}, false)
var DecimalEight = decimalIntegerUnsafe([]byte{8}, false)
var DecimalNine = decimalIntegerUnsafe([]byte{9}, false)

var decimalDigitConstants = [10]DecimalInteger{DecimalZero, DecimalOne, DecimalTwo, DecimalThree, DecimalFour, DecimalFive, DecimalSix, DecimalSeven, DecimalEight, DecimalNine}

// GetDecimalDigit returns the single-digit DecimalInteger 0-9 for index.
func GetDecimalDigit(index int) (DecimalInteger, error) {
	if index < 0 || index > 9 {
		return DecimalInteger{}, fmt.Errorf("the index must be between 0 and 9")
	}
	return decimalDigitConstants[index], nil
}

// NewDecimalInteger builds a DecimalInteger from base-10 digits (each 0-9,
// most significant first) and a sign, validating the digits.
func NewDecimalInteger(digits []byte, negative bool) (DecimalInteger, error) {
	if len(digits) == 0 {
		return DecimalInteger{}, fmt.Errorf("input digit slice cannot be empty")
	}
	for _, d := range digits {
		if d > 9 {
			return DecimalInteger{}, fmt.Errorf("input slice is not a valid decimal integer")
		}
	}
	copied := make([]byte, len(digits))
	copy(copied, digits)
	return decimalIntegerUnsafe(copied, negative), nil
}

// NewDecimalIntegerFromInt64 builds a DecimalInteger from a machine integer.
func NewDecimalIntegerFromInt64(n int64) DecimalInteger {
	d, err := ParseDecimalInteger(strconv.FormatInt(n, 10))
	if err != nil {
		panic(err) // unreachable: strconv.FormatInt always yields a parseable integer
	}
	return d
}

// NewDecimalIntegerFromInt32 builds a DecimalInteger from a 32-bit integer.
func NewDecimalIntegerFromInt32(n int32) DecimalInteger {
	return NewDecimalIntegerFromInt64(int64(n))
}

// NewDecimalIntegerFromTight converts a TightInteger to a DecimalInteger.
func NewDecimalIntegerFromTight(t TightInteger) DecimalInteger {
	return t.ToDecimalInteger()
}

// ParseDecimalInteger parses a (possibly signed) decimal integer literal.
func ParseDecimalInteger(number string) (DecimalInteger, error) {
	number = stripWhitespace(number)
	if number == "" {
		return DecimalInteger{}, fmt.Errorf("input is empty")
	}
	start := 0
	negative := false
	if isMinus(number[0]) {
		negative = true
		start = 1
	} else if isPlus(number[0]) {
		start = 1
	}
	if start == len(number) {
		return DecimalInteger{}, fmt.Errorf("input %q has no digits", number)
	}
	for i := start; i < len(number); i++ {
		if !isDigit(number[i]) {
			return DecimalInteger{}, fmt.Errorf("input %q is invalid and cannot be parsed as a decimal integer", number)
		}
	}
	digits := make([]byte, len(number)-start)
	for i := start; i < len(number); i++ {
		digits[i-start] = charToDigit(number[i])
	}
	return decimalIntegerUnsafe(digits, negative), nil
}

func stripWhitespace(s string) string {
	var sb strings.Builder
	for _, r := range s {
		if r != ' ' && r != '\t' && r != '\n' && r != '\r' && r != '\f' && r != '\v' {
			sb.WriteRune(r)
		}
	}
	return sb.String()
}

func (d DecimalInteger) String() string {
	var sb strings.Builder
	if d.negative {
		sb.WriteByte('-')
	}
	for _, digit := range d.digits {
		sb.WriteByte(digitToChar(digit))
	}
	return sb.String()
}

// ToTightInteger converts this DecimalInteger to the base-2^32 representation.
func (d DecimalInteger) ToTightInteger() TightInteger {
	tightLength := int(float64(len(d.digits))*decimalToTightLengthRatio+1) + 1
	words := make([]uint32, tightLength)
	convertDigitsToWords(words, d.digits)
	return tightIntegerUnsafe(words, d.negative)
}

func (d DecimalInteger) IsZero() bool     { return len(d.digits) == 1 && d.digits[0] == 0 }
func (d DecimalInteger) IsOne() bool      { return !d.negative && len(d.digits) == 1 && d.digits[0] == 1 }
func (d DecimalInteger) IsUnitAbs() bool  { return len(d.digits) == 1 && d.digits[0] == 1 }
func (d DecimalInteger) IsPositive() bool { return !d.negative && !d.IsZero() }
func (d DecimalInteger) IsNegative() bool { return d.negative }

func (d DecimalInteger) Negate() DecimalInteger { return decimalIntegerUnsafe(d.digits, !d.negative) }
func (d DecimalInteger) Abs() DecimalInteger    { return decimalIntegerUnsafe(d.digits, false) }

func (d DecimalInteger) Compare(other DecimalInteger) int {
	if d.negative && !other.negative {
		return -1
	} else if !d.negative && other.negative {
		return 1
	}
	if len(d.digits) < len(other.digits) {
		return ifNeg(d.negative, 1, -1)
	} else if len(d.digits) > len(other.digits) {
		return ifNeg(d.negative, -1, 1)
	}
	for i := range d.digits {
		if d.digits[i] < other.digits[i] {
			return ifNeg(d.negative, 1, -1)
		} else if d.digits[i] > other.digits[i] {
			return ifNeg(d.negative, -1, 1)
		}
	}
	return 0
}

func ifNeg(negative bool, whenNeg, whenPos int) int {
	if negative {
		return whenNeg
	}
	return whenPos
}

func (d DecimalInteger) Equals(other DecimalInteger) bool {
	if d.negative != other.negative || len(d.digits) != len(other.digits) {
		return false
	}
	for i := range d.digits {
		if d.digits[i] != other.digits[i] {
			return false
		}
	}
	return true
}

func (d DecimalInteger) plusRaw(other DecimalInteger) DecimalInteger {
	// expandDigits always allocates here since the target length (max+1)
	// is strictly greater than len(d.digits), so this never aliases d.digits.
	digits := expandDigits(d.digits, max(len(d.digits), len(other.digits))+1)
	addDigits(digits, other.digits)
	return decimalIntegerUnsafe(digits, d.negative)
}

func (d DecimalInteger) Plus(other DecimalInteger) DecimalInteger {
	if d.IsZero() {
		return other
	}
	if other.IsZero() {
		return d
	}
	if d.negative != other.negative {
		return d.minusRaw(other.Negate())
	}
	return d.plusRaw(other)
}

func (d DecimalInteger) minusRaw(other DecimalInteger) DecimalInteger {
	cmp := d.Abs().Compare(other.Abs())
	a, b := d, other
	negative := d.negative
	if cmp == 0 {
		return DecimalZero
	} else if cmp < 0 {
		a, b = other, d
		negative = !negative
	}
	digits := append([]byte(nil), a.digits...)
	subtractDigits(digits, b.digits)
	return decimalIntegerUnsafe(digits, negative)
}

func (d DecimalInteger) Minus(other DecimalInteger) DecimalInteger {
	if d.IsZero() {
		return other.Negate()
	}
	if other.IsZero() {
		return d
	}
	if d.negative != other.negative {
		return d.plusRaw(other.Negate())
	}
	return d.minusRaw(other)
}

func (d DecimalInteger) Multiply(other DecimalInteger) DecimalInteger {
	if d.IsZero() || other.IsZero() {
		return DecimalZero
	}
	if d.IsUnitAbs() {
		if d.IsPositive() {
			return other
		}
		return other.Negate()
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return d
		}
		return d.Negate()
	}
	digits := make([]byte, len(d.digits)+len(other.digits))
	multiplyDigits(digits, d.digits, other.digits)
	return decimalIntegerUnsafe(digits, d.negative != other.negative)
}

func (d DecimalInteger) MultiplyBase(times int32) DecimalInteger {
	if times < 0 {
		panic("multiplication times cannot be negative")
	} else if times == 0 {
		return d
	} else if d.IsZero() {
		return DecimalZero
	}
	digits := make([]byte, len(d.digits)+int(times))
	copy(digits, d.digits)
	return decimalIntegerUnsafe(digits, d.negative)
}

func (d DecimalInteger) MultiplyBaseOnce() DecimalInteger { return d.MultiplyBase(1) }

func (d DecimalInteger) DivideByBase(times int32) DecimalInteger {
	if times < 0 {
		panic("division times cannot be negative")
	} else if times == 0 {
		return d
	} else if d.IsZero() || int(times) >= len(d.digits) {
		return DecimalZero
	}
	digits := make([]byte, len(d.digits)-int(times))
	copy(digits, d.digits)
	return decimalIntegerUnsafe(digits, d.negative)
}

func (d DecimalInteger) DivideByBaseOnce() DecimalInteger { return d.DivideByBase(1) }

func (d DecimalInteger) DivideBy(other DecimalInteger) DecimalInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if d.IsZero() {
		return DecimalZero
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return d
		}
		return d.Negate()
	}
	digits := make([]byte, len(d.digits))
	divideDigits(digits, d.digits, other.digits)
	return decimalIntegerUnsafe(digits, d.negative != other.negative)
}

func (d DecimalInteger) Modulo(other DecimalInteger) DecimalInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if d.IsZero() || other.IsUnitAbs() {
		return DecimalZero
	}
	digits := make([]byte, len(other.digits))
	moduloDigits(digits, d.digits, other.digits)
	return decimalIntegerUnsafe(digits, d.negative)
}

func (d DecimalInteger) DivideByAndModulo(other DecimalInteger) [2]DecimalInteger {
	if other.IsZero() {
		panic("cannot divide by zero")
	}
	if d.IsZero() {
		return [2]DecimalInteger{DecimalZero, DecimalZero}
	}
	if other.IsUnitAbs() {
		if other.IsPositive() {
			return [2]DecimalInteger{d, DecimalZero}
		}
		return [2]DecimalInteger{d.Negate(), DecimalZero}
	}
	quotient := make([]byte, len(d.digits))
	remainder := make([]byte, len(other.digits))
	divideAndModuloDigits(quotient, remainder, d.digits, other.digits)
	return [2]DecimalInteger{
		decimalIntegerUnsafe(quotient, d.negative != other.negative),
		decimalIntegerUnsafe(remainder, d.negative),
	}
}

func (d DecimalInteger) Gcd(other DecimalInteger) DecimalInteger {
	if d.IsZero() {
		return other
	}
	if other.IsZero() {
		return d
	}
	if d.IsUnitAbs() || other.IsUnitAbs() {
		return DecimalOne
	}
	digits := make([]byte, min(len(d.digits), len(other.digits)))
	gcdDigits(digits, d.digits, other.digits)
	return decimalIntegerUnsafe(digits, false)
}

func (d DecimalInteger) Lcm(other DecimalInteger) DecimalInteger {
	return d.DivideBy(d.Gcd(other)).Multiply(other)
}

func (d DecimalInteger) Pow(exponent int32) DecimalInteger {
	if exponent < 0 {
		panic("exponent cannot be negative")
	} else if exponent == 0 {
		return DecimalOne
	} else if exponent == 1 {
		return d
	}
	result := DecimalOne
	base := d
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
