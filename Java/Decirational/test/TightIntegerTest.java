package decirational;

public final class TightIntegerTest {
    private static void verifyDivisionIdentity(final TestFramework t, final String a_str, final String b_str) {
        final TightInteger a = new TightInteger(a_str);
        final TightInteger b = new TightInteger(b_str);
        final TightInteger[] qr = a.divideByAndModulo(b);
        final TightInteger reconstructed = qr[0].multiply(b).plus(qr[1]);
        t.checkEquals(a, reconstructed, "divideByAndModulo(" + a_str + "," + b_str + ") satisfies q*b+r==a");
        t.checkEquals(qr[0], a.divideBy(b), "divideByAndModulo(" + a_str + "," + b_str + ") quotient matches divideBy");
        t.checkEquals(qr[1], a.modulo(b), "divideByAndModulo(" + a_str + "," + b_str + ") remainder matches modulo");
    }

    private static void verifyRoundTrip(final TestFramework t, final String value) {
        t.checkEquals(value, new TightInteger(value).toString(), "TightInteger round-trip for " + value);
        t.checkEquals(new DecimalInteger(value), new TightInteger(value).toDecimalInteger(), "toDecimalInteger matches for " + value);
        t.checkEquals(value, new DecimalInteger(value).toTightInteger().toDecimalInteger().toString(), "DecimalInteger->TightInteger->DecimalInteger round-trip for " + value);
        t.checkEquals(value, new TightInteger(new DecimalInteger(value)).toString(), "TightInteger(DecimalInteger) constructor for " + value);
    }

    private static void crossCheck(final TestFramework t, final String a_str, final String b_str) {
        final DecimalInteger da = new DecimalInteger(a_str);
        final DecimalInteger db = new DecimalInteger(b_str);
        final TightInteger ta = new TightInteger(a_str);
        final TightInteger tb = new TightInteger(b_str);
        t.checkEquals(da.plus(db).toString(), ta.plus(tb).toString(), "plus cross-check " + a_str + "+" + b_str);
        t.checkEquals(da.minus(db).toString(), ta.minus(tb).toString(), "minus cross-check " + a_str + "-" + b_str);
        t.checkEquals(da.multiply(db).toString(), ta.multiply(tb).toString(), "multiply cross-check " + a_str + "*" + b_str);
        if (!db.isZero()) {
            t.checkEquals(da.divideBy(db).toString(), ta.divideBy(tb).toString(), "divideBy cross-check " + a_str + "/" + b_str);
            t.checkEquals(da.modulo(db).toString(), ta.modulo(tb).toString(), "modulo cross-check " + a_str + "%" + b_str);
        }
        t.checkEquals(da.gcd(db).toString(), ta.gcd(tb).toString(), "gcd cross-check " + a_str + "," + b_str);
        if (!da.isZero() && !db.isZero()) {
            t.checkEquals(da.lcm(db).toString(), ta.lcm(tb).toString(), "lcm cross-check " + a_str + "," + b_str);
        }
        t.checkEquals(da.pow(3).toString(), ta.pow(3).toString(), "pow(3) cross-check " + a_str);
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("TightInteger");

        // Construction / toString (mirrors DecimalInteger contract)
        t.checkEquals("123", new TightInteger("123").toString(), "parse plain digits");
        t.checkEquals("-123", new TightInteger("-123").toString(), "parse negative digits");
        t.checkEquals("0", new TightInteger("0").toString(), "zero stays zero");
        t.checkEquals("0", new TightInteger("-0").toString(), "negative zero normalizes to zero");
        t.checkEquals("0", new TightInteger(0).toString(), "int constructor zero");
        t.checkEquals("42", new TightInteger(42).toString(), "int constructor positive");
        t.checkEquals("-42", new TightInteger(-42).toString(), "int constructor negative");
        t.checkEquals(TightInteger.ZERO, new TightInteger("0"), "ZERO constant equals parsed zero");
        t.checkEquals(TightInteger.ONE, new TightInteger("1"), "ONE constant equals parsed one");
        t.checkEquals(TightInteger.getDigit(1), new TightInteger("1"), "getDigit(1) matches parsed 1");
        t.checkThrows(IllegalArgumentException.class, () -> TightInteger.getDigit(2), "getDigit out of range rejected");

        // Predicates
        t.check(new TightInteger("0").isZero(), "0 is zero");
        t.check(new TightInteger("1").isOne(), "1 is one");
        t.check(!new TightInteger("-1").isOne(), "-1 is not one");
        t.check(new TightInteger("-1").isUnitAbs(), "-1 has unit abs");
        t.check(new TightInteger("5").isPositive(), "5 is positive");
        t.check(new TightInteger("-5").isNegative(), "-5 is negative");

        // negate / abs
        t.checkEquals("-5", new TightInteger("5").negate().toString(), "negate positive");
        t.checkEquals("5", new TightInteger("-5").abs().toString(), "abs of negative");
        t.checkEquals("0", new TightInteger("0").negate().toString(), "negate zero stays zero");

        // compareTo
        t.check(new TightInteger("5").compareTo(new TightInteger("3")) > 0, "5 > 3");
        t.check(new TightInteger("-5").compareTo(new TightInteger("-3")) < 0, "-5 < -3");
        t.check(0 == new TightInteger("5").compareTo(new TightInteger("5")), "5 == 5");

        // equals / hashCode
        t.checkEquals(new TightInteger("007"), new TightInteger(7), "equal values from different literals are equal");
        t.check(new TightInteger("007").hashCode() == new TightInteger(7).hashCode(), "equal values have equal hashCodes");
        t.check(!new TightInteger("5").equals(new TightInteger("-5")), "5 is not equal to -5");
        t.check(!new TightInteger("5").equals(null), "not equal to null");

        // plus / minus / multiply basics
        t.checkEquals("579", new TightInteger("123").plus(new TightInteger("456")).toString(), "123+456");
        t.checkEquals("2", new TightInteger("5").minus(new TightInteger("3")).toString(), "5-3");
        t.checkEquals("144", new TightInteger("12").multiply(new TightInteger("12")).toString(), "12*12");
        t.checkEquals("-999", new TightInteger("-1").multiply(new TightInteger("999")).toString(), "-1*999 unit shortcut");

        // multiplyBase / divideByBase (base here is 2^32, verify via round trip rather than literal digits)
        t.checkEquals(new TightInteger("12"), new TightInteger("12").multiplyBase(2).divideByBase(2), "multiplyBase then divideByBase is identity");
        t.checkEquals("12", new TightInteger("12").multiplyBase(0).toString(), "multiplyBase by 0 is identity");
        t.checkThrows(IllegalArgumentException.class, () -> new TightInteger("5").multiplyBase(-1), "multiplyBase rejects negative times");
        t.checkThrows(IllegalArgumentException.class, () -> new TightInteger("5").divideByBase(-1), "divideByBase rejects negative times");

        // divideBy / modulo
        t.checkEquals("12", new TightInteger("144").divideBy(new TightInteger("12")).toString(), "144/12");
        t.checkEquals("-3", new TightInteger("-7").divideBy(new TightInteger("2")).toString(), "-7/2 truncates toward zero");
        t.checkEquals("-1", new TightInteger("-7").modulo(new TightInteger("2")).toString(), "-7%2 keeps dividend sign");
        t.checkThrows(ArithmeticException.class, () -> new TightInteger("5").divideBy(new TightInteger("0")), "divide by zero throws");
        t.checkThrows(ArithmeticException.class, () -> new TightInteger("5").modulo(new TightInteger("0")), "modulo by zero throws");

        verifyDivisionIdentity(t, "17", "5");
        verifyDivisionIdentity(t, "-17", "5");
        verifyDivisionIdentity(t, "123456789012345678901234567890", "987654321");
        verifyDivisionIdentity(t, "0", "5");

        // gcd / lcm
        t.checkEquals("6", new TightInteger("48").gcd(new TightInteger("18")).toString(), "gcd(48,18)");
        t.checkEquals("5", new TightInteger("0").gcd(new TightInteger("-5")).toString(), "gcd(0,-x)=x");
        t.checkEquals("5", new TightInteger("-5").gcd(new TightInteger("0")).toString(), "gcd(-x,0)=x");
        t.checkEquals("42", new TightInteger("21").lcm(new TightInteger("6")).toString(), "lcm(21,6)");

        // pow
        t.checkEquals("1024", new TightInteger("2").pow(10).toString(), "2^10");
        t.checkEquals("1", new TightInteger("5").pow(0).toString(), "x^0=1");
        t.checkThrows(IllegalArgumentException.class, () -> new TightInteger("2").pow(-1), "negative exponent rejected");

        // Round trips between TightInteger's base-2^32 representation and DecimalInteger's base-10 one
        verifyRoundTrip(t, "0");
        verifyRoundTrip(t, "1");
        verifyRoundTrip(t, "-1");
        verifyRoundTrip(t, "9");
        verifyRoundTrip(t, "10");
        verifyRoundTrip(t, "99");
        verifyRoundTrip(t, "100");
        verifyRoundTrip(t, "2147483647");
        verifyRoundTrip(t, "2147483648");
        verifyRoundTrip(t, "4294967295");
        verifyRoundTrip(t, "4294967296");
        verifyRoundTrip(t, "-2147483648");
        verifyRoundTrip(t, "123456789012345678901234567890");
        verifyRoundTrip(t, "-999999999999999999999999999999999999999");

        // Cross-validate arithmetic against the independently-implemented DecimalInteger
        crossCheck(t, "123456789012345678901234567890", "987654321098765432109876543210");
        crossCheck(t, "1000000000000000000000000000000", "3");
        crossCheck(t, "-123456789", "456");
        crossCheck(t, "0", "999999999999999999999");
        crossCheck(t, "7", "7");
        crossCheck(t, "-999999999999999999999999999999999999999", "-1");
        crossCheck(t, "4294967296", "4294967295");

        return t;
    }
}
