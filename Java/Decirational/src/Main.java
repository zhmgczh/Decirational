void main() {
    Scanner input = new Scanner(System.in);
    while (input.hasNextLine()) {
        String integer_str = input.nextLine().trim();
        String b_str = input.nextLine().trim();
        // DecimalInteger integer = new DecimalInteger(Integer.parseInt(integer_str));
        // LargeInteger integer = new LargeInteger(integer_str);
        DecimalInteger integer = new DecimalInteger(integer_str);
        DecimalInteger b = new DecimalInteger(b_str);
        IO.println(integer.plus(b));
        IO.println(integer.minus(b));
    }
}