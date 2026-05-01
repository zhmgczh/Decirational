public final class Operand implements Token {
    private final OperandType operand_type;
    private final Object value;

    <T extends CustomInteger<T>> Operand(String value, final Class<T> large_integer_type, final Class<Rational<T>> rational_type) {
        Integer integer;
        try {
            integer = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            integer = null;
        }
        if (null != integer) {
            this.value = integer;
            this.operand_type = OperandType.INTEGER;
            return;
        }
        T large_integer;
        try {
            large_integer = large_integer_type.getConstructor(String.class).newInstance(value);
        } catch (Exception e) {
            large_integer = null;
        }
        if (null != large_integer) {
            this.value = large_integer;
            this.operand_type = OperandType.LARGE_INTEGER;
            return;
        }
        Rational<T> rational;
        try {
            rational = rational_type.getConstructor(String.class, Class.class).newInstance(value, large_integer_type);
        } catch (Exception e) {
            throw new IllegalArgumentException(value + " is not a valid rational.");
        }
        this.value = rational;
        this.operand_type = OperandType.RATIONAL;
    }

    @Override
    public String toString() {
        return operand_type.toString() + '(' + value + ')';
    }

    @Override
    public char get_type_code() {
        return operand_type.get_type_code();
    }

    public Object get_value() {
        return value;
    }
}