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
 *   - DecirationalRational* handles are opaque and MUST be freed with
 *     decirational_rational_free(). Passing NULL to either free function is
 *     a safe no-op.
 *   - A DecirationalRational is internally tagged with which integer
 *     backend built it (decimal vs. tight, chosen at construction time via
 *     integer_backend); combining two handles of different backends in one
 *     call is a normal (non-crashing) error, not undefined behavior.
 *   - This library is not async-signal-safe and each DecirationalRational*
 *     handle is not safe to share across threads without external
 *     synchronization; decirational_last_error() is thread-local.
 *
 * ---- Example ----
 *
 *   char *result = decirational_eval("100/7", DECIRATIONAL_BACKEND_DECIMAL,
 *                                     DECIRATIONAL_FORMAT_DECIMAL, 0);
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

/* Opaque handle to a Rational value. */
typedef struct DecirationalRational DecirationalRational;

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
 * formatted result, or NULL on error. Free with decirational_string_free. */
char *decirational_eval(const char *expression, int32_t integer_backend, int32_t format, int32_t precision);

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

/* Renders r per format/precision (DECIRATIONAL_FORMAT_* /
 * DECIRATIONAL_BACKEND_* above, matching the CLI's --format/--precision).
 * Returns a newly allocated string, or NULL on error. Free with
 * decirational_string_free. */
char *decirational_rational_to_string(const DecirationalRational *r, int32_t format, int32_t precision);

/* The (always-reduced) numerator, as a decimal string. */
char *decirational_rational_numerator_string(const DecirationalRational *r);
/* The (always-reduced, always-positive) denominator, as a decimal string. */
char *decirational_rational_denominator_string(const DecirationalRational *r);

#ifdef __cplusplus
}
#endif

#endif /* DECIRATIONAL_H */
