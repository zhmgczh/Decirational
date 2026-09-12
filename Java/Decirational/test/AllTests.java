package decirational;

public final class AllTests {
    public static void main(final String[] args) {
        final TestFramework[] suites = {
                ArithmeticTest.run(),
                DecimalIntegerTest.run(),
                TightIntegerTest.run(),
                RationalTest.run(),
                OperandTest.run(),
                LexerTest.run(),
                ParserTest.run(),
        };

        int total = 0;
        int passed = 0;
        for (final TestFramework suite : suites) {
            suite.printSummary();
            total += suite.getTotal();
            passed += suite.getPassed();
        }

        System.out.println("TOTAL: " + passed + "/" + total + " passed");
        if (passed != total) {
            System.exit(1);
        }
    }
}
