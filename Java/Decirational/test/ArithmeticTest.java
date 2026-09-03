import java.util.Arrays;

public final class ArithmeticTest {
    public static TestFramework run() {
        final TestFramework t = new TestFramework("Arithmetic");

        t.check(Arrays.equals(new byte[]{1, 2}, Arithmetic.optimize(new byte[]{0, 0, 1, 2})), "optimize(byte[]) strips leading zeros");
        t.check(Arrays.equals(new byte[]{0}, Arithmetic.optimize(new byte[]{0, 0, 0})), "optimize(byte[]) of all zeros keeps a single zero");
        t.check(Arrays.equals(new byte[]{5}, Arithmetic.optimize(new byte[]{5})), "optimize(byte[]) leaves a single nonzero digit alone");

        t.check(Arrays.equals(new int[]{1, 2}, Arithmetic.optimize(new int[]{0, 0, 1, 2})), "optimize(int[]) strips leading zeros");
        t.check(Arrays.equals(new int[]{0}, Arithmetic.optimize(new int[]{0, 0, 0})), "optimize(int[]) of all zeros keeps a single zero");

        t.check(!Arithmetic.optimize(true, new byte[]{0}), "optimize(negative,byte[]) forces sign false for zero magnitude");
        t.check(Arithmetic.optimize(true, new byte[]{5}), "optimize(negative,byte[]) preserves sign for nonzero magnitude");
        t.check(!Arithmetic.optimize(true, new int[]{0}), "optimize(negative,int[]) forces sign false for zero magnitude");

        t.check(Arrays.equals(new byte[]{0, 0, 1, 2}, Arithmetic.expand(new byte[]{1, 2}, 4)), "expand(byte[]) left-pads with zeros");
        t.check(Arrays.equals(new byte[]{1, 2}, Arithmetic.expand(new byte[]{1, 2}, 2)), "expand(byte[]) is a no-op when length already matches");
        t.check(Arrays.equals(new byte[]{1, 2}, Arithmetic.expand(new byte[]{1, 2}, 1)), "expand(byte[]) is a no-op when requested length is smaller");

        t.check(0 == Arithmetic.compare(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}), "compare(byte[]) equal arrays");
        t.check(Arithmetic.compare(new byte[]{1, 2, 3}, new byte[]{1, 2, 4}) < 0, "compare(byte[]) less-than on last digit");
        t.check(Arithmetic.compare(new byte[]{2}, new byte[]{1, 9}) < 0, "compare(byte[]) shorter true magnitude beats longer");
        t.check(0 == Arithmetic.compare(new byte[]{0, 0, 5}, new byte[]{5}), "compare(byte[]) ignores leading zero padding");
        t.check(Arithmetic.compare(new byte[]{9}, new byte[]{1}) > 0, "compare(byte[]) greater-than");

        t.check(0 == Arithmetic.compare(new int[]{1, 2}, new int[]{1, 2}), "compare(int[]) equal arrays");
        t.check(Arithmetic.compare(new int[]{1}, new int[]{2}) < 0, "compare(int[]) less-than");
        t.check(0 == Arithmetic.compare(new int[]{0, 7}, new int[]{7}), "compare(int[]) ignores leading zero padding");
        t.check(Arithmetic.compare(new int[]{-1}, new int[]{1}) > 0, "compare(int[]) treats ints as unsigned magnitude");

        return t;
    }
}
