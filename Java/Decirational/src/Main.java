import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String integer_str = input.nextLine().trim();
            String b_str = input.nextLine().trim();
            // DecimalInteger integer = new DecimalInteger(Integer.parseInt(integer_str));
            // LargeInteger integer = new LargeInteger(integer_str);
            DecimalInteger integer = new DecimalInteger(integer_str);
            DecimalInteger b = new DecimalInteger(b_str);
            System.out.println(integer);
            System.out.println(b);
            System.out.println(integer.plus(b));
            System.out.println(integer.minus(b));
            System.out.println(integer.multiply(b));
            System.out.println(integer.divide_by(b));
            System.out.println(integer.mod(b));
        }
    }
}