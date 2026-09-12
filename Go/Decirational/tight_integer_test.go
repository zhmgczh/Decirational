package decirational

import "testing"

func mustT(t *testing.T, s string) TightInteger {
	t.Helper()
	v, err := ParseTightInteger(s)
	if err != nil {
		t.Fatalf("ParseTightInteger(%q): %v", s, err)
	}
	return v
}

func TestTightIntegerConstruction(t *testing.T) {
	cases := []struct{ input, want string }{
		{"123", "123"}, {"-123", "-123"}, {"0", "0"}, {"-0", "0"},
	}
	for _, c := range cases {
		if got := mustT(t, c.input).String(); got != c.want {
			t.Errorf("parse(%q) = %q, want %q", c.input, got, c.want)
		}
	}
	if NewTightIntegerFromInt32(0).String() != "0" {
		t.Error("int32 constructor zero")
	}
	if NewTightIntegerFromInt32(42).String() != "42" {
		t.Error("int32 constructor positive")
	}
	if NewTightIntegerFromInt32(-42).String() != "-42" {
		t.Error("int32 constructor negative")
	}
	if got := NewTightIntegerFromInt32(-2147483648).String(); got != "-2147483648" {
		t.Errorf("int32 constructor handles math.MinInt32 without overflow: got %s", got)
	}
	if !TightZero.Equals(mustT(t, "0")) {
		t.Error("TightZero equals parsed zero")
	}
	if !TightOne.Equals(mustT(t, "1")) {
		t.Error("TightOne equals parsed one")
	}
	if one, err := GetTightDigit(1); err != nil || !one.Equals(mustT(t, "1")) {
		t.Error("GetTightDigit(1) matches parsed 1")
	}
	if _, err := GetTightDigit(2); err == nil {
		t.Error("GetTightDigit(2) should be rejected")
	}
	if _, err := NewTightInteger(nil, false); err == nil {
		t.Error("NewTightInteger with empty words should be rejected")
	}
}

func TestTightIntegerPredicates(t *testing.T) {
	if !mustT(t, "0").IsZero() {
		t.Error("0 is zero")
	}
	if !mustT(t, "1").IsOne() || mustT(t, "-1").IsOne() {
		t.Error("IsOne")
	}
	if !mustT(t, "-1").IsUnitAbs() {
		t.Error("-1 has unit abs")
	}
	if !mustT(t, "5").IsPositive() {
		t.Error("5 is positive")
	}
	if !mustT(t, "-5").IsNegative() {
		t.Error("-5 is negative")
	}
}

func TestTightIntegerNegateAbs(t *testing.T) {
	if got := mustT(t, "5").Negate().String(); got != "-5" {
		t.Errorf("negate positive: %s", got)
	}
	if got := mustT(t, "-5").Abs().String(); got != "5" {
		t.Errorf("abs of negative: %s", got)
	}
	if got := mustT(t, "0").Negate().String(); got != "0" {
		t.Errorf("negate zero stays zero: %s", got)
	}
}

func TestTightIntegerCompare(t *testing.T) {
	if sign(mustT(t, "5").Compare(mustT(t, "3"))) != 1 {
		t.Error("5 > 3")
	}
	if sign(mustT(t, "-5").Compare(mustT(t, "-3"))) != -1 {
		t.Error("-5 < -3")
	}
	if mustT(t, "5").Compare(mustT(t, "5")) != 0 {
		t.Error("5 == 5")
	}
}

func TestTightIntegerEquals(t *testing.T) {
	if !mustT(t, "007").Equals(NewTightIntegerFromInt32(7)) {
		t.Error("equal values from different literals are equal")
	}
	if mustT(t, "5").Equals(mustT(t, "-5")) {
		t.Error("5 should not equal -5")
	}
}

func TestTightIntegerArithmeticBasics(t *testing.T) {
	if got := mustT(t, "123").Plus(mustT(t, "456")).String(); got != "579" {
		t.Errorf("123+456 = %s", got)
	}
	if got := mustT(t, "5").Minus(mustT(t, "3")).String(); got != "2" {
		t.Errorf("5-3 = %s", got)
	}
	if got := mustT(t, "12").Multiply(mustT(t, "12")).String(); got != "144" {
		t.Errorf("12*12 = %s", got)
	}
	if got := mustT(t, "-1").Multiply(mustT(t, "999")).String(); got != "-999" {
		t.Errorf("-1*999 unit shortcut: %s", got)
	}
	if got := mustT(t, "144").DivideBy(mustT(t, "12")).String(); got != "12" {
		t.Errorf("144/12 = %s", got)
	}
	if got := mustT(t, "-7").DivideBy(mustT(t, "2")).String(); got != "-3" {
		t.Errorf("-7/2 truncates toward zero: %s", got)
	}
	if got := mustT(t, "-7").Modulo(mustT(t, "2")).String(); got != "-1" {
		t.Errorf("-7%%2 keeps dividend sign: %s", got)
	}
	expectPanic(t, "divide by zero panics", func() { mustT(t, "5").DivideBy(TightZero) })
	expectPanic(t, "modulo by zero panics", func() { mustT(t, "5").Modulo(TightZero) })
	expectPanic(t, "multiply_base rejects negative times", func() { mustT(t, "5").MultiplyBase(-1) })
	expectPanic(t, "divide_by_base rejects negative times", func() { mustT(t, "5").DivideByBase(-1) })
}

func TestTightIntegerMultiplyDivideBaseIdentity(t *testing.T) {
	if got := mustT(t, "12").MultiplyBase(2).DivideByBase(2); !got.Equals(mustT(t, "12")) {
		t.Errorf("multiply_base then divide_by_base is identity: got %s", got)
	}
	if got := mustT(t, "12").MultiplyBase(0).String(); got != "12" {
		t.Errorf("multiply_base by 0 is identity: %s", got)
	}
}

func verifyTightDivisionIdentity(t *testing.T, aStr, bStr string) {
	t.Helper()
	a, b := mustT(t, aStr), mustT(t, bStr)
	qr := a.DivideByAndModulo(b)
	reconstructed := qr[0].Multiply(b).Plus(qr[1])
	if !reconstructed.Equals(a) {
		t.Errorf("(%s)/(%s): q*b+r = %s, want %s", aStr, bStr, reconstructed, a)
	}
	if !qr[0].Equals(a.DivideBy(b)) {
		t.Errorf("(%s)/(%s): quotient mismatch with DivideBy", aStr, bStr)
	}
	if !qr[1].Equals(a.Modulo(b)) {
		t.Errorf("(%s)/(%s): remainder mismatch with Modulo", aStr, bStr)
	}
}

func TestTightIntegerDivisionIdentity(t *testing.T) {
	pairs := [][2]string{{"17", "5"}, {"-17", "5"}, {"123456789012345678901234567890", "987654321"}, {"0", "5"}}
	for _, p := range pairs {
		verifyTightDivisionIdentity(t, p[0], p[1])
	}
}

func TestTightIntegerGcdPow(t *testing.T) {
	if got := mustT(t, "48").Gcd(mustT(t, "18")).String(); got != "6" {
		t.Errorf("gcd(48,18) = %s", got)
	}
	if got := mustT(t, "0").Gcd(mustT(t, "-5")).String(); got != "5" {
		t.Errorf("gcd(0,-5) = %s", got)
	}
	if got := mustT(t, "-5").Gcd(mustT(t, "0")).String(); got != "5" {
		t.Errorf("gcd(-5,0) = %s", got)
	}
	if got := mustT(t, "21").Lcm(mustT(t, "6")).String(); got != "42" {
		t.Errorf("lcm(21,6) = %s", got)
	}
	if got := mustT(t, "2").Pow(10).String(); got != "1024" {
		t.Errorf("2^10 = %s", got)
	}
	if got := mustT(t, "5").Pow(0).String(); got != "1" {
		t.Errorf("x^0=1: %s", got)
	}
	expectPanic(t, "negative exponent rejected", func() { mustT(t, "2").Pow(-1) })
}

func verifyTightRoundTrip(t *testing.T, value string) {
	t.Helper()
	tight := mustT(t, value)
	if got := tight.String(); got != value {
		t.Errorf("TightInteger round-trip for %s: got %s", value, got)
	}
	if got := tight.ToDecimalInteger().String(); got != value {
		t.Errorf("ToDecimalInteger round-trip for %s: got %s", value, got)
	}
	if got := mustD(t, value).ToTightInteger().ToDecimalInteger().String(); got != value {
		t.Errorf("DecimalInteger->TightInteger->DecimalInteger round-trip for %s: got %s", value, got)
	}
}

func TestTightIntegerRoundTrips(t *testing.T) {
	values := []string{
		"0", "1", "-1", "9", "10", "99", "100",
		"2147483647", "2147483648", "4294967295", "4294967296",
		"-2147483648", "123456789012345678901234567890",
		"-999999999999999999999999999999999999999",
		// Exercise convertWordsToDigits's 9-decimal-digit chunking directly:
		// exactly one chunk, exactly two chunks, and one digit into a third
		// chunk, each with a leading digit that must not become a spurious
		// leading zero once the top chunk's unused high digits are trimmed.
		"999999999", "-999999999", "100000000", "123456789123456789",
		"-123456789123456789", "1000000000000000001",
	}
	for _, v := range values {
		verifyTightRoundTrip(t, v)
	}
}

func crossCheckTightVsDecimal(t *testing.T, aStr, bStr string) {
	t.Helper()
	da, db := mustD(t, aStr), mustD(t, bStr)
	ta, tb := mustT(t, aStr), mustT(t, bStr)
	if got, want := ta.Plus(tb).String(), da.Plus(db).String(); got != want {
		t.Errorf("plus(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
	}
	if got, want := ta.Minus(tb).String(), da.Minus(db).String(); got != want {
		t.Errorf("minus(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
	}
	if got, want := ta.Multiply(tb).String(), da.Multiply(db).String(); got != want {
		t.Errorf("multiply(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
	}
	if !db.IsZero() {
		if got, want := ta.DivideBy(tb).String(), da.DivideBy(db).String(); got != want {
			t.Errorf("divide(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
		}
		if got, want := ta.Modulo(tb).String(), da.Modulo(db).String(); got != want {
			t.Errorf("modulo(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
		}
	}
	if got, want := ta.Gcd(tb).String(), da.Gcd(db).String(); got != want {
		t.Errorf("gcd(%s,%s): tight=%s decimal=%s", aStr, bStr, got, want)
	}
	if got, want := ta.Pow(3).String(), da.Pow(3).String(); got != want {
		t.Errorf("pow3(%s): tight=%s decimal=%s", aStr, got, want)
	}
}

func TestTightVsDecimalCrossCheck(t *testing.T) {
	crossCheckTightVsDecimal(t, "123456789012345678901234567890", "987654321098765432109876543210")
	crossCheckTightVsDecimal(t, "1000000000000000000000000000000", "3")
	crossCheckTightVsDecimal(t, "-123456789", "456")
	crossCheckTightVsDecimal(t, "0", "999999999999999999999")
	crossCheckTightVsDecimal(t, "4294967296", "4294967295")
	crossCheckTightVsDecimal(t, "4294967295", "4294967295") // both words at uint32 max: exercises the widest carry
	crossCheckTightVsDecimal(t, "-999999999999999999999999999999999999999", "-1")
}
