public enum Absolute implements Token {
    ABSOLUTE('|');
    private final char type_code;
    Token child;

    Absolute(char type_code) {
        this.type_code = type_code;
    }

    @Override
    public char get_type_code() {
        return type_code;
    }

    @Override
    public String toString() {
        return super.toString() + '(' + type_code + ')';
    }
}