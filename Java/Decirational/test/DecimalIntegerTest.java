package decirational;

public final class DecimalIntegerTest {
    private static void verifyDivisionIdentity(final TestFramework t, final String a_str, final String b_str) {
        final DecimalInteger a = new DecimalInteger(a_str);
        final DecimalInteger b = new DecimalInteger(b_str);
        final DecimalInteger[] qr = a.divideByAndModulo(b);
        final DecimalInteger reconstructed = qr[0].multiply(b).plus(qr[1]);
        t.checkEquals(a, reconstructed, "divideByAndModulo(" + a_str + "," + b_str + ") satisfies q*b+r==a");
        t.checkEquals(qr[0], a.divideBy(b), "divideByAndModulo(" + a_str + "," + b_str + ") quotient matches divideBy");
        t.checkEquals(qr[1], a.modulo(b), "divideByAndModulo(" + a_str + "," + b_str + ") remainder matches modulo");
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("DecimalInteger");

        // Construction / toString
        t.checkEquals("123", new DecimalInteger("123").toString(), "parse plain digits");
        t.checkEquals("-123", new DecimalInteger("-123").toString(), "parse negative digits");
        t.checkEquals("123", new DecimalInteger("+123").toString(), "parse explicit plus sign");
        t.checkEquals("123", new DecimalInteger("000123").toString(), "strip leading zeros");
        t.checkEquals("0", new DecimalInteger("0").toString(), "zero stays zero");
        t.checkEquals("0", new DecimalInteger("-0").toString(), "negative zero normalizes to zero");
        t.checkEquals("0", new DecimalInteger(0).toString(), "int constructor zero");
        t.checkEquals("42", new DecimalInteger(42).toString(), "int constructor positive");
        t.checkEquals("-42", new DecimalInteger(-42).toString(), "int constructor negative");
        t.checkEquals("123456789012345678901234567890", new DecimalInteger("123456789012345678901234567890").toString(), "large number round-trips");
        t.checkEquals(DecimalInteger.ZERO, new DecimalInteger("0"), "ZERO constant equals parsed zero");
        t.checkEquals(DecimalInteger.ONE, new DecimalInteger("1"), "ONE constant equals parsed one");
        t.checkEquals(DecimalInteger.getDigit(7), new DecimalInteger("7"), "getDigit(7) matches parsed 7");

        t.checkThrows(NumberFormatException.class, () -> new DecimalInteger((String) null), "null string rejected");
        t.checkThrows(NumberFormatException.class, () -> new DecimalInteger(""), "empty string rejected");
        t.checkThrows(NumberFormatException.class, () -> new DecimalInteger("-"), "bare sign rejected");
        t.checkThrows(NumberFormatException.class, () -> new DecimalInteger("12a"), "non-digit character rejected");
        t.checkThrows(NumberFormatException.class, () -> new DecimalInteger("1.5"), "decimal point rejected");
        t.checkThrows(IllegalArgumentException.class, () -> DecimalInteger.getDigit(10), "getDigit out of range rejected");

        // Predicates
        t.check(new DecimalInteger("0").isZero(), "0 is zero");
        t.check(!new DecimalInteger("1").isZero(), "1 is not zero");
        t.check(new DecimalInteger("1").isOne(), "1 is one");
        t.check(!new DecimalInteger("-1").isOne(), "-1 is not one");
        t.check(!new DecimalInteger("2").isOne(), "2 is not one");
        t.check(new DecimalInteger("1").isUnitAbs(), "1 has unit abs");
        t.check(new DecimalInteger("-1").isUnitAbs(), "-1 has unit abs");
        t.check(!new DecimalInteger("2").isUnitAbs(), "2 does not have unit abs");
        t.check(new DecimalInteger("5").isPositive(), "5 is positive");
        t.check(!new DecimalInteger("0").isPositive(), "0 is not positive");
        t.check(!new DecimalInteger("-5").isPositive(), "-5 is not positive");
        t.check(new DecimalInteger("-5").isNegative(), "-5 is negative");
        t.check(!new DecimalInteger("0").isNegative(), "0 is not negative");
        t.check(!new DecimalInteger("5").isNegative(), "5 is not negative");

        // negate / abs
        t.checkEquals("-5", new DecimalInteger("5").negate().toString(), "negate positive");
        t.checkEquals("5", new DecimalInteger("-5").negate().toString(), "negate negative");
        t.checkEquals("0", new DecimalInteger("0").negate().toString(), "negate zero stays zero");
        t.checkEquals("5", new DecimalInteger("-5").abs().toString(), "abs of negative");
        t.checkEquals("5", new DecimalInteger("5").abs().toString(), "abs of positive is unchanged");

        // compareTo
        t.check(new DecimalInteger("5").compareTo(new DecimalInteger("3")) > 0, "5 > 3");
        t.check(new DecimalInteger("3").compareTo(new DecimalInteger("5")) < 0, "3 < 5");
        t.check(0 == new DecimalInteger("5").compareTo(new DecimalInteger("5")), "5 == 5");
        t.check(new DecimalInteger("-5").compareTo(new DecimalInteger("3")) < 0, "-5 < 3");
        t.check(new DecimalInteger("-5").compareTo(new DecimalInteger("-3")) < 0, "-5 < -3");
        t.check(new DecimalInteger("-3").compareTo(new DecimalInteger("-5")) > 0, "-3 > -5");
        t.check(new DecimalInteger("100").compareTo(new DecimalInteger("99")) > 0, "longer positive beats shorter");
        t.check(new DecimalInteger("-100").compareTo(new DecimalInteger("-99")) < 0, "longer negative is smaller");

        // equals / hashCode
        t.checkEquals(new DecimalInteger("007"), new DecimalInteger(7), "equal values from different literals are equal");
        t.check(new DecimalInteger("007").hashCode() == new DecimalInteger(7).hashCode(), "equal values have equal hashCodes");
        t.check(!new DecimalInteger("5").equals(new DecimalInteger("-5")), "5 is not equal to -5");
        t.check(!new DecimalInteger("5").equals(null), "not equal to null");
        t.check(!new DecimalInteger("5").equals("5"), "not equal to different type");
        t.check(new DecimalInteger("5").equals(new DecimalInteger("5")), "equals is reflexive-consistent");

        // plus
        t.checkEquals("579", new DecimalInteger("123").plus(new DecimalInteger("456")).toString(), "123+456");
        t.checkEquals("-2", new DecimalInteger("-5").plus(new DecimalInteger("3")).toString(), "-5+3");
        t.checkEquals("2", new DecimalInteger("5").plus(new DecimalInteger("-3")).toString(), "5+(-3)");
        t.checkEquals("-8", new DecimalInteger("-5").plus(new DecimalInteger("-3")).toString(), "-5+(-3)");
        t.checkEquals("7", new DecimalInteger("0").plus(new DecimalInteger("7")).toString(), "0+7 shortcut");
        t.checkEquals("7", new DecimalInteger("7").plus(new DecimalInteger("0")).toString(), "7+0 shortcut");
        t.checkEquals("0", new DecimalInteger("5").plus(new DecimalInteger("-5")).toString(), "5+(-5)=0");

        // minus
        t.checkEquals("2", new DecimalInteger("5").minus(new DecimalInteger("3")).toString(), "5-3");
        t.checkEquals("-2", new DecimalInteger("3").minus(new DecimalInteger("5")).toString(), "3-5");
        t.checkEquals("-2", new DecimalInteger("-5").minus(new DecimalInteger("-3")).toString(), "-5-(-3)");
        t.checkEquals("8", new DecimalInteger("5").minus(new DecimalInteger("-3")).toString(), "5-(-3)");
        t.checkEquals("-7", new DecimalInteger("0").minus(new DecimalInteger("7")).toString(), "0-7");
        t.checkEquals("7", new DecimalInteger("7").minus(new DecimalInteger("0")).toString(), "7-0");

        // multiply
        t.checkEquals("144", new DecimalInteger("12").multiply(new DecimalInteger("12")).toString(), "12*12");
        t.checkEquals("-12", new DecimalInteger("-3").multiply(new DecimalInteger("4")).toString(), "-3*4");
        t.checkEquals("12", new DecimalInteger("-3").multiply(new DecimalInteger("-4")).toString(), "-3*-4");
        t.checkEquals("0", new DecimalInteger("0").multiply(new DecimalInteger("999")).toString(), "0*999");
        t.checkEquals("999", new DecimalInteger("1").multiply(new DecimalInteger("999")).toString(), "1*999 unit shortcut");
        t.checkEquals("-999", new DecimalInteger("-1").multiply(new DecimalInteger("999")).toString(), "-1*999 unit shortcut");
        t.checkEquals("-999", new DecimalInteger("999").multiply(new DecimalInteger("-1")).toString(), "999*-1 unit shortcut");

        // multiplyBase / divideByBase
        t.checkEquals("1200", new DecimalInteger("12").multiplyBase(2).toString(), "multiplyBase by 2");
        t.checkEquals("12", new DecimalInteger("12").multiplyBase(0).toString(), "multiplyBase by 0 is identity");
        t.checkEquals("-1200", new DecimalInteger("-12").multiplyBase(2).toString(), "multiplyBase preserves sign");
        t.checkEquals("12", new DecimalInteger("1234").divideByBase(2).toString(), "divideByBase by 2");
        t.checkEquals("1234", new DecimalInteger("1234").divideByBase(0).toString(), "divideByBase by 0 is identity");
        t.checkEquals("0", new DecimalInteger("1234").divideByBase(10).toString(), "divideByBase beyond length is zero");
        t.checkThrows(IllegalArgumentException.class, () -> new DecimalInteger("5").multiplyBase(-1), "multiplyBase rejects negative times");
        t.checkThrows(IllegalArgumentException.class, () -> new DecimalInteger("5").divideByBase(-1), "divideByBase rejects negative times");

        // divideBy / modulo
        t.checkEquals("12", new DecimalInteger("144").divideBy(new DecimalInteger("12")).toString(), "144/12");
        t.checkEquals("-12", new DecimalInteger("-144").divideBy(new DecimalInteger("12")).toString(), "-144/12");
        t.checkEquals("3", new DecimalInteger("7").divideBy(new DecimalInteger("2")).toString(), "7/2 truncates");
        t.checkEquals("-3", new DecimalInteger("-7").divideBy(new DecimalInteger("2")).toString(), "-7/2 truncates toward zero");
        t.checkEquals("1", new DecimalInteger("7").modulo(new DecimalInteger("2")).toString(), "7%2");
        t.checkEquals("-1", new DecimalInteger("-7").modulo(new DecimalInteger("2")).toString(), "-7%2 keeps dividend sign");
        t.checkThrows(ArithmeticException.class, () -> new DecimalInteger("5").divideBy(new DecimalInteger("0")), "divide by zero throws");
        t.checkThrows(ArithmeticException.class, () -> new DecimalInteger("5").modulo(new DecimalInteger("0")), "modulo by zero throws");

        verifyDivisionIdentity(t, "17", "5");
        verifyDivisionIdentity(t, "-17", "5");
        verifyDivisionIdentity(t, "17", "-5");
        verifyDivisionIdentity(t, "-17", "-5");
        verifyDivisionIdentity(t, "100", "7");
        verifyDivisionIdentity(t, "0", "5");
        verifyDivisionIdentity(t, "999999999999999999999", "37");
        verifyDivisionIdentity(t, "1", "1");

        // gcd / lcm
        t.checkEquals("6", new DecimalInteger("48").gcd(new DecimalInteger("18")).toString(), "gcd(48,18)");
        t.checkEquals("1", new DecimalInteger("17").gcd(new DecimalInteger("5")).toString(), "gcd of coprimes");
        t.checkEquals("25", new DecimalInteger("100").gcd(new DecimalInteger("75")).toString(), "gcd(100,75)");
        t.checkEquals("5", new DecimalInteger("0").gcd(new DecimalInteger("5")).toString(), "gcd(0,x)=x");
        t.checkEquals("5", new DecimalInteger("5").gcd(new DecimalInteger("0")).toString(), "gcd(x,0)=x");
        t.checkEquals("6", new DecimalInteger("-12").gcd(new DecimalInteger("18")).toString(), "gcd ignores sign");
        t.checkEquals("5", new DecimalInteger("0").gcd(new DecimalInteger("-5")).toString(), "gcd(0,-x)=x");
        t.checkEquals("5", new DecimalInteger("-5").gcd(new DecimalInteger("0")).toString(), "gcd(-x,0)=x");
        t.checkEquals("42", new DecimalInteger("21").lcm(new DecimalInteger("6")).toString(), "lcm(21,6)");
        t.checkEquals("0", new DecimalInteger("0").lcm(new DecimalInteger("5")).toString(), "lcm(0,x)=0");

        // pow
        t.checkEquals("1024", new DecimalInteger("2").pow(10).toString(), "2^10");
        t.checkEquals("1", new DecimalInteger("5").pow(0).toString(), "x^0=1");
        t.checkEquals("1", new DecimalInteger("0").pow(0).toString(), "0^0=1 by convention");
        t.checkEquals("5", new DecimalInteger("5").pow(1).toString(), "x^1=x");
        t.checkEquals("-8", new DecimalInteger("-2").pow(3).toString(), "(-2)^3 is negative");
        t.checkEquals("4", new DecimalInteger("-2").pow(2).toString(), "(-2)^2 is positive");
        t.checkThrows(IllegalArgumentException.class, () -> new DecimalInteger("2").pow(-1), "negative exponent rejected");

        return t;
    }
}
