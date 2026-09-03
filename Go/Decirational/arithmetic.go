package decirational

// This file is a faithful port of the Java Arithmetic class: schoolbook
// add/subtract/multiply, binary-search long division, Euclidean gcd, and
// base conversion between decimal digits and base-2^32 words.
//
// Two structural changes from the Java source (results are unchanged):
//   - Java threads explicit (array, start, length) triples everywhere because
//     Java arrays cannot be cheaply sub-viewed. Go slices ARE such a view, so
//     every function here takes a slice directly; a Java call on the "whole
//     array" is simply a call with the whole slice.
//   - The base-2^32 "tight" digits use Go's native uint32/uint64 instead of
//     Java's signed int/long plus "&0xffffffffL" masking, which Java needs
//     only because it has no unsigned integer type. The arithmetic performed
//     is identical.

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

// ---- decimal digits (base 10, one decimal digit per byte, most significant first) ----

func precedingZerosDigits(digits []byte) int {
	for i, d := range digits {
		if d != 0 {
			return i
		}
	}
	return len(digits)
}

func isZeroDigits(digits []byte) bool {
	return precedingZerosDigits(digits) == len(digits)
}

// optimizeDigits trims leading zero digits, always keeping at least one digit.
// It returns the input slice unchanged (same backing array) when nothing needs trimming.
func optimizeDigits(digits []byte) []byte {
	cutLength := precedingZerosDigits(digits)
	if cutLength > len(digits)-1 {
		cutLength = len(digits) - 1
	}
	if cutLength == 0 {
		return digits
	}
	result := make([]byte, len(digits)-cutLength)
	copy(result, digits[cutLength:])
	return result
}

// optimizeSignDigits forces the sign to non-negative when the magnitude is zero.
func optimizeSignDigits(negative bool, digits []byte) bool {
	if len(digits) == 1 && digits[0] == 0 {
		return false
	}
	return negative
}

// expandDigits left-pads digits with zeros to reach length. If length is not
// larger than len(digits), digits is returned unchanged.
func expandDigits(digits []byte, length int) []byte {
	if length <= len(digits) {
		return digits
	}
	result := make([]byte, length)
	copy(result[length-len(digits):], digits)
	return result
}

func passCarryDigit(digits []byte, sum byte, i int) byte {
	carry := sum / 10
	if carry == 0 {
		digits[i] = sum
	} else {
		digits[i] = sum % 10
	}
	return carry
}

func addDigits(digits []byte, other []byte) {
	diff := len(digits) - len(other)
	var carry byte = 0
	otherIndex := len(other) - 1
	for i := len(digits) - 1; i >= diff; i-- {
		sum := digits[i] + other[otherIndex] + carry
		carry = passCarryDigit(digits, sum, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		sum := digits[i] + carry
		carry = passCarryDigit(digits, sum, i)
	}
}

func passBorrowDigit(digits []byte, difference int, i int) byte {
	if difference < 0 {
		digits[i] = byte(difference + 10)
		return 1
	}
	digits[i] = byte(difference)
	return 0
}

func subtractDigits(digits []byte, other []byte) {
	diff := len(digits) - len(other)
	var borrow byte = 0
	otherIndex := len(other) - 1
	for i := len(digits) - 1; i >= diff; i-- {
		d := int(digits[i]) - int(other[otherIndex]) - int(borrow)
		borrow = passBorrowDigit(digits, d, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		d := int(digits[i]) - int(borrow)
		borrow = passBorrowDigit(digits, d, i)
	}
}

// multiplyDigits sets digits (assumed pre-zeroed, len(a)+len(b) long) to a*b.
func multiplyDigits(digits []byte, a []byte, b []byte) {
	for i := 1; i <= len(a); i++ {
		var carry byte = 0
		digitsIndex := len(digits) - i
		aIndex := len(a) - i
		for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
			sum := digits[digitsIndex] + a[aIndex]*b[bIndex] + carry
			carry = passCarryDigit(digits, sum, digitsIndex)
			digitsIndex--
		}
		if carry != 0 {
			digits[digitsIndex] += carry
		}
	}
}

// multiplyDigitsScalar sets digits (assumed pre-zeroed, len(b)+1 long) to a*b for a single digit a.
func multiplyDigitsScalar(digits []byte, a byte, b []byte) {
	var carry byte = 0
	digitsIndex := len(digits) - 1
	for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
		sum := digits[digitsIndex] + a*b[bIndex] + carry
		carry = passCarryDigit(digits, sum, digitsIndex)
		digitsIndex--
	}
	if carry != 0 {
		digits[digitsIndex] += carry
	}
}

func compareDigits(a []byte, b []byte) int {
	aStart := precedingZerosDigits(a)
	bStart := precedingZerosDigits(b)
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

// findRightBoundaryDigits returns, relative to the start of dividend, the last
// index of the shortest leading window of dividend that is >= divisor.
func findRightBoundaryDigits(dividend []byte, divisor []byte) int {
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

// multiplierDigits finds, via binary search, the largest single digit d such
// that d*divisor <= dividend, leaving that product in temp (len(divisor)+1 long).
func multiplierDigits(temp []byte, dividend []byte, divisor []byte) byte {
	var left, right byte = 0, 9
	mid := (left + right + 1) >> 1
outer:
	for left < right {
		mid = (left + right + 1) >> 1
		clear(temp)
		multiplyDigitsScalar(temp, mid, divisor)
		switch compareDigits(temp, dividend) {
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
		multiplyDigitsScalar(temp, left, divisor)
	}
	return left
}

// alignedTempDigits drops temp's leading digit when it is zero, so its length
// matches whichever dividend window multiplierDigits was solving for.
func alignedTempDigits(temp []byte) []byte {
	if temp[0] == 0 {
		return temp[1:]
	}
	return temp
}

func divideDigits(quotient []byte, dividend []byte, divisor []byte) {
	divisorSig := divisor[precedingZerosDigits(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]byte, len(divisorSig)+1)
	remaining := make([]byte, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZerosDigits(remaining)
	right := left + findRightBoundaryDigits(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplierDigits(temp, window, divisorSig)
		subtractDigits(window, alignedTempDigits(temp))
		left += precedingZerosDigits(remaining[left:])
		right = left + findRightBoundaryDigits(remaining[left:], divisorSig)
	}
}

func moduloDigits(remainder []byte, dividend []byte, divisor []byte) {
	divisorSig := divisor[precedingZerosDigits(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]byte, len(divisorSig)+1)
	remaining := make([]byte, len(dividend))
	copy(remaining, dividend)
	left := precedingZerosDigits(remaining)
	right := left + findRightBoundaryDigits(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		multiplierDigits(temp, window, divisorSig)
		subtractDigits(window, alignedTempDigits(temp))
		left += precedingZerosDigits(remaining[left:])
		right = left + findRightBoundaryDigits(remaining[left:], divisorSig)
	}
	start := precedingZerosDigits(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func divideAndModuloDigits(quotient []byte, remainder []byte, dividend []byte, divisor []byte) {
	divisorSig := divisor[precedingZerosDigits(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]byte, len(divisorSig)+1)
	remaining := make([]byte, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZerosDigits(remaining)
	right := left + findRightBoundaryDigits(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplierDigits(temp, window, divisorSig)
		subtractDigits(window, alignedTempDigits(temp))
		left += precedingZerosDigits(remaining[left:])
		right = left + findRightBoundaryDigits(remaining[left:], divisorSig)
	}
	start := precedingZerosDigits(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func gcdDigits(result []byte, a []byte, b []byte) {
	cmp := compareDigits(a, b)
	if cmp == 0 {
		target := min(len(result), min(len(a), len(b)))
		copy(result[len(result)-target:], a[len(a)-target:])
		return
	}
	maxLength := max(len(a), len(b))
	larger := make([]byte, maxLength)
	smaller := make([]byte, maxLength)
	if cmp > 0 {
		copy(larger[maxLength-len(a):], a)
		copy(smaller[maxLength-len(b):], b)
	} else {
		copy(larger[maxLength-len(b):], b)
		copy(smaller[maxLength-len(a):], a)
	}
	temp := make([]byte, maxLength)
	largerLeft := precedingZerosDigits(larger)
	smallerLeft := precedingZerosDigits(smaller)
	for !isZeroDigits(smaller[smallerLeft:]) {
		clear(temp[smallerLeft:])
		moduloDigits(temp[smallerLeft:], larger[largerLeft:], smaller[smallerLeft:])
		oldSmallerLeft := smallerLeft
		larger, largerLeft, smaller, temp = smaller, smallerLeft, temp, larger
		smallerLeft = oldSmallerLeft + precedingZerosDigits(smaller[oldSmallerLeft:])
	}
	copy(result[len(result)-(len(larger)-largerLeft):], larger[largerLeft:])
}

// ---- tight words (base 2^32, one word per uint32, most significant first) ----

func precedingZerosWords(words []uint32) int {
	for i, w := range words {
		if w != 0 {
			return i
		}
	}
	return len(words)
}

func isZeroWords(words []uint32) bool {
	return precedingZerosWords(words) == len(words)
}

func optimizeWords(words []uint32) []uint32 {
	cutLength := precedingZerosWords(words)
	if cutLength > len(words)-1 {
		cutLength = len(words) - 1
	}
	if cutLength == 0 {
		return words
	}
	result := make([]uint32, len(words)-cutLength)
	copy(result, words[cutLength:])
	return result
}

func optimizeSignWords(negative bool, words []uint32) bool {
	if len(words) == 1 && words[0] == 0 {
		return false
	}
	return negative
}

func expandWords(words []uint32, length int) []uint32 {
	if length <= len(words) {
		return words
	}
	result := make([]uint32, length)
	copy(result[length-len(words):], words)
	return result
}

// reverseAbs64 mirrors Java's Arithmetic.reverse_negative(long): the absolute
// value of a widened 64-bit number, used so that math.MinInt32 doesn't overflow.
func reverseAbs64(number int64) int64 {
	if number < 0 {
		return -number
	}
	return number
}

func passCarryWord(words []uint32, sum uint64, i int) uint32 {
	carry := uint32(sum >> 32)
	words[i] = uint32(sum & 0xffffffff)
	return carry
}

func addWords(words []uint32, other []uint32) {
	diff := len(words) - len(other)
	var carry uint32 = 0
	otherIndex := len(other) - 1
	for i := len(words) - 1; i >= diff; i-- {
		sum := uint64(words[i]) + uint64(other[otherIndex]) + uint64(carry)
		carry = passCarryWord(words, sum, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		sum := uint64(words[i]) + uint64(carry)
		carry = passCarryWord(words, sum, i)
	}
}

func passBorrowWord(words []uint32, difference int64, i int) uint32 {
	words[i] = uint32(difference)
	if difference < 0 {
		return 1
	}
	return 0
}

func subtractWords(words []uint32, other []uint32) {
	diff := len(words) - len(other)
	var borrow uint32 = 0
	otherIndex := len(other) - 1
	for i := len(words) - 1; i >= diff; i-- {
		d := int64(words[i]) - int64(other[otherIndex]) - int64(borrow)
		borrow = passBorrowWord(words, d, i)
		otherIndex--
	}
	for i := diff - 1; i >= 0; i-- {
		d := int64(words[i]) - int64(borrow)
		borrow = passBorrowWord(words, d, i)
	}
}

func multiplyWords(words []uint32, a []uint32, b []uint32) {
	for i := 1; i <= len(a); i++ {
		var carry uint32 = 0
		wordsIndex := len(words) - i
		aIndex := len(a) - i
		for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
			sum := uint64(words[wordsIndex]) + uint64(a[aIndex])*uint64(b[bIndex]) + uint64(carry)
			carry = passCarryWord(words, sum, wordsIndex)
			wordsIndex--
		}
		if carry != 0 {
			words[wordsIndex] += carry
		}
	}
}

func multiplyWordsScalar(words []uint32, a uint32, b []uint32) {
	var carry uint32 = 0
	wordsIndex := len(words) - 1
	for bIndex := len(b) - 1; bIndex >= 0; bIndex-- {
		sum := uint64(words[wordsIndex]) + uint64(a)*uint64(b[bIndex]) + uint64(carry)
		carry = passCarryWord(words, sum, wordsIndex)
		wordsIndex--
	}
	if carry != 0 {
		words[wordsIndex] += carry
	}
}

func compareWords(a []uint32, b []uint32) int {
	aStart := precedingZerosWords(a)
	bStart := precedingZerosWords(b)
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

func findRightBoundaryWords(dividend []uint32, divisor []uint32) int {
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

func multiplierWords(temp []uint32, dividend []uint32, divisor []uint32) uint32 {
	var left, right uint32 = 0, 0xffffffff
	mid := uint32((uint64(left) + uint64(right) + 1) >> 1)
outer:
	for left < right {
		mid = uint32((uint64(left) + uint64(right) + 1) >> 1)
		clear(temp)
		multiplyWordsScalar(temp, mid, divisor)
		switch compareWords(temp, dividend) {
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
		multiplyWordsScalar(temp, left, divisor)
	}
	return left
}

func alignedTempWords(temp []uint32) []uint32 {
	if temp[0] == 0 {
		return temp[1:]
	}
	return temp
}

func divideWords(quotient []uint32, dividend []uint32, divisor []uint32) {
	divisorSig := divisor[precedingZerosWords(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]uint32, len(divisorSig)+1)
	remaining := make([]uint32, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZerosWords(remaining)
	right := left + findRightBoundaryWords(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplierWords(temp, window, divisorSig)
		subtractWords(window, alignedTempWords(temp))
		left += precedingZerosWords(remaining[left:])
		right = left + findRightBoundaryWords(remaining[left:], divisorSig)
	}
}

func moduloWords(remainder []uint32, dividend []uint32, divisor []uint32) {
	divisorSig := divisor[precedingZerosWords(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]uint32, len(divisorSig)+1)
	remaining := make([]uint32, len(dividend))
	copy(remaining, dividend)
	left := precedingZerosWords(remaining)
	right := left + findRightBoundaryWords(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		multiplierWords(temp, window, divisorSig)
		subtractWords(window, alignedTempWords(temp))
		left += precedingZerosWords(remaining[left:])
		right = left + findRightBoundaryWords(remaining[left:], divisorSig)
	}
	start := precedingZerosWords(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func divideAndModuloWords(quotient []uint32, remainder []uint32, dividend []uint32, divisor []uint32) {
	divisorSig := divisor[precedingZerosWords(divisor):]
	if len(divisorSig) == 0 {
		panic("divide by 0")
	}
	temp := make([]uint32, len(divisorSig)+1)
	remaining := make([]uint32, len(dividend))
	copy(remaining, dividend)
	diff := len(quotient) - len(remaining)
	left := precedingZerosWords(remaining)
	right := left + findRightBoundaryWords(remaining[left:], divisorSig)
	for right < len(remaining) {
		window := remaining[left : right+1]
		quotient[diff+right] = multiplierWords(temp, window, divisorSig)
		subtractWords(window, alignedTempWords(temp))
		left += precedingZerosWords(remaining[left:])
		right = left + findRightBoundaryWords(remaining[left:], divisorSig)
	}
	start := precedingZerosWords(remaining)
	validLength := len(remaining) - start
	copy(remainder[len(remainder)-validLength:], remaining[start:])
}

func gcdWords(result []uint32, a []uint32, b []uint32) {
	cmp := compareWords(a, b)
	if cmp == 0 {
		target := min(len(result), min(len(a), len(b)))
		copy(result[len(result)-target:], a[len(a)-target:])
		return
	}
	maxLength := max(len(a), len(b))
	larger := make([]uint32, maxLength)
	smaller := make([]uint32, maxLength)
	if cmp > 0 {
		copy(larger[maxLength-len(a):], a)
		copy(smaller[maxLength-len(b):], b)
	} else {
		copy(larger[maxLength-len(b):], b)
		copy(smaller[maxLength-len(a):], a)
	}
	temp := make([]uint32, maxLength)
	largerLeft := precedingZerosWords(larger)
	smallerLeft := precedingZerosWords(smaller)
	for !isZeroWords(smaller[smallerLeft:]) {
		clear(temp[smallerLeft:])
		moduloWords(temp[smallerLeft:], larger[largerLeft:], smaller[smallerLeft:])
		oldSmallerLeft := smallerLeft
		larger, largerLeft, smaller, temp = smaller, smallerLeft, temp, larger
		smallerLeft = oldSmallerLeft + precedingZerosWords(smaller[oldSmallerLeft:])
	}
	copy(result[len(result)-(len(larger)-largerLeft):], larger[largerLeft:])
}

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
