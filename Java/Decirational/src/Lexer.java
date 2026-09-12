import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.function.Function;

public final class Lexer<T extends CustomInteger<T>> {
    private final Function<String, T> from_string;
    private static final HashMap<Character, LinkedHashSet<Token>> char_table = new HashMap<>();
    private static final Token[] normal_tokens = {Parenthesis.LEFT_PARENTHESIS, Parenthesis.RIGHT_PARENTHESIS, Floor.LEFT_FLOOR, Floor.RIGHT_FLOOR, Absolute.ABSOLUTE, Operator.PLUS, Operator.MINUS, Operator.MULTIPLICATION, Operator.DIVISION, Operator.MODULO, Operator.POWER};

    private static void register_token(Token token) {
        LinkedHashSet<Token> token_set = new LinkedHashSet<>();
        token_set.add(token);
        char_table.put(token.get_type_code(), token_set);
    }

    static {
        for (Token token : normal_tokens) {
            register_token(token);
        }
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
    }

    public Lexer(final Function<String, T> from_string) {
        this.from_string = from_string;
    }

    private void record_token(String expression, LinkedHashSet<Token> set, int left_index, int right_index, ArrayList<Token> tokens) {
        final Token token = set.getFirst();
        if (token.getClass() == OperandType.class) {
            final String value = expression.substring(left_index, right_index);
            final Operand operand = new Operand(value, from_string);
            tokens.add(operand);
        } else if (token == Operator.DIVISION) {
            // Pair up consecutive '/' characters into "//" (integer division)
            // tokens, left to right, leaving a lone trailing '/' (if the run
            // is an odd length) as ordinary (exact) division.
            int remaining = right_index - left_index;
            while (remaining >= 2) {
                tokens.add(Operator.INTEGER_DIVISION);
                remaining -= 2;
            }
            if (1 == remaining) {
                tokens.add(Operator.DIVISION);
            }
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
        if (null == set) {
            throw new IllegalArgumentException("illegal character " + expression.charAt(0) + " in expression: " + expression);
        }
        int left_index = 0;
        int right_index = 1;
        while (right_index < expression.length()) {
            final char c = expression.charAt(right_index);
            final LinkedHashSet<Token> current_set = char_table.get(c);
            if (null == current_set) {
                throw new IllegalArgumentException("illegal character " + c + " in expression: " + expression);
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
        final Lexer<TightInteger> lexer = new Lexer<>(TightInteger::new);
        while (input.hasNextLine()) {
            final String expression = input.nextLine();
            System.out.println(lexer.get_tokens(expression));
        }
    }
}