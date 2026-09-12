package decirational;

public final class TestFramework {
    private final String suite_name;
    private int total = 0;
    private int passed = 0;
    private final StringBuilder failures = new StringBuilder();

    public TestFramework(final String suite_name) {
        this.suite_name = suite_name;
    }

    public void check(final boolean condition, final String description) {
        ++total;
        if (condition) {
            ++passed;
        } else {
            failures.append("  FAIL: ").append(description).append('\n');
        }
    }

    public void checkEquals(final Object expected, final Object actual, final String description) {
        final boolean ok = null == expected ? null == actual : expected.equals(actual);
        check(ok, description + " (expected <" + expected + "> but was <" + actual + ">)");
    }

    public void checkThrows(final Class<? extends Throwable> expected_type, final Runnable action, final String description) {
        try {
            action.run();
            check(false, description + " (expected " + expected_type.getSimpleName() + " to be thrown, but nothing was thrown)");
        } catch (final Throwable t) {
            check(expected_type.isInstance(t), description + " (expected " + expected_type.getSimpleName() + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
        }
    }

    public int getTotal() {
        return total;
    }

    public int getPassed() {
        return passed;
    }

    public void printSummary() {
        System.out.println("[" + suite_name + "] " + passed + "/" + total + " passed");
        if (passed != total) {
            System.out.print(failures);
        }
    }
}
