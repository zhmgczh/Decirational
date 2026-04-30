enum TokenType {
    INTEGER('i'), LARGE_INTEGER('l'), RATIONAL('r'), PLUS('+'), MINUS('-'), MULTIPLICATION('*'), DIVISION('/'), MODULO('%'), POWER('^'), LEFT_PARENTHESIS('('), RIGHT_PARENTHESIS(')');
    private final char type_code;

    TokenType(char type_code) {
        this.type_code = type_code;
    }

    public char get_type_code() {
        return type_code;
    }

    public static int get_priority(TokenType type) {
        return switch (type) {
            case LEFT_PARENTHESIS, RIGHT_PARENTHESIS -> 4;
            case POWER -> 3;
            case MULTIPLICATION, DIVISION, MODULO -> 2;
            case PLUS, MINUS -> 1;
            case INTEGER, LARGE_INTEGER, RATIONAL -> 0;
        };
    }
}

interface Token {
    public char get_type_code();
}

enum Operator implements Token {
    PLUS('+'), MINUS('-'), MULTIPLICATION('*'), DIVISION('/'), MODULO('%'), POWER('^');
    private final char operator_code;
    Token left;
    Token right;

    Operator(char operator_code) {
        this.operator_code = operator_code;
    }

    @Override
    public char get_type_code() {
        return operator_code;
    }
}

enum Operand implements Token {
    INTEGER('i'), LARGE_INTEGER('l'), RATIONAL('r');
    private final char operand_code;
    Object value;

    Operand(char operand_code) {
        this.operand_code = operand_code;
    }

    @Override
    public char get_type_code() {
        return operand_code;
    }

    public Object get_value() {
        return value;
    }
}

public final class Parser<T extends Comparable<T>> {
}