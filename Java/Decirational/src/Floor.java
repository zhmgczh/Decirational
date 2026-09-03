public enum Floor implements Token {
    LEFT_FLOOR('['), RIGHT_FLOOR(']');
    private final char type_code;
    Token child;

    Floor(char type_code) {
        this.type_code = type_code;
    }

    @Override
    public char get_type_code() {
        return type_code;
    }

    @Override
    public String toString() {
        // The literal syntax (e.g. "[", not the enum constant name
        // "LEFT_FLOOR") so error messages read the same as a user's own
        // input.
        return String.valueOf(type_code);
    }
}
