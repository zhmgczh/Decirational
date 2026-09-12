package decirational

// Schoolbook add/subtract/multiply, binary-search long division, Euclidean
// gcd, and base conversion between decimal digits and base-2^32 words.
//
// Each algorithm is written once, generic over limb (byte | uint32), and
// instantiated for both by the *Digits/*Words functions below - kept as the
// stable entry points every call site elsewhere in the package uses. Go's
// generics can't express "the base/max value for this type" as an
// associated constant, so a small limbSpec value carries those two numbers
// alongside the type parameter instead.

const cyclicBegin = '{'
const cyclicEnd = '}'

func isDigit(c byte) bool           { return c >= '0' && c <= '9' }
func isPlus(c byte) bool            { return c == '+' }
func isMinus(c byte) bool           { return c == '-' }
func isFractionBar(c byte) bool     { return c == '/' }
func isDecimalPoint(c byte) bool    { return c == '.' }
func isCyclicBeginChar(c byte) bool { return c == cyclicBegin }
func isCyclicEndChar(c byte) bool   { return c == cyclicEnd }

func charToDigit(c byte) byte { return c - '0' }
func digitToChar(d byte) byte { return d + '0' }

// ---- the shared algorithm, generic over the limb type ----

// limb is a single element of a big-integer magnitude: a decimal digit
// (byte, base 10) or a base-2^32 word (uint32, base 2^32).
type limb interface {
	byte | uint32
}

// limbSpec carries the base-specific constants (BASE and the largest valid
// single-limb value, BASE-1) that isolate a limb type from the shared
// algorithm below. All arithmetic widens through uint64 - comfortably large
// enough for both: the largest possible uint32 product plus two uint32
// addends still fits uint64 - so the same schoolbook algorithms work
// unchanged for either limb type.
type limbSpec[T limb] struct {
	base uint64
	max  T
}

var digitSpec = limbSpec[byte]{base: 10, max: 9}
var wordSpec = limbSpec[uint32]{base: 1 << 32, max: ^uint32(0)}

func precedingZeros[T limb](limbs []T) int {
	for i, d := range limbs {
		if d != 0 {
			return i
		}
	}
	return len(limbs)
}

func isZero[T limb](limbs []T) bool {
	return precedingZeros(limbs) == len(limbs)
}

// optimize trims leading zero limbs, always keeping at least one. It returns
// the input slice unchanged (same backing array) when nothing needs trimming.
func optimize[T limb](limbs []T) []T {
	cutLength := precedingZeros(limbs)
	if cutLength > len(limbs)-1 {
		cutLength = len(limbs) - 1
	}
	if cutLength == 0 {
		return limbs
	}
	result := make([]T, len(limbs)-cutLength)
	copy(result, limbs[cutLength:])
	return result
}

// optimizeSign forces the sign to non-negative when the magnitude is zero.
func optimizeSign[T limb](negative bool, limbs []T) bool {
	if len(limbs) == 1 && limbs[0] == 0 {
		return false
	}
	return negative
}

// expand left-pads limbs with zeros to reach length. If length is not larger
// than len(limbs), limbs is returned unchanged.
func expand[T limb](limbs []T, length int) []T {
	if length <= len(limbs) {
		return limbs
	}
	result := make([]T, length)
	copy(result[length-len(limbs):], limbs)
	return result
}

func passCarry[T limb](spec limbSpec[T], limbs []T, sum uint64, i int) uint64 {
	carry := sum / spec.base
	if carry == 0 {
		limbs[i] = T(sum)
	} else {
		limbs[i] = T(sum % spec.base)
	}
	return carry
}

func add[T limb](spec limbSpec[T], limbs []T, other []T) {
	diff := len(limbs) - len(other)
	var carry uint64 = 0
	otherIndex := len(other) - 1
	for i := len(limbs) - 1; i >= diff; i-- {
		sum := uint64(limbs[i]) + uint64(other[otherIndex]) + carry
		carry = passCarry(spec, limbs, sum, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		sum := uint64(limbs[i]) + carry
		carry = passCarry(spec, limbs, sum, i)
	}
}

func passBorrow[T limb](spec limbSpec[T], limbs []T, difference int64, i int) uint64 {
	if difference < 0 {
		limbs[i] = T(uint64(difference + int64(spec.base)))
		return 1
	}
	limbs[i] = T(uint64(difference))
	return 0
}

func subtract[T limb](spec limbSpec[T], limbs []T, other []T) {
	diff := len(limbs) - len(other)
	var borrow uint64 = 0
	otherIndex := len(other) - 1
	for i := len(limbs) - 1; i >= diff; i-- {
		d := int64(limbs[i]) - int64(other[otherIndex]) - int64(borrow)
		borrow = passBorrow(spec, limbs, d, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		d := int64(limbs[i]) - int64(borrow)
		borrow = passBorrow(spec, limbs, d, i)
	}
}

// multiply sets limbs (assumed pre-zeroed, len(a)+len(b) long) to a*b.
func multiply[T limb](spec limbSpec[T], limbs []T, a []T, b []T) {
	for i := 1; i <= len(a); i++ {
		var carry uint64 = 0
		limbsIndex := len(limbs) - i
		aValue := uint64(a[len(a)-i])
		for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
			sum := uint64(limbs[limbsIndex]) + aValue*uint64(b[bIndex]) + carry
			carry = passCarry(spec, limbs, sum, limbsIndex)
			limbsIndex--
		}
		if carry != 0 {
			limbs[limbsIndex] += T(carry)
		}
	}
}

// multiplyScalar sets limbs (assumed pre-zeroed, len(b)+1 long) to a*b for a single limb a.
func multiplyScalar[T limb](spec limbSpec[T], limbs []T, a T, b []T) {
	var carry uint64 = 0
	limbsIndex := len(limbs) - 1
	aValue := uint64(a)
	for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
		sum := uint64(limbs[limbsIndex]) + aValue*uint64(b[bIndex]) + carry
		carry = passCarry(spec, limbs, sum, limbsIndex)
		limbsIndex--
	}
	if carry != 0 {
		limbs[limbsIndex] += T(carry)
	}
}

func compare[T limb](a []T, b []T) int {
	aStart := precedingZeros(a)
	bStart := precedingZeros(b)
	aLen := len(a) - aStart
	bLen := len(b) - bStart
	if aLen > bLen {
		return 1
	} else if aLen < bLen {
		return -1
	}
	for i := 0; i < aLen; i++ {
		av, bv := a[aStart+i], b[bStart+i]
		if av > bv {
			return 1
		} else if av < bv {
			return -1
		}
	}
	return 0
}

// findRightBoundary returns, relative to the start of dividend, the last
// index of the shortest leading window of dividend that is >= divisor.
func findRightBoundary[T limb](dividend []T, divisor []T) int {
	bound := len(divisor) - 1
	for i := 0; i < len(divisor); i++ {
		if i >= len(dividend) {
			return bound + 1
		}
		if dividend[i] > divisor[i] {
			return bound
		} else if dividend[i] < divisor[i] {
			return bound + 1
		}
	}
	return bound
}

// multiplier finds, via binary search, the largest single limb d such that
// d*divisor <= dividend, leaving that product in temp (len(divisor)+1 long).
func multiplier[T limb](spec limbSpec[T], temp []T, dividend []T, divisor []T) T {
	var left, right uint64 = 0, uint64(spec.max)
	mid := (left + right + 1) >> 1
outer:
	for left < right {
		mid = (left + right + 1) >> 1
		clear(temp)
		multiplyScalar(spec, temp, T(mid), divisor)
		switch compare(temp, dividend) {
		case 0:
			left = mid
			break outer
		case -1:
			left = mid
		case 1:
			right = mid - 1
		}
	}
	if mid != left {
		clear(temp)
		multiplyScalar(spec, temp, T(left), divisor)
	}
	return T(left)
}

// alignedTemp drops temp's leading limb when it is zero, so its length
// matches whichever dividend window multiplier was solving for.
func alignedTemp[T limb](temp []T) []T {
	if temp[0] == 0 {
		return temp[1:]
	}
	return temp
}

func divide[T limb](spec limbSpec[T], quotient []T, dividend []T, divisor []T) {
	divisorSig := divisor[precedingZeros(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]T, len(divisorSig)+1)
	remaining := make([]T, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZeros(remaining)
	right := left + findRightBoundary(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplier(spec, temp, window, divisorSig)
		subtract(spec, window, alignedTemp(temp))
		left += precedingZeros(remaining[left:])
		right = left + findRightBoundary(remaining[left:], divisorSig)
	}
}

func modulo[T limb](spec limbSpec[T], remainder []T, dividend []T, divisor []T) {
	divisorSig := divisor[precedingZeros(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]T, len(divisorSig)+1)
	remaining := make([]T, len(dividend))
	copy(remaining, dividend)
	left := precedingZeros(remaining)
	right := left + findRightBoundary(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		multiplier(spec, temp, window, divisorSig)
		subtract(spec, window, alignedTemp(temp))
		left += precedingZeros(remaining[left:])
		right = left + findRightBoundary(remaining[left:], divisorSig)
	}
	start := precedingZeros(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func divideAndModulo[T limb](spec limbSpec[T], quotient []T, remainder []T, dividend []T, divisor []T) {
	divisorSig := divisor[precedingZeros(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]T, len(divisorSig)+1)
	remaining := make([]T, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZeros(remaining)
	right := left + findRightBoundary(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplier(spec, temp, window, divisorSig)
		subtract(spec, window, alignedTemp(temp))
		left += precedingZeros(remaining[left:])
		right = left + findRightBoundary(remaining[left:], divisorSig)
	}
	start := precedingZeros(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func gcd[T limb](spec limbSpec[T], result []T, a []T, b []T) {
	cmp := compare(a, b)
	if cmp == 0 {
		target := min(len(result), min(len(a), len(b)))
		copy(result[len(result)-target:], a[len(a)-target:])
		return
	}
	maxLength := max(len(a), len(b))
	larger := make([]T, maxLength)
	smaller := make([]T, maxLength)
	if cmp > 0 {
		copy(larger[maxLength-len(a):], a)
		copy(smaller[maxLength-len(b):], b)
	} else {
		copy(larger[maxLength-len(b):], b)
		copy(smaller[maxLength-len(a):], a)
	}
	temp := make([]T, maxLength)
	largerLeft := precedingZeros(larger)
	smallerLeft := precedingZeros(smaller)
	for !isZero(smaller[smallerLeft:]) {
		clear(temp[smallerLeft:])
		modulo(spec, temp[smallerLeft:], larger[largerLeft:], smaller[smallerLeft:])
		oldSmallerLeft := smallerLeft
		larger, largerLeft, smaller, temp = smaller, smallerLeft, temp, larger
		smallerLeft = oldSmallerLeft + precedingZeros(smaller[oldSmallerLeft:])
	}
	copy(result[len(result)-(len(larger)-largerLeft):], larger[largerLeft:])
}

// ---- decimal digits (base 10, one decimal digit per byte, most significant first) ----

func precedingZerosDigits(digits []byte) int               { return precedingZeros(digits) }
func isZeroDigits(digits []byte) bool                      { return isZero(digits) }
func optimizeDigits(digits []byte) []byte                  { return optimize(digits) }
func optimizeSignDigits(negative bool, digits []byte) bool { return optimizeSign(negative, digits) }
func expandDigits(digits []byte, length int) []byte        { return expand(digits, length) }
func addDigits(digits []byte, other []byte)                { add(digitSpec, digits, other) }
func subtractDigits(digits []byte, other []byte)           { subtract(digitSpec, digits, other) }
func multiplyDigits(digits []byte, a []byte, b []byte)     { multiply(digitSpec, digits, a, b) }
func multiplyDigitsScalar(digits []byte, a byte, b []byte) { multiplyScalar(digitSpec, digits, a, b) }
func compareDigits(a []byte, b []byte) int                 { return compare(a, b) }
func findRightBoundaryDigits(dividend []byte, divisor []byte) int {
	return findRightBoundary(dividend, divisor)
}
func multiplierDigits(temp []byte, dividend []byte, divisor []byte) byte {
	return multiplier(digitSpec, temp, dividend, divisor)
}
func divideDigits(quotient []byte, dividend []byte, divisor []byte) {
	divide(digitSpec, quotient, dividend, divisor)
}
func moduloDigits(remainder []byte, dividend []byte, divisor []byte) {
	modulo(digitSpec, remainder, dividend, divisor)
}
func divideAndModuloDigits(quotient []byte, remainder []byte, dividend []byte, divisor []byte) {
	divideAndModulo(digitSpec, quotient, remainder, dividend, divisor)
}
func gcdDigits(result []byte, a []byte, b []byte) { gcd(digitSpec, result, a, b) }

// ---- tight words (base 2^32, one word per uint32, most significant first) ----

func precedingZerosWords(words []uint32) int               { return precedingZeros(words) }
func isZeroWords(words []uint32) bool                      { return isZero(words) }
func optimizeWords(words []uint32) []uint32                { return optimize(words) }
func optimizeSignWords(negative bool, words []uint32) bool { return optimizeSign(negative, words) }
func expandWords(words []uint32, length int) []uint32      { return expand(words, length) }

// reverseAbs64 is the absolute value of a widened 64-bit number, used so
// that math.MinInt32 doesn't overflow.
func reverseAbs64(number int64) int64 {
	if number < 0 {
		return -number
	}
	return number
}

func addWords(words []uint32, other []uint32)              { add(wordSpec, words, other) }
func subtractWords(words []uint32, other []uint32)         { subtract(wordSpec, words, other) }
func multiplyWords(words []uint32, a []uint32, b []uint32) { multiply(wordSpec, words, a, b) }
func multiplyWordsScalar(words []uint32, a uint32, b []uint32) {
	multiplyScalar(wordSpec, words, a, b)
}
func compareWords(a []uint32, b []uint32) int { return compare(a, b) }
func findRightBoundaryWords(dividend []uint32, divisor []uint32) int {
	return findRightBoundary(dividend, divisor)
}
func multiplierWords(temp []uint32, dividend []uint32, divisor []uint32) uint32 {
	return multiplier(wordSpec, temp, dividend, divisor)
}
func divideWords(quotient []uint32, dividend []uint32, divisor []uint32) {
	divide(wordSpec, quotient, dividend, divisor)
}
func moduloWords(remainder []uint32, dividend []uint32, divisor []uint32) {
	modulo(wordSpec, remainder, dividend, divisor)
}
func divideAndModuloWords(quotient []uint32, remainder []uint32, dividend []uint32, divisor []uint32) {
	divideAndModulo(wordSpec, quotient, remainder, dividend, divisor)
}
func gcdWords(result []uint32, a []uint32, b []uint32) { gcd(wordSpec, result, a, b) }

// ---- base conversion between decimal digits and base-2^32 words ----

var tightBaseAsWord = []uint32{10}
var tightBaseAsDigits = []byte{4, 2, 9, 4, 9, 6, 7, 2, 9, 6} // 4294967296 written in decimal digits

func decimalRemainderToWord(remainder []byte) uint32 {
	var value uint64 = 0
	for _, d := range remainder {
		value = value*10 + uint64(d)
	}
	return uint32(value)
}

// convertWordsToDigits writes the decimal digit expansion of words (base 2^32) into digits.
func convertWordsToDigits(digits []byte, words []uint32) {
	quotient := make([]uint32, len(words))
	copy(quotient, words)
	temp := make([]uint32, len(quotient))
	remainder := make([]uint32, 1)
	digitsIndex := len(digits) - 1
	left := precedingZerosWords(quotient)
	for !isZeroWords(quotient[left:]) {
		window := quotient[left:]
		clear(temp[left:])
		remainder[0] = 0
		divideAndModuloWords(temp[left:], remainder, window, tightBaseAsWord)
		digits[digitsIndex] = byte(remainder[0])
		left += precedingZerosWords(temp[left:])
		copy(quotient[left:], temp[left:])
		digitsIndex--
	}
}

// convertDigitsToWords writes the base-2^32 word expansion of digits (decimal) into words.
func convertDigitsToWords(words []uint32, digits []byte) {
	quotient := make([]byte, len(digits))
	copy(quotient, digits)
	temp := make([]byte, len(quotient))
	remainder := make([]byte, len(tightBaseAsDigits))
	wordsIndex := len(words) - 1
	left := precedingZerosDigits(quotient)
	for !isZeroDigits(quotient[left:]) {
		window := quotient[left:]
		clear(temp[left:])
		clear(remainder)
		divideAndModuloDigits(temp[left:], remainder, window, tightBaseAsDigits)
		words[wordsIndex] = decimalRemainderToWord(remainder)
		left += precedingZerosDigits(temp[left:])
		copy(quotient[left:], temp[left:])
		wordsIndex--
	}
}
