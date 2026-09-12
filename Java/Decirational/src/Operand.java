package decirational;

import java.util.function.Function;

public final class Operand implements Token {
    private final OperandType operand_type;
    private final Object value;

    <T extends CustomInteger<T>> Operand(String value, final Function<String, T> from_string) {
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
            large_integer = from_string.apply(value);
        } catch (NumberFormatException e) {
            large_integer = null;
        }
        if (null != large_integer) {
            this.value = large_integer;
            this.operand_type = OperandType.LARGE_INTEGER;
            return;
        }
        final Rational<T> rational;
        try {
            rational = new Rational<>(value, from_string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(value + " is not a valid rational", e);
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

    public OperandType get_operand_type() {
        return operand_type;
    }

    public Object get_value() {
        return value;
    }
}
