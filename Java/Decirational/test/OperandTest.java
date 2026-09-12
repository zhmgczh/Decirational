package decirational;

public final class OperandTest {
    public static TestFramework run() {
        final TestFramework t = new TestFramework("Operand");

        Operand operand = new Operand("123", TightInteger::new);
        t.check(operand.getValue() instanceof Integer, "small literal stores a plain Integer");
        t.checkEquals(123, operand.getValue(), "small literal value is correct");
        t.checkEquals('i', operand.getTypeCode(), "small literal type code is 'i'");

        operand = new Operand("99999999999999999999", TightInteger::new);
        t.check(operand.getValue() instanceof TightInteger, "large literal stores the configured large-integer type");
        t.checkEquals('l', operand.getTypeCode(), "large literal type code is 'l'");
        t.checkEquals(new TightInteger("99999999999999999999"), operand.getValue(), "large literal value is correct");

        operand = new Operand("1.5", TightInteger::new);
        t.check(operand.getValue() instanceof Rational, "decimal literal stores a Rational");
        t.checkEquals('r', operand.getTypeCode(), "decimal literal type code is 'r'");

        operand = new Operand("-5", TightInteger::new);
        t.check(operand.getValue() instanceof Integer, "a signed literal parses directly as an Integer");
        t.checkEquals(-5, operand.getValue(), "signed literal value is correct");

        t.checkThrows(IllegalArgumentException.class, () -> new Operand("1.2.3", TightInteger::new), "malformed literal is rejected");

        return t;
    }
}
