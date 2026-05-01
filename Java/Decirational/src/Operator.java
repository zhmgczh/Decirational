public enum Operator implements Token {
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

    @Override
    public String toString() {
        return super.toString() + '(' + operator_code + ')';
    }

    public static int get_priority(Operator operator) {
        return switch (operator) {
            case PLUS, MINUS -> 0;
            case MULTIPLICATION, DIVISION, MODULO -> 1;
            case POWER -> 2;
        };
    }
}