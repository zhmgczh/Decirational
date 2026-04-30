import java.util.Enumeration;

enum TokenType {
    INTEGER('i'), RATIONAL('r'), PLUS('+'), MINUS('-'), MULTIPLICATION('*'), DIVISION('/'), MODULO('%'), LEFT_PARENTHESIS('('), RIGHT_PARENTHESIS(')');
    private final char type_code;

    TokenType(char type_code) {
        this.type_code = type_code;
    }

    public char get_type_code() {
        return type_code;
    }

    public static int get_priority(TokenType type) {
        return switch (type) {
            case LEFT_PARENTHESIS, RIGHT_PARENTHESIS -> 3;
            case MULTIPLICATION, DIVISION, MODULO -> 2;
            case PLUS, MINUS -> 1;
            case INTEGER, RATIONAL -> 0;
        };
    }
}

interface Token extends Enumeration<Token> {
    public char get_type_code();
}

enum Operator implements Token {
    PLUS('+'), MINUS('-'), MULTIPLICATION('*'), DIVISION('/'), MODULO('%');
    private final char operator_code;
    Enumeration<Token> left;
    Enumeration<Token> right;

    Operator(char operator_code) {
        this.operator_code = operator_code;
    }

    @Override
    public char get_type_code() {
        return operator_code;
    }

    @Override
    public boolean hasMoreElements() {
        return false;
    }

    @Override
    public Token nextElement() {
        return null;
    }
}

enum Operand implements Token {
    INTEGER('i'), RATIONAL('r');
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

    @Override
    public boolean hasMoreElements() {
        return false;
    }

    @Override
    public Token nextElement() {
        return null;
    }
}

public final class Parser<T extends Comparable<T>> {
}