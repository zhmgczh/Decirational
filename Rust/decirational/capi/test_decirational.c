/* A real C program exercising the raw C API end to end - not just "does it
 * link", but "does every function produce the right answer, report errors
 * correctly, and not leak/corrupt memory". Build/run via ../run_capi_test.sh. */
#include "decirational.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>

static int failures = 0;

static void check_str(const char *label, const char *got, const char *want) {
    if (got == NULL) {
        printf("FAIL %s: got NULL, error=%s\n", label, decirational_last_error());
        failures++;
        return;
    }
    if (strcmp(got, want) != 0) {
        printf("FAIL %s: got %s, want %s\n", label, got, want);
        failures++;
    } else {
        printf("ok   %s = %s\n", label, got);
    }
}

static void check_null(const char *label, const void *got) {
    if (got != NULL) {
        printf("FAIL %s: expected NULL (an error), got a value\n", label);
        failures++;
    } else {
        printf("ok   %s correctly failed: %s\n", label, decirational_last_error());
    }
}

static void check_int(const char *label, int32_t got, int32_t want) {
    if (got != want) {
        printf("FAIL %s: got %d, want %d\n", label, got, want);
        failures++;
    } else {
        printf("ok   %s = %d\n", label, got);
    }
}

int main(void) {
    printf("decirational C API version: %s\n\n", decirational_version());

    /* ---- decirational_eval: the whole CLI as one call ---- */
    char *r = decirational_eval("100/7", DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DECIMAL, 0);
    check_str("eval(100/7, decimal)", r, "14.{285714}");
    decirational_string_free(r);

    r = decirational_eval("2^10 + |3-10| * [7.5]", DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DECIMAL, 0);
    check_str("eval(combined expression)", r, "1073");
    decirational_string_free(r);

    r = decirational_eval("100/7", DECIRATIONAL_BACKEND_TIGHT, DECIRATIONAL_FORMAT_DECIMAL, 0);
    check_str("eval(100/7, tight)", r, "14.{285714}");
    decirational_string_free(r);

    r = decirational_eval("100//7", DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_str("eval(100//7)", r, "14");
    decirational_string_free(r);

    r = decirational_eval("1/0", DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_null("eval(1/0) is an error", r);

    r = decirational_eval("2#3", DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_null("eval(illegal char) is an error", r);

    r = decirational_eval(NULL, DECIRATIONAL_BACKEND_DECIMAL, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_null("eval(NULL) is an error, not a crash", r);
    printf("\n");

    /* ---- construction, arithmetic, formatting on handles ---- */
    DecirationalRational *a = decirational_rational_parse("1/3", DECIRATIONAL_BACKEND_DECIMAL);
    DecirationalRational *b = decirational_rational_parse("1/6", DECIRATIONAL_BACKEND_DECIMAL);
    assert(a && b);

    DecirationalRational *sum = decirational_rational_add(a, b);
    r = decirational_rational_to_string(sum, DECIRATIONAL_FORMAT_DECIMAL, 0);
    check_str("1/3 + 1/6 (decimal)", r, "0.5");
    decirational_string_free(r);
    r = decirational_rational_to_string(sum, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_str("1/3 + 1/6 (default/fraction)", r, "1/2");
    decirational_string_free(r);
    decirational_rational_free(sum);

    DecirationalRational *seven_fourths = decirational_rational_parse("7/4", DECIRATIONAL_BACKEND_DECIMAL);
    r = decirational_rational_to_string(seven_fourths, DECIRATIONAL_FORMAT_MIXED, 0);
    check_str("7/4 mixed", r, "1 3/4");
    decirational_string_free(r);

    r = decirational_rational_numerator_string(seven_fourths);
    check_str("7/4 numerator", r, "7");
    decirational_string_free(r);
    r = decirational_rational_denominator_string(seven_fourths);
    check_str("7/4 denominator", r, "4");
    decirational_string_free(r);
    decirational_rational_free(seven_fourths);

    DecirationalRational *neg_one_third = decirational_rational_parse("-1/3", DECIRATIONAL_BACKEND_DECIMAL);
    r = decirational_rational_to_string(neg_one_third, DECIRATIONAL_FORMAT_DECIMAL, 0);
    check_str("-1/3 decimal (repeating)", r, "-0.{3}");
    decirational_string_free(r);
    check_int("-1/3 is_negative", decirational_rational_is_negative(neg_one_third), 1);
    check_int("-1/3 is_zero", decirational_rational_is_zero(neg_one_third), 0);
    check_int("-1/3 is_integer", decirational_rational_is_integer(neg_one_third), 0);
    decirational_rational_free(neg_one_third);

    /* divide by zero -> NULL + error, not a crash */
    DecirationalRational *zero = decirational_rational_from_i64(0, DECIRATIONAL_BACKEND_DECIMAL);
    DecirationalRational *div_by_zero = decirational_rational_div(a, zero);
    check_null("1/3 / 0 is an error", div_by_zero);
    DecirationalRational *recip_of_zero = decirational_rational_reciprocal(zero);
    check_null("reciprocal(0) is an error", recip_of_zero);
    decirational_rational_free(zero);

    /* pow, negate, abs, compare */
    DecirationalRational *two = decirational_rational_from_i64(2, DECIRATIONAL_BACKEND_DECIMAL);
    DecirationalRational *two_pow_neg2 = decirational_rational_pow(two, -2);
    r = decirational_rational_to_string(two_pow_neg2, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_str("2^-2", r, "1/4");
    decirational_string_free(r);
    decirational_rational_free(two_pow_neg2);
    decirational_rational_free(two);

    DecirationalRational *neg_a = decirational_rational_negate(a);
    r = decirational_rational_to_string(neg_a, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_str("-(1/3)", r, "-1/3");
    decirational_string_free(r);
    DecirationalRational *abs_neg_a = decirational_rational_abs(neg_a);
    r = decirational_rational_to_string(abs_neg_a, DECIRATIONAL_FORMAT_DEFAULT, 0);
    check_str("|-(1/3)|", r, "1/3");
    decirational_string_free(r);
    decirational_rational_free(abs_neg_a);
    decirational_rational_free(neg_a);

    check_int("1/3 vs 1/6", decirational_rational_compare(a, b), 1);
    check_int("1/6 vs 1/3", decirational_rational_compare(b, a), -1);
    DecirationalRational *a_clone = decirational_rational_clone(a);
    check_int("1/3 vs clone(1/3)", decirational_rational_compare(a, a_clone), 0);
    decirational_rational_free(a_clone);

    /* mismatched backends: a clear error, not UB */
    DecirationalRational *tight_one = decirational_rational_from_i64(1, DECIRATIONAL_BACKEND_TIGHT);
    DecirationalRational *mixed = decirational_rational_add(a, tight_one);
    check_null("decimal + tight is rejected", mixed);
    check_int("mismatched-backend compare returns INT32_MIN sentinel", decirational_rational_compare(a, tight_one), INT32_MIN);
    decirational_rational_free(tight_one);

    /* round_to == INT32_MIN is a documented panic in Rust; must surface as
     * NULL + error here, not abort the process. */
    DecirationalRational *five = decirational_rational_from_i64(5, DECIRATIONAL_BACKEND_DECIMAL);
    r = decirational_rational_to_string(five, DECIRATIONAL_FORMAT_ROUND, INT32_MIN);
    check_null("round at INT32_MIN precision (a Rust panic) is caught, not UB", r);
    decirational_rational_free(five);

    decirational_rational_free(a);
    decirational_rational_free(b);

    printf("\n%d failure(s)\n", failures);
    return failures == 0 ? 0 : 1;
}
