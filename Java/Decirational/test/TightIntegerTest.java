package decirational;

public final class TightIntegerTest {
    private static void verify_division_identity(final TestFramework t, final String a_str, final String b_str) {
        final TightInteger a = new TightInteger(a_str);
        final TightInteger b = new TightInteger(b_str);
        final TightInteger[] qr = a.divide_by_and_modulo(b);
        final TightInteger reconstructed = qr[0].multiply(b).plus(qr[1]);
        t.check_equals(a, reconstructed, "divide_by_and_modulo(" + a_str + "," + b_str + ") satisfies q*b+r==a");
        t.check_equals(qr[0], a.divide_by(b), "divide_by_and_modulo(" + a_str + "," + b_str + ") quotient matches divide_by");
        t.check_equals(qr[1], a.modulo(b), "divide_by_and_modulo(" + a_str + "," + b_str + ") remainder matches modulo");
    }

    private static void verify_round_trip(final TestFramework t, final String value) {
        t.check_equals(value, new TightInteger(value).toString(), "TightInteger round-trip for " + value);
        t.check_equals(new DecimalInteger(value), new TightInteger(value).to_decimal_integer(), "to_decimal_integer matches for " + value);
        t.check_equals(value, new DecimalInteger(value).to_tight_integer().to_decimal_integer().toString(), "DecimalInteger->TightInteger->DecimalInteger round-trip for " + value);
        t.check_equals(value, new TightInteger(new DecimalInteger(value)).toString(), "TightInteger(DecimalInteger) constructor for " + value);
    }

    private static void cross_check(final TestFramework t, final String a_str, final String b_str) {
        final DecimalInteger da = new DecimalInteger(a_str);
        final DecimalInteger db = new DecimalInteger(b_str);
        final TightInteger ta = new TightInteger(a_str);
        final TightInteger tb = new TightInteger(b_str);
        t.check_equals(da.plus(db).toString(), ta.plus(tb).toString(), "plus cross-check " + a_str + "+" + b_str);
        t.check_equals(da.minus(db).toString(), ta.minus(tb).toString(), "minus cross-check " + a_str + "-" + b_str);
        t.check_equals(da.multiply(db).toString(), ta.multiply(tb).toString(), "multiply cross-check " + a_str + "*" + b_str);
        if (!db.is_zero()) {
            t.check_equals(da.divide_by(db).toString(), ta.divide_by(tb).toString(), "divide_by cross-check " + a_str + "/" + b_str);
            t.check_equals(da.modulo(db).toString(), ta.modulo(tb).toString(), "modulo cross-check " + a_str + "%" + b_str);
        }
        t.check_equals(da.gcd(db).toString(), ta.gcd(tb).toString(), "gcd cross-check " + a_str + "," + b_str);
        if (!da.is_zero() && !db.is_zero()) {
            t.check_equals(da.lcm(db).toString(), ta.lcm(tb).toString(), "lcm cross-check " + a_str + "," + b_str);
        }
        t.check_equals(da.pow(3).toString(), ta.pow(3).toString(), "pow(3) cross-check " + a_str);
    }

    public static TestFramework run() {
        final TestFramework t = new TestFramework("TightInteger");

        // Construction / toString (mirrors DecimalInteger contract)
        t.check_equals("123", new TightInteger("123").toString(), "parse plain digits");
        t.check_equals("-123", new TightInteger("-123").toString(), "parse negative digits");
        t.check_equals("0", new TightInteger("0").toString(), "zero stays zero");
        t.check_equals("0", new TightInteger("-0").toString(), "negative zero normalizes to zero");
        t.check_equals("0", new TightInteger(0).toString(), "int constructor zero");
        t.check_equals("42", new TightInteger(42).toString(), "int constructor positive");
        t.check_equals("-42", new TightInteger(-42).toString(), "int constructor negative");
        t.check_equals(TightInteger.ZERO, new TightInteger("0"), "ZERO constant equals parsed zero");
        t.check_equals(TightInteger.ONE, new TightInteger("1"), "ONE constant equals parsed one");
        t.check_equals(TightInteger.get_digit(1), new TightInteger("1"), "get_digit(1) matches parsed 1");
        t.check_throws(IllegalArgumentException.class, () -> TightInteger.get_digit(2), "get_digit out of range rejected");

        // Predicates
        t.check(new TightInteger("0").is_zero(), "0 is zero");
        t.check(new TightInteger("1").is_one(), "1 is one");
        t.check(!new TightInteger("-1").is_one(), "-1 is not one");
        t.check(new TightInteger("-1").is_unit_abs(), "-1 has unit abs");
        t.check(new TightInteger("5").is_positive(), "5 is positive");
        t.check(new TightInteger("-5").is_negative(), "-5 is negative");

        // negate / abs
        t.check_equals("-5", new TightInteger("5").negate().toString(), "negate positive");
        t.check_equals("5", new TightInteger("-5").abs().toString(), "abs of negative");
        t.check_equals("0", new TightInteger("0").negate().toString(), "negate zero stays zero");

        // compareTo
        t.check(new TightInteger("5").compareTo(new TightInteger("3")) > 0, "5 > 3");
        t.check(new TightInteger("-5").compareTo(new TightInteger("-3")) < 0, "-5 < -3");
        t.check(0 == new TightInteger("5").compareTo(new TightInteger("5")), "5 == 5");

        // equals / hashCode
        t.check_equals(new TightInteger("007"), new TightInteger(7), "equal values from different literals are equal");
        t.check(new TightInteger("007").hashCode() == new TightInteger(7).hashCode(), "equal values have equal hashCodes");
        t.check(!new TightInteger("5").equals(new TightInteger("-5")), "5 is not equal to -5");
        t.check(!new TightInteger("5").equals(null), "not equal to null");

        // plus / minus / multiply basics
        t.check_equals("579", new TightInteger("123").plus(new TightInteger("456")).toString(), "123+456");
        t.check_equals("2", new TightInteger("5").minus(new TightInteger("3")).toString(), "5-3");
        t.check_equals("144", new TightInteger("12").multiply(new TightInteger("12")).toString(), "12*12");
        t.check_equals("-999", new TightInteger("-1").multiply(new TightInteger("999")).toString(), "-1*999 unit shortcut");

        // multiply_base / divide_by_base (base here is 2^32, verify via round trip rather than literal digits)
        t.check_equals(new TightInteger("12"), new TightInteger("12").multiply_base(2).divide_by_base(2), "multiply_base then divide_by_base is identity");
        t.check_equals("12", new TightInteger("12").multiply_base(0).toString(), "multiply_base by 0 is identity");
        t.check_throws(IllegalArgumentException.class, () -> new TightInteger("5").multiply_base(-1), "multiply_base rejects negative times");
        t.check_throws(IllegalArgumentException.class, () -> new TightInteger("5").divide_by_base(-1), "divide_by_base rejects negative times");

        // divide_by / modulo
        t.check_equals("12", new TightInteger("144").divide_by(new TightInteger("12")).toString(), "144/12");
        t.check_equals("-3", new TightInteger("-7").divide_by(new TightInteger("2")).toString(), "-7/2 truncates toward zero");
        t.check_equals("-1", new TightInteger("-7").modulo(new TightInteger("2")).toString(), "-7%2 keeps dividend sign");
        t.check_throws(ArithmeticException.class, () -> new TightInteger("5").divide_by(new TightInteger("0")), "divide by zero throws");
        t.check_throws(ArithmeticException.class, () -> new TightInteger("5").modulo(new TightInteger("0")), "modulo by zero throws");

        verify_division_identity(t, "17", "5");
        verify_division_identity(t, "-17", "5");
        verify_division_identity(t, "123456789012345678901234567890", "987654321");
        verify_division_identity(t, "0", "5");

        // gcd / lcm
        t.check_equals("6", new TightInteger("48").gcd(new TightInteger("18")).toString(), "gcd(48,18)");
        t.check_equals("5", new TightInteger("0").gcd(new TightInteger("-5")).toString(), "gcd(0,-x)=x");
        t.check_equals("5", new TightInteger("-5").gcd(new TightInteger("0")).toString(), "gcd(-x,0)=x");
        t.check_equals("42", new TightInteger("21").lcm(new TightInteger("6")).toString(), "lcm(21,6)");

        // pow
        t.check_equals("1024", new TightInteger("2").pow(10).toString(), "2^10");
        t.check_equals("1", new TightInteger("5").pow(0).toString(), "x^0=1");
        t.check_throws(IllegalArgumentException.class, () -> new TightInteger("2").pow(-1), "negative exponent rejected");

        // Round trips between TightInteger's base-2^32 representation and DecimalInteger's base-10 one
        verify_round_trip(t, "0");
        verify_round_trip(t, "1");
        verify_round_trip(t, "-1");
        verify_round_trip(t, "9");
        verify_round_trip(t, "10");
        verify_round_trip(t, "99");
        verify_round_trip(t, "100");
        verify_round_trip(t, "2147483647");
        verify_round_trip(t, "2147483648");
        verify_round_trip(t, "4294967295");
        verify_round_trip(t, "4294967296");
        verify_round_trip(t, "-2147483648");
        verify_round_trip(t, "123456789012345678901234567890");
        verify_round_trip(t, "-999999999999999999999999999999999999999");

        // Cross-validate arithmetic against the independently-implemented DecimalInteger
        cross_check(t, "123456789012345678901234567890", "987654321098765432109876543210");
        cross_check(t, "1000000000000000000000000000000", "3");
        cross_check(t, "-123456789", "456");
        cross_check(t, "0", "999999999999999999999");
        cross_check(t, "7", "7");
        cross_check(t, "-999999999999999999999999999999999999999", "-1");
        cross_check(t, "4294967296", "4294967295");

        return t;
    }
}
