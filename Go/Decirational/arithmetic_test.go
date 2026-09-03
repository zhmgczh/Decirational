package decirational

import "testing"

func TestArithmeticDigitsHelpers(t *testing.T) {
	check := func(got, want []byte, desc string) {
		t.Helper()
		if !bytesEqual(got, want) {
			t.Errorf("%s: got %v, want %v", desc, got, want)
		}
	}
	check(optimizeDigits([]byte{0, 0, 1, 2}), []byte{1, 2}, "optimizeDigits strips leading zeros")
	check(optimizeDigits([]byte{0, 0, 0}), []byte{0}, "optimizeDigits of all zeros keeps a single zero")
	check(optimizeDigits([]byte{5}), []byte{5}, "optimizeDigits leaves a single nonzero digit alone")

	if optimizeSignDigits(true, []byte{0}) {
		t.Error("optimizeSignDigits forces sign false for zero magnitude")
	}
	if !optimizeSignDigits(true, []byte{5}) {
		t.Error("optimizeSignDigits preserves sign for nonzero magnitude")
	}

	check(expandDigits([]byte{1, 2}, 4), []byte{0, 0, 1, 2}, "expandDigits left-pads with zeros")
	check(expandDigits([]byte{1, 2}, 2), []byte{1, 2}, "expandDigits is a no-op when length already matches")
	check(expandDigits([]byte{1, 2}, 1), []byte{1, 2}, "expandDigits is a no-op when requested length is smaller")

	if compareDigits([]byte{1, 2, 3}, []byte{1, 2, 3}) != 0 {
		t.Error("compareDigits equal arrays")
	}
	if compareDigits([]byte{1, 2, 3}, []byte{1, 2, 4}) >= 0 {
		t.Error("compareDigits less-than on last digit")
	}
	if compareDigits([]byte{2}, []byte{1, 9}) >= 0 {
		t.Error("compareDigits shorter true magnitude beats longer")
	}
	if compareDigits([]byte{0, 0, 5}, []byte{5}) != 0 {
		t.Error("compareDigits ignores leading zero padding")
	}
	if compareDigits([]byte{9}, []byte{1}) <= 0 {
		t.Error("compareDigits greater-than")
	}
}

func TestArithmeticWordsHelpers(t *testing.T) {
	checkW := func(got, want []uint32, desc string) {
		t.Helper()
		if !wordsEqual(got, want) {
			t.Errorf("%s: got %v, want %v", desc, got, want)
		}
	}
	checkW(optimizeWords([]uint32{0, 0, 1, 2}), []uint32{1, 2}, "optimizeWords strips leading zeros")
	checkW(optimizeWords([]uint32{0, 0, 0}), []uint32{0}, "optimizeWords of all zeros keeps a single zero")

	if optimizeSignWords(true, []uint32{0}) {
		t.Error("optimizeSignWords forces sign false for zero magnitude")
	}

	if compareWords([]uint32{1, 2}, []uint32{1, 2}) != 0 {
		t.Error("compareWords equal arrays")
	}
	if compareWords([]uint32{1}, []uint32{2}) >= 0 {
		t.Error("compareWords less-than")
	}
	if compareWords([]uint32{0, 7}, []uint32{7}) != 0 {
		t.Error("compareWords ignores leading zero padding")
	}
	// Words are unsigned: 0xffffffff must compare greater than 1, unlike a signed int32 would.
	if compareWords([]uint32{0xffffffff}, []uint32{1}) <= 0 {
		t.Error("compareWords treats words as unsigned magnitude")
	}
}

func bytesEqual(a, b []byte) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func wordsEqual(a, b []uint32) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
