public final class OperandTest {
    @SuppressWarnings("unchecked")
    public static TestFramework run() {
        final TestFramework t = new TestFramework("Operand");
        final Class<Rational<TightInteger>> rational_type = (Class<Rational<TightInteger>>) (Class<?>) Rational.class;

        Operand operand = new Operand("123", TightInteger.class, rational_type);
        t.check(operand.get_value() instanceof Integer, "small literal stores a plain Integer");
        t.check_equals(123, operand.get_value(), "small literal value is correct");
        t.check_equals('i', operand.get_type_code(), "small literal type code is 'i'");

        operand = new Operand("99999999999999999999", TightInteger.class, rational_type);
        t.check(operand.get_value() instanceof TightInteger, "large literal stores the configured large-integer type");
        t.check_equals('l', operand.get_type_code(), "large literal type code is 'l'");
        t.check_equals(new TightInteger("99999999999999999999"), operand.get_value(), "large literal value is correct");

        operand = new Operand("1.5", TightInteger.class, rational_type);
        t.check(operand.get_value() instanceof Rational, "decimal literal stores a Rational");
        t.check_equals('r', operand.get_type_code(), "decimal literal type code is 'r'");

        operand = new Operand("-5", TightInteger.class, rational_type);
        t.check(operand.get_value() instanceof Integer, "a signed literal parses directly as an Integer");
        t.check_equals(-5, operand.get_value(), "signed literal value is correct");

        t.check_throws(IllegalArgumentException.class, () -> new Operand("1.2.3", TightInteger.class, rational_type), "malformed literal is rejected");

        return t;
    }
}
