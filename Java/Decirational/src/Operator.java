public enum Operator implements Token {
    PLUS('+'), MINUS('-'), MULTIPLICATION('*'), DIVISION('/'), INTEGER_DIVISION('÷'), MODULO('%'), POWER('^');
    private final char operator_code;

    Operator(char operator_code) {
        this.operator_code = operator_code;
    }

    @Override
    public char get_type_code() {
        return operator_code;
    }

    @Override
    public String toString() {
        // The literal syntax (e.g. "/", not the enum constant name
        // "DIVISION") so error messages read the same as a user's own
        // input; INTEGER_DIVISION's type_code is an internal-only marker
        // (its real two-character syntax "//" can't be a single char), so
        // it is special-cased here rather than shown via type_code.
        return this == INTEGER_DIVISION ? "//" : String.valueOf(operator_code);
    }
}