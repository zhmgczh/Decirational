public enum Parenthesis implements Token {
    LEFT_PARENTHESIS('('), RIGHT_PARENTHESIS(')');
    private final char type_code;

    Parenthesis(char type_code) {
        this.type_code = type_code;
    }

    @Override
    public char get_type_code() {
        return type_code;
    }

    @Override
    public String toString() {
        // The literal syntax (e.g. "(", not the enum constant name
        // "LEFT_PARENTHESIS") so error messages read the same as a user's
        // own input.
        return String.valueOf(type_code);
    }
}