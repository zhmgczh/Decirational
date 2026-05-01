import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Scanner;

interface Token {
    char get_type_code();

    String toString();
}

enum Parenthesis implements Token {
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
        return super.toString();
    }
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

enum OperandType implements Token {
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

final class Operand implements Token {
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

final class Lexer<T extends CustomInteger<T>> {
    private final Class<T> large_integer_type;
    private final Class<Rational<T>> rational_type;
    private static final HashMap<Character, LinkedHashSet<Token>> char_table = new HashMap<>();

    static {
        LinkedHashSet<Token> number_set = new LinkedHashSet<>();
        number_set.add(OperandType.INTEGER);
        number_set.add(OperandType.LARGE_INTEGER);
        number_set.add(OperandType.RATIONAL);
        for (char c = '0'; c <= '9'; ++c) {
            char_table.put(c, number_set);
        }
        LinkedHashSet<Token> rational_set = new LinkedHashSet<>();
        rational_set.add(OperandType.RATIONAL);
        char_table.put('.', rational_set);
        char_table.put(Arithmetic.cyclic_begin, rational_set);
        char_table.put(Arithmetic.cyclic_end, rational_set);
        LinkedHashSet<Token> left_parenthesis_set = new LinkedHashSet<>();
        left_parenthesis_set.add(Parenthesis.LEFT_PARENTHESIS);
        char_table.put('(', left_parenthesis_set);
        LinkedHashSet<Token> right_parenthesis_set = new LinkedHashSet<>();
        right_parenthesis_set.add(Parenthesis.RIGHT_PARENTHESIS);
        char_table.put(')', right_parenthesis_set);
        LinkedHashSet<Token> plus_set = new LinkedHashSet<>();
        plus_set.add(Operator.PLUS);
        char_table.put('+', plus_set);
        LinkedHashSet<Token> minus_set = new LinkedHashSet<>();
        minus_set.add(Operator.MINUS);
        char_table.put('-', minus_set);
        LinkedHashSet<Token> multiplication_set = new LinkedHashSet<>();
        multiplication_set.add(Operator.MULTIPLICATION);
        char_table.put('*', multiplication_set);
        LinkedHashSet<Token> division_set = new LinkedHashSet<>();
        division_set.add(Operator.DIVISION);
        char_table.put('/', division_set);
        LinkedHashSet<Token> modulo_set = new LinkedHashSet<>();
        modulo_set.add(Operator.MODULO);
        char_table.put('%', modulo_set);
        LinkedHashSet<Token> power_set = new LinkedHashSet<>();
        power_set.add(Operator.POWER);
        char_table.put('^', power_set);
    }

    public Lexer(final Class<T> large_integer_type, final Class<Rational<T>> rational_type) {
        this.large_integer_type = large_integer_type;
        this.rational_type = rational_type;
    }

    private void record_token(String expression, LinkedHashSet<Token> set, int left_index, int right_index, ArrayList<Token> tokens) {
        final Token token = set.getFirst();
        if (token.getClass() == OperandType.class) {
            final String value = expression.substring(left_index, right_index);
            final Operand operand = new Operand(value, large_integer_type, rational_type);
            tokens.add(operand);
        } else {
            for (int i = left_index; i < right_index; ++i) {
                tokens.add(token);
            }
        }
    }

    public ArrayList<Token> get_tokens(String expression) {
        if (null == expression) {
            return new ArrayList<>();
        }
        expression = expression.replaceAll("\\s", "");
        if (expression.isEmpty()) {
            return new ArrayList<>();
        }
        final ArrayList<Token> tokens = new ArrayList<>();
        LinkedHashSet<Token> set = char_table.get(expression.charAt(0));
        int left_index = 0;
        int right_index = 1;
        while (right_index < expression.length()) {
            final char c = expression.charAt(right_index);
            final LinkedHashSet<Token> current_set = char_table.get(c);
            if (null == current_set) {
                throw new IllegalArgumentException("Illegal character " + c + " in expression: " + expression);
            }
            final LinkedHashSet<Token> next_set = new LinkedHashSet<>(set);
            next_set.retainAll(current_set);
            if (next_set.isEmpty()) {
                record_token(expression, set, left_index, right_index, tokens);
                set = current_set;
                left_index = right_index;
            } else {
                set = next_set;
            }
            ++right_index;
        }
        record_token(expression, set, left_index, right_index, tokens);
        return tokens;
    }

    public static void main(final String[] args) {
        final Scanner input = new Scanner(System.in);
        @SuppressWarnings("unchecked") final Lexer<DecimalInteger> lexer = new Lexer<>(DecimalInteger.class, (Class<Rational<DecimalInteger>>) (Class<?>) Rational.class);
        while (input.hasNextLine()) {
            final String expression = input.nextLine();
            System.out.println(lexer.get_tokens(expression));
        }
    }
}

public final class Parser<T extends Comparable<T>> {
}