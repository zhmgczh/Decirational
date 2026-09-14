/*
 * decirational.h - raw C ABI for the Rust Decirational implementation.
 *
 * This header describes the `extern "C"` functions implemented in
 * src/capi.rs, exported by libdecirational.so / .dylib / .dll (cdylib) and
 * libdecirational.a (staticlib), built alongside the normal Rust library and
 * the `decirational` binary from the same crate (see ../Cargo.toml's
 * `crate-type`). Hand-written to match src/capi.rs exactly - there is no
 * dependency (this project has none) generating it, so if you change one,
 * change the other.
 *
 * ---- Conventions ----
 *
 *   - Every function that can fail returns a null pointer, or a negative
 *     sentinel (-1 for a normally-0/1 boolean query, INT32_MIN for
 *     decirational_rational_compare, which otherwise returns -1/0/1). Call
 *     decirational_last_error() for a human-readable reason; it stays valid
 *     until the next decirational_* call on the same thread.
 *   - char* results are heap-allocated by Rust and MUST be freed with
 *     decirational_string_free() - never with free() directly.
 *   - DecirationalRational* and DecirationalInteger* handles are opaque and
 *     MUST be freed with decirational_rational_free() /
 *     decirational_integer_free() respectively. Passing NULL to any free
 *     function is a safe no-op.
 *   - Both handle types are internally tagged with which integer backend
 *     built them (decimal vs. tight, chosen at construction time via
 *     integer_backend); combining two handles of different backends in one
 *     call is a normal (non-crashing) error, not undefined behavior.
 *   - This library is not async-signal-safe and no handle is safe to share
 *     across threads without external synchronization;
 *     decirational_last_error() is thread-local.
 *
 * ---- Example ----
 *
 *   char *result = decirational_eval("100/7", DECIRATIONAL_BACKEND_DECIMAL,
 *                                     DECIRATIONAL_FORMAT_DECIMAL, 0,
 *                                     DECIRATIONAL_ROUNDING_HALF_UP);
 *   if (!result) {
 *       fprintf(stderr, "error: %s\n", decirational_last_error());
 *   } else {
 *       printf("%s\n", result);  // 14.{285714}
 *       decirational_string_free(result);
 *   }
 */

#ifndef DECIRATIONAL_H
#define DECIRATIONAL_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ---- integer_backend values ---- */
#define DECIRATIONAL_BACKEND_DECIMAL 0 /* DecimalInteger: base-10 digits (default) */
#define DECIRATIONAL_BACKEND_TIGHT   1 /* TightInteger: base-2^32 words */

/* ---- format values, matching the CLI's --format flag ---- */
#define DECIRATIONAL_FORMAT_DEFAULT  0
#define DECIRATIONAL_FORMAT_FRACTION 1
#define DECIRATIONAL_FORMAT_MIXED    2
#define DECIRATIONAL_FORMAT_DECIMAL  3
#define DECIRATIONAL_FORMAT_TRUNCATE 4
#define DECIRATIONAL_FORMAT_ROUND    5
#define DECIRATIONAL_FORMAT_CEIL     6
#define DECIRATIONAL_FORMAT_FLOOR    7

/* ---- rounding values, matching the CLI's --rounding flag; only consulted
 * when format is DECIRATIONAL_FORMAT_ROUND ---- */
#define DECIRATIONAL_ROUNDING_HALF_UP   0 /* an exact tie rounds away from zero (default) */
#define DECIRATIONAL_ROUNDING_HALF_EVEN 1 /* an exact tie rounds to the nearest even digit ("banker's rounding") */

/* Opaque handle to a Rational value. */
typedef struct DecirationalRational DecirationalRational;

/* Opaque handle to a DecimalInteger or TightInteger value (the same two
 * CustomInteger backends a DecirationalRational is built on). */
typedef struct DecirationalInteger DecirationalInteger;

/* ---- errors, strings, misc ---- */

/* Most recent error on this thread, or NULL if the last call succeeded.
 * Valid only until the next decirational_* call on this thread. */
const char *decirational_last_error(void);

/* Frees a string returned by any decirational_* function. NULL is a no-op. */
void decirational_string_free(char *s);

/* Static version string; do not free. */
const char *decirational_version(void);

/* ---- expression evaluation (the whole CLI, as one call) ---- */

/* Evaluates a full expression string (+ - * / // % ^, (), [] floor, ||
 * absolute value, decimal and repeating-decimal literals) and returns the
 * formatted result, or NULL on error. rounding (DECIRATIONAL_ROUNDING_*) is
 * only consulted when format is DECIRATIONAL_FORMAT_ROUND. Free with
 * decirational_string_free. */
char *decirational_eval(const char *expression, int32_t integer_backend, int32_t format, int32_t precision, int32_t rounding);

/* ---- construction / destruction ---- */

/* Parses a fraction/decimal/repeating-decimal literal ("3/4", "0.5",
 * "0.{3}", "-7", ...). Returns NULL on error. */
DecirationalRational *decirational_rational_parse(const char *literal, int32_t integer_backend);

/* Builds the fraction value/1. */
DecirationalRational *decirational_rational_from_i64(int64_t value, int32_t integer_backend);

/* Frees a handle returned by any decirational_rational_* function. NULL is a no-op. */
void decirational_rational_free(DecirationalRational *r);

/* Returns a new, independent handle with the same value. */
DecirationalRational *decirational_rational_clone(const DecirationalRational *r);

/* ---- arithmetic (all return a new handle; operands are untouched) ---- */

/* a + b */
DecirationalRational *decirational_rational_add(const DecirationalRational *a, const DecirationalRational *b);
/* a - b */
DecirationalRational *decirational_rational_sub(const DecirationalRational *a, const DecirationalRational *b);
/* a * b */
DecirationalRational *decirational_rational_mul(const DecirationalRational *a, const DecirationalRational *b);
/* a / b (exact division); NULL if b is zero */
DecirationalRational *decirational_rational_div(const DecirationalRational *a, const DecirationalRational *b);
/* -r */
DecirationalRational *decirational_rational_negate(const DecirationalRational *r);
/* |r| */
DecirationalRational *decirational_rational_abs(const DecirationalRational *r);
/* 1/r; NULL if r is zero */
DecirationalRational *decirational_rational_reciprocal(const DecirationalRational *r);
/* r^exponent (exponent may be negative for a nonzero r) */
DecirationalRational *decirational_rational_pow(const DecirationalRational *r, int32_t exponent);

/* All arithmetic functions above require a and b (where both are taken) to
 * share the same integer_backend; mixing backends returns NULL with
 * decirational_last_error() explaining why. */

/* ---- queries ---- */

/* -1 / 0 / 1 as a < / == / > b. Returns INT32_MIN on error (null handle,
 * mismatched backends) - check decirational_last_error() to tell that apart
 * from a genuine result, which is never INT32_MIN. */
int32_t decirational_rational_compare(const DecirationalRational *a, const DecirationalRational *b);

/* 1 / 0 / -1 for true / false / error (check decirational_last_error()). */
int32_t decirational_rational_is_zero(const DecirationalRational *r);
int32_t decirational_rational_is_negative(const DecirationalRational *r);
int32_t decirational_rational_is_integer(const DecirationalRational *r);

/* ---- formatting ---- */

/* Renders r per format/precision/rounding (DECIRATIONAL_FORMAT_* /
 * DECIRATIONAL_ROUNDING_* above, matching the CLI's
 * --format/--precision/--rounding). rounding is only consulted when format
 * is DECIRATIONAL_FORMAT_ROUND. Returns a newly allocated string, or NULL on
 * error. Free with decirational_string_free. */
char *decirational_rational_to_string(const DecirationalRational *r, int32_t format, int32_t precision, int32_t rounding);

/* The (always-reduced) numerator, as a decimal string. */
char *decirational_rational_numerator_string(const DecirationalRational *r);
/* The (always-reduced, always-positive) denominator, as a decimal string. */
char *decirational_rational_denominator_string(const DecirationalRational *r);

/* ---- integer construction / destruction ---- */

/* Parses a (possibly signed) decimal integer literal ("12345", "-7", ...).
 * Returns NULL on error. */
DecirationalInteger *decirational_integer_parse(const char *literal, int32_t integer_backend);

/* Builds an integer handle directly from a machine int64_t. */
DecirationalInteger *decirational_integer_from_i64(int64_t value, int32_t integer_backend);

/* Frees a handle returned by any decirational_integer_* function. NULL is a safe no-op. */
void decirational_integer_free(DecirationalInteger *n);

/* Returns a new, independent handle with the same value. */
DecirationalInteger *decirational_integer_clone(const DecirationalInteger *n);

/* ---- integer arithmetic (all return a new handle; operands are untouched) ---- */

/* a + b */
DecirationalInteger *decirational_integer_add(const DecirationalInteger *a, const DecirationalInteger *b);
/* a - b */
DecirationalInteger *decirational_integer_sub(const DecirationalInteger *a, const DecirationalInteger *b);
/* a * b */
DecirationalInteger *decirational_integer_mul(const DecirationalInteger *a, const DecirationalInteger *b);
/* Truncating integer division a / b (the CLI's //). NULL if b is zero. */
DecirationalInteger *decirational_integer_div(const DecirationalInteger *a, const DecirationalInteger *b);
/* a % b. NULL if b is zero. */
DecirationalInteger *decirational_integer_mod(const DecirationalInteger *a, const DecirationalInteger *b);
/* The (always non-negative) greatest common divisor of a and b. */
DecirationalInteger *decirational_integer_gcd(const DecirationalInteger *a, const DecirationalInteger *b);
/* The least common multiple of a and b. NULL if both are zero. */
DecirationalInteger *decirational_integer_lcm(const DecirationalInteger *a, const DecirationalInteger *b);

/* Divides a by b, producing both the quotient and remainder in one pass.
 * Returns 0 and sets the two out params to newly allocated handles on
 * success; returns -1, sets both outputs to NULL, and records an error
 * (check decirational_last_error()) if b is zero or the backends don't
 * match. out_quotient and out_remainder must not be NULL. */
int32_t decirational_integer_divmod(const DecirationalInteger *a, const DecirationalInteger *b,
                                     DecirationalInteger **out_quotient, DecirationalInteger **out_remainder);

/* Shifts n by `times` "digits" in its own base - decimal digits (n * 10^times)
 * for a decimal-backed handle, base-2^32 words (n * (2^32)^times) for a
 * tight-backed one. NULL if times is negative. */
DecirationalInteger *decirational_integer_multiply_base(const DecirationalInteger *n, int32_t times);
/* The inverse of decirational_integer_multiply_base: divides n by
 * base^times, discarding the low `times` "digits". NULL if times is negative. */
DecirationalInteger *decirational_integer_divide_by_base(const DecirationalInteger *n, int32_t times);
/* n raised to the non-negative exponent. NULL if exponent is negative. */
DecirationalInteger *decirational_integer_pow(const DecirationalInteger *n, int32_t exponent);
/* -n */
DecirationalInteger *decirational_integer_negate(const DecirationalInteger *n);
/* |n| */
DecirationalInteger *decirational_integer_abs(const DecirationalInteger *n);

/* All integer arithmetic functions above require a and b (where both are
 * taken) to share the same integer_backend; mixing backends returns NULL
 * with decirational_last_error() explaining why. */

/* ---- integer queries ---- */

/* -1 / 0 / 1 as a < / == / > b. Returns INT32_MIN on error (null handle,
 * mismatched backends) - check decirational_last_error() to tell that apart
 * from a genuine result, which is never INT32_MIN. */
int32_t decirational_integer_compare(const DecirationalInteger *a, const DecirationalInteger *b);

/* 1 / 0 / -1 for true / false / error (check decirational_last_error()). */
int32_t decirational_integer_is_zero(const DecirationalInteger *n);
int32_t decirational_integer_is_one(const DecirationalInteger *n);
int32_t decirational_integer_is_unit_abs(const DecirationalInteger *n); /* true for exactly 1 and -1 */
int32_t decirational_integer_is_positive(const DecirationalInteger *n);
int32_t decirational_integer_is_negative(const DecirationalInteger *n);

/* ---- integer formatting ---- */

/* Renders n as a decimal string. Free with decirational_string_free. */
char *decirational_integer_to_string(const DecirationalInteger *n);

/* ---- bridging integers and rationals ---- */

/* Builds the fraction n/1 from an integer handle. */
DecirationalRational *decirational_rational_from_integer(const DecirationalInteger *n);

/* Builds a reduced, sign-normalized fraction numerator/denominator from two
 * integer handles. NULL if denominator is zero or the two handles don't
 * share the same backend. */
DecirationalRational *decirational_rational_from_integers(const DecirationalInteger *numerator, const DecirationalInteger *denominator);

/* The (always-reduced) numerator of r, as a new integer handle. */
DecirationalInteger *decirational_rational_numerator(const DecirationalRational *r);
/* The (always-reduced, always-positive) denominator of r, as a new integer handle. */
DecirationalInteger *decirational_rational_denominator(const DecirationalRational *r);

#ifdef __cplusplus
}
#endif

#endif /* DECIRATIONAL_H */
