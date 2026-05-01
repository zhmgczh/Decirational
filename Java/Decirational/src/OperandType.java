public enum OperandType implements Token {
    INTEGER('i'), LARGE_INTEGER('l'), RATIONAL('r');
    private final char operand_code;

    OperandType(char operand_code) {
        this.operand_code = operand_code;
    }

    public char getOperand_code() {
        return operand_code;
    }

    @Override
    public char get_type_code() {
        return operand_code;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}