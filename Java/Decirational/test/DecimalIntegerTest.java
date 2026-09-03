public final class DecimalIntegerTest {
    private static void verify_division_identity(final TestFramework t, final String a_str, final String b_str) {
        final DecimalInteger a = new DecimalInteger(a_str);
        final DecimalInteger b = new DecimalInteger(b_str);
        final DecimalInteger[] qr = a.divide_by_and_modulo(b);
        final DecimalInteger reconstructed = qr[0].multiply(b).plus(qr[1]);
        t.check_equals(a, reconstructed, "divide_by_and_modulo(" + a_str + "," + b_str + ") satisfies q*b+r==a");
        t.check_equals(qr[0], a.divide_by(b), "divide_by_and_modulo(" + a_str + "," + b_str + ") quotient matches divide_by");
        t.check_equals(qr[1], a.modulo(b), "divide_by_and_modulo(" + a_str + "," + b_str + ") remainder matches modulo");
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("DecimalInteger");

        // Construction / toString
        t.check_equals("123", new DecimalInteger("123").toString(), "parse plain digits");
        t.check_equals("-123", new DecimalInteger("-123").toString(), "parse negative digits");
        t.check_equals("123", new DecimalInteger("+123").toString(), "parse explicit plus sign");
        t.check_equals("123", new DecimalInteger("000123").toString(), "strip leading zeros");
        t.check_equals("0", new DecimalInteger("0").toString(), "zero stays zero");
        t.check_equals("0", new DecimalInteger("-0").toString(), "negative zero normalizes to zero");
        t.check_equals("0", new DecimalInteger(0).toString(), "int constructor zero");
        t.check_equals("42", new DecimalInteger(42).toString(), "int constructor positive");
        t.check_equals("-42", new DecimalInteger(-42).toString(), "int constructor negative");
        t.check_equals("123456789012345678901234567890", new DecimalInteger("123456789012345678901234567890").toString(), "large number round-trips");
        t.check_equals(DecimalInteger.ZERO, new DecimalInteger("0"), "ZERO constant equals parsed zero");
        t.check_equals(DecimalInteger.ONE, new DecimalInteger("1"), "ONE constant equals parsed one");
        t.check_equals(DecimalInteger.get_digit(7), new DecimalInteger("7"), "get_digit(7) matches parsed 7");

        t.check_throws(NumberFormatException.class, () -> new DecimalInteger((String) null), "null string rejected");
        t.check_throws(NumberFormatException.class, () -> new DecimalInteger(""), "empty string rejected");
        t.check_throws(NumberFormatException.class, () -> new DecimalInteger("-"), "bare sign rejected");
        t.check_throws(NumberFormatException.class, () -> new DecimalInteger("12a"), "non-digit character rejected");
        t.check_throws(NumberFormatException.class, () -> new DecimalInteger("1.5"), "decimal point rejected");
        t.check_throws(IllegalArgumentException.class, () -> DecimalInteger.get_digit(10), "get_digit out of range rejected");

        // Predicates
        t.check(new DecimalInteger("0").is_zero(), "0 is zero");
        t.check(!new DecimalInteger("1").is_zero(), "1 is not zero");
        t.check(new DecimalInteger("1").is_one(), "1 is one");
        t.check(!new DecimalInteger("-1").is_one(), "-1 is not one");
        t.check(!new DecimalInteger("2").is_one(), "2 is not one");
        t.check(new DecimalInteger("1").is_unit_abs(), "1 has unit abs");
        t.check(new DecimalInteger("-1").is_unit_abs(), "-1 has unit abs");
        t.check(!new DecimalInteger("2").is_unit_abs(), "2 does not have unit abs");
        t.check(new DecimalInteger("5").is_positive(), "5 is positive");
        t.check(!new DecimalInteger("0").is_positive(), "0 is not positive");
        t.check(!new DecimalInteger("-5").is_positive(), "-5 is not positive");
        t.check(new DecimalInteger("-5").is_negative(), "-5 is negative");
        t.check(!new DecimalInteger("0").is_negative(), "0 is not negative");
        t.check(!new DecimalInteger("5").is_negative(), "5 is not negative");

        // negate / abs
        t.check_equals("-5", new DecimalInteger("5").negate().toString(), "negate positive");
        t.check_equals("5", new DecimalInteger("-5").negate().toString(), "negate negative");
        t.check_equals("0", new DecimalInteger("0").negate().toString(), "negate zero stays zero");
        t.check_equals("5", new DecimalInteger("-5").abs().toString(), "abs of negative");
        t.check_equals("5", new DecimalInteger("5").abs().toString(), "abs of positive is unchanged");

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
        t.check_equals(new DecimalInteger("007"), new DecimalInteger(7), "equal values from different literals are equal");
        t.check(new DecimalInteger("007").hashCode() == new DecimalInteger(7).hashCode(), "equal values have equal hashCodes");
        t.check(!new DecimalInteger("5").equals(new DecimalInteger("-5")), "5 is not equal to -5");
        t.check(!new DecimalInteger("5").equals(null), "not equal to null");
        t.check(!new DecimalInteger("5").equals("5"), "not equal to different type");
        t.check(new DecimalInteger("5").equals(new DecimalInteger("5")), "equals is reflexive-consistent");

        // plus
        t.check_equals("579", new DecimalInteger("123").plus(new DecimalInteger("456")).toString(), "123+456");
        t.check_equals("-2", new DecimalInteger("-5").plus(new DecimalInteger("3")).toString(), "-5+3");
        t.check_equals("2", new DecimalInteger("5").plus(new DecimalInteger("-3")).toString(), "5+(-3)");
        t.check_equals("-8", new DecimalInteger("-5").plus(new DecimalInteger("-3")).toString(), "-5+(-3)");
        t.check_equals("7", new DecimalInteger("0").plus(new DecimalInteger("7")).toString(), "0+7 shortcut");
        t.check_equals("7", new DecimalInteger("7").plus(new DecimalInteger("0")).toString(), "7+0 shortcut");
        t.check_equals("0", new DecimalInteger("5").plus(new DecimalInteger("-5")).toString(), "5+(-5)=0");

        // minus
        t.check_equals("2", new DecimalInteger("5").minus(new DecimalInteger("3")).toString(), "5-3");
        t.check_equals("-2", new DecimalInteger("3").minus(new DecimalInteger("5")).toString(), "3-5");
        t.check_equals("-2", new DecimalInteger("-5").minus(new DecimalInteger("-3")).toString(), "-5-(-3)");
        t.check_equals("8", new DecimalInteger("5").minus(new DecimalInteger("-3")).toString(), "5-(-3)");
        t.check_equals("-7", new DecimalInteger("0").minus(new DecimalInteger("7")).toString(), "0-7");
        t.check_equals("7", new DecimalInteger("7").minus(new DecimalInteger("0")).toString(), "7-0");

        // multiply
        t.check_equals("144", new DecimalInteger("12").multiply(new DecimalInteger("12")).toString(), "12*12");
        t.check_equals("-12", new DecimalInteger("-3").multiply(new DecimalInteger("4")).toString(), "-3*4");
        t.check_equals("12", new DecimalInteger("-3").multiply(new DecimalInteger("-4")).toString(), "-3*-4");
        t.check_equals("0", new DecimalInteger("0").multiply(new DecimalInteger("999")).toString(), "0*999");
        t.check_equals("999", new DecimalInteger("1").multiply(new DecimalInteger("999")).toString(), "1*999 unit shortcut");
        t.check_equals("-999", new DecimalInteger("-1").multiply(new DecimalInteger("999")).toString(), "-1*999 unit shortcut");
        t.check_equals("-999", new DecimalInteger("999").multiply(new DecimalInteger("-1")).toString(), "999*-1 unit shortcut");

        // multiply_base / divide_by_base
        t.check_equals("1200", new DecimalInteger("12").multiply_base(2).toString(), "multiply_base by 2");
        t.check_equals("12", new DecimalInteger("12").multiply_base(0).toString(), "multiply_base by 0 is identity");
        t.check_equals("-1200", new DecimalInteger("-12").multiply_base(2).toString(), "multiply_base preserves sign");
        t.check_equals("12", new DecimalInteger("1234").divide_by_base(2).toString(), "divide_by_base by 2");
        t.check_equals("1234", new DecimalInteger("1234").divide_by_base(0).toString(), "divide_by_base by 0 is identity");
        t.check_equals("0", new DecimalInteger("1234").divide_by_base(10).toString(), "divide_by_base beyond length is zero");
        t.check_throws(IllegalArgumentException.class, () -> new DecimalInteger("5").multiply_base(-1), "multiply_base rejects negative times");
        t.check_throws(IllegalArgumentException.class, () -> new DecimalInteger("5").divide_by_base(-1), "divide_by_base rejects negative times");

        // divide_by / modulo
        t.check_equals("12", new DecimalInteger("144").divide_by(new DecimalInteger("12")).toString(), "144/12");
        t.check_equals("-12", new DecimalInteger("-144").divide_by(new DecimalInteger("12")).toString(), "-144/12");
        t.check_equals("3", new DecimalInteger("7").divide_by(new DecimalInteger("2")).toString(), "7/2 truncates");
        t.check_equals("-3", new DecimalInteger("-7").divide_by(new DecimalInteger("2")).toString(), "-7/2 truncates toward zero");
        t.check_equals("1", new DecimalInteger("7").modulo(new DecimalInteger("2")).toString(), "7%2");
        t.check_equals("-1", new DecimalInteger("-7").modulo(new DecimalInteger("2")).toString(), "-7%2 keeps dividend sign");
        t.check_throws(ArithmeticException.class, () -> new DecimalInteger("5").divide_by(new DecimalInteger("0")), "divide by zero throws");
        t.check_throws(ArithmeticException.class, () -> new DecimalInteger("5").modulo(new DecimalInteger("0")), "modulo by zero throws");

        verify_division_identity(t, "17", "5");
        verify_division_identity(t, "-17", "5");
        verify_division_identity(t, "17", "-5");
        verify_division_identity(t, "-17", "-5");
        verify_division_identity(t, "100", "7");
        verify_division_identity(t, "0", "5");
        verify_division_identity(t, "999999999999999999999", "37");
        verify_division_identity(t, "1", "1");

        // gcd / lcm
        t.check_equals("6", new DecimalInteger("48").gcd(new DecimalInteger("18")).toString(), "gcd(48,18)");
        t.check_equals("1", new DecimalInteger("17").gcd(new DecimalInteger("5")).toString(), "gcd of coprimes");
        t.check_equals("25", new DecimalInteger("100").gcd(new DecimalInteger("75")).toString(), "gcd(100,75)");
        t.check_equals("5", new DecimalInteger("0").gcd(new DecimalInteger("5")).toString(), "gcd(0,x)=x");
        t.check_equals("5", new DecimalInteger("5").gcd(new DecimalInteger("0")).toString(), "gcd(x,0)=x");
        t.check_equals("6", new DecimalInteger("-12").gcd(new DecimalInteger("18")).toString(), "gcd ignores sign");
        t.check_equals("42", new DecimalInteger("21").lcm(new DecimalInteger("6")).toString(), "lcm(21,6)");
        t.check_equals("0", new DecimalInteger("0").lcm(new DecimalInteger("5")).toString(), "lcm(0,x)=0");

        // pow
        t.check_equals("1024", new DecimalInteger("2").pow(10).toString(), "2^10");
        t.check_equals("1", new DecimalInteger("5").pow(0).toString(), "x^0=1");
        t.check_equals("1", new DecimalInteger("0").pow(0).toString(), "0^0=1 by convention");
        t.check_equals("5", new DecimalInteger("5").pow(1).toString(), "x^1=x");
        t.check_equals("-8", new DecimalInteger("-2").pow(3).toString(), "(-2)^3 is negative");
        t.check_equals("4", new DecimalInteger("-2").pow(2).toString(), "(-2)^2 is positive");
        t.check_throws(IllegalArgumentException.class, () -> new DecimalInteger("2").pow(-1), "negative exponent rejected");

        return t;
    }
}
