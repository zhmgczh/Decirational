# Decirational

## High-Performance Decimal Rational Calculator

Decirational is a highly optimized, cross-platform calculator engine designed exclusively for exact arithmetic within the decimal rational field. It completely bypasses the precision loss inherent in IEEE 754 floating-point representations, making it ideal for financial systems, highly concurrent data processing, and precise mathematical modeling.

To ensure maximum interoperability and performance flexibility, this project provides independent, functionally identical implementations in **Rust**, **Go**, and **Java**.

## 🚀 Features

* **Exact Decimal Arithmetic Calculator**: Absolute precision for all numbers and operations in the decimal rational field (decimal integers with arbitrary length, decimal numbers accurate to any places, repeating decimals with the repetend enclosed by {}, addition +, subtraction -, multiplication *, exact division /, truncating integer division //, integer modulo %, exponentiation ^, parenthesis (), absolute value |, and round-down floor []).
* **Easy-to-use APIs to call**: For experienced programmers to be much more flexible and able to perform other advanced functions, some well-defined APIs are provided and are easy to call — including a raw C ABI for the Rust implementation (see below), for calling it from C, C++, or any other language that links against a C ABI.
* **High Concurrency Ready**: Thread-safe, immutable architecture designed for lock-free read operations and high-throughput multi-threaded environments.
* **Implementation Parity**: Strict functional and API consistency across Rust, Go, and Java — the three CLIs accept identical flags and produce byte-for-byte identical output for identical input.
* **Zero Dependencies**: Utilizes native big-number abstractions or highly audited, lightweight implementations to maintain secure, high-performance execution.
* **Thoroughly Tested**: 518 assertions in Java, 60 test functions in Go, and 65 in Rust, covering the same arithmetic edge cases, parsing rules, and error paths in every implementation.

## 🧮 The Calculator

This section describes behavior shared identically by all three CLIs — the expression language, and every command-line flag. The per-language sections below cover only what differs: how to build/run that language's binary and how to call its library API.

### Expression language

Each CLI is a REPL: it reads one arithmetic expression per line from standard input and prints its value. It supports:

| Syntax | Meaning |
|---|---|
| `+` `-` `*` `^` | addition, subtraction, multiplication, exponentiation |
| `/` | **exact** division — the mathematical result in the rational field, e.g. `100/7` stays the exact fraction `100/7` |
| `//` | **integer** division — the truncated-toward-zero integer quotient, paired with `%` exactly the way `CustomInteger`'s `divide_by`/`modulo` already pair (`(a//b)*b + (a%b) == a`); requires both operands to be integers |
| `%` | integer modulo (remainder takes the dividend's sign); requires both operands to be integers |
| `( )` | grouping |
| `[ ]` | floor — round down to the nearest integer |
| `\| \|` | absolute value |
| `0.5`, `-3.25` | decimal literals |
| `0.{3}`, `1.5{6}` | repeating decimals — the repetend goes inside `{ }` (`0.{3}` = 1/3) |

Standard precedence applies (`^` binds tightest and is right-associative, then `* / // %` — all the same precedence, left-associative — then `+ -`), unary `+`/`-` are supported, and arbitrarily large integers are supported natively — there is no overflow. `//`, `%`, and exponent-of-a-fraction only accept integer operands; dividing by zero, unmatched brackets, and malformed literals are all reported as `Error: ...` without stopping the REPL. A run of three or more `/` characters is read left to right in pairs (`///` is `//` then `/`; `////` is `//` then `//`).

```
$ echo '100/7' | decirational
100/7
$ echo '100//7' | decirational
14
$ echo '2^10 + |3-10| * [7.5]' | decirational
1073
```

### `--integer`: DecimalInteger vs. TightInteger

Every number is ultimately backed by one of two arbitrary-precision integer implementations, selectable independently of everything else:

| | `decimal` (default) | `tight` |
|---|---|---|
| Internal representation | one **byte** per decimal digit, e.g. `"1234"` → `[1,2,3,4]` | one **32-bit word** per base-2³² digit |
| Digits needed for the same magnitude | *n* | ≈ *n* / 9.63 |

The two are functionally interchangeable (every operation and every output is identical either way) but represent numbers very differently internally, which shows up as a real, measurable performance trade-off:

**`DecimalInteger` (`--integer=decimal`)** stores the number as literally its own decimal digits.
- ✅ Parsing a decimal literal and printing a result are both effectively free — an O(*n*) pass that just maps digits to characters, with no arithmetic involved. Since a calculator spends most of its time reading and printing decimal text, this dominates for ordinary interactive use.
- ✅ The internal representation *is* the decimal representation, so it's trivially human-inspectable when debugging.
- ❌ Every digit only carries 4 bits of information (0-9) but occupies a full byte, and — the bigger cost — a number needs about **9.6× more array elements** than `TightInteger` to represent the same magnitude (see the ratio above; base 2³² packs `log₁₀(2³²) ≈ 9.633` decimal digits per word). Since schoolbook addition/multiplication and long division all scale with digit count, every arithmetic operation on a large number does correspondingly more work.

**`TightInteger` (`--integer=tight`)** stores the number as base-2³² words.
- ✅ With ~9.6× fewer array elements per number, and schoolbook multiplication costing O(len(*a*)·len(*b*)), multiplying two similarly-sized large numbers takes roughly 9.6² ≈ **93× fewer element-wise multiply-accumulate steps** than `DecimalInteger`.
- ⚠️ Long division claws some of that back: finding each quotient digit is a binary search over that digit's possible values — 0-9 for decimal (⌈log₂10⌉ = **4** trial multiplications) versus the full 32-bit range for tight (⌈log₂2³²⌉ = **32** trial multiplications, 8× wider). Net effect for division-heavy work: (4/32) × 9.6² ≈ **11.6× fewer total operations** — still a clear win, just a smaller one than multiplication's ~93×.
- ❌ None of that speed is free at the edges: every decimal literal typed in and every result printed out requires an actual base conversion between 2³² and 10 — the same long-division machinery, run once per line — rather than the free reinterpretation `DecimalInteger` gets. For a REPL session of small, mostly-typed expressions, that per-line conversion overhead can outweigh the deeper-arithmetic win entirely.
- ❌ The internal words aren't decimal digits, so they're opaque if you're debugging the library itself rather than just using the calculator.

**In short**: keep the default (`decimal`) for typical interactive use and small-to-medium numbers, where parsing and printing dominate. Reach for `--integer=tight` when you're doing arithmetic-heavy work on numbers that are already large — long chains of multiplication, exponentiation, or division on numbers with hundreds or thousands of digits — where the arithmetic performed between one parse and one print vastly outweighs the conversion cost of that single parse and print.

### `--format`: how the result is rendered

`Rational`'s value is always exact internally; `--format` only controls how it's turned into text. Examples below use `7/4` (a terminating value) and `100/7` (a repeating one):

| Format | Meaning | `7/4` | `100/7` |
|---|---|---|---|
| `default` | `Rational`'s natural form: a fraction, or a plain integer when the denominator is 1 | `7/4` | `100/7` |
| `fraction` | always `numerator/denominator`, even for a whole number (`default` would print `5` for the value `5`; `fraction` prints `5/1`) | `7/4` | `100/7` |
| `mixed` | a whole part plus a proper fraction | `1 3/4` | `14 2/7` |
| `decimal` | the full decimal expansion, with any repeating digits wrapped in `{}` | `1.75` | `14.{285714}` |
| `truncate` | decimal expansion cut off (toward zero) at `--precision` digits | `1.75` | `14.28` |
| `round` | decimal expansion rounded half-up at `--precision` digits | `1.75` | `14.29` |
| `ceil` | rounded up (toward +∞) to `--precision` digits | `2` | `15` |
| `floor` | rounded down (toward −∞) to `--precision` digits | `1` | `14` |

(`ceil`/`floor` in the table above use the default `--precision=0`, i.e. round to the nearest whole number.)

### `--precision`: digits of precision for truncate/round/ceil/floor

An integer, default `0`, giving the number of digits **after** the decimal point to keep; ignored by `default`/`fraction`/`mixed`/`decimal`, which are always exact. It may be **negative**, which rounds to the nearest ten, hundred, etc. *before* the decimal point instead:

```
$ echo '1234' | decirational --format=truncate --precision=-2
1200
```

## 📦 Implementations

### 🦀 Rust

The Rust implementation lives under [`Rust/decirational`](Rust/decirational): a library crate (`decirational`) plus a `decirational` binary, with zero external dependencies. Fallible parsing/construction returns `Result<_, DError>` (Rust's idiomatic mechanism, unlike the unchecked exceptions Java throws or the panic/recover Go uses for the same cases); a handful of pure-arithmetic edge cases (dividing by zero, an absurd `--precision`) `panic!`, matching how Rust's own `/` operator behaves. `DecimalInteger`, `TightInteger`, and `Rational<T>` all implement the standard `std::ops` operators (`+ - * / % -`, in every value/reference combination — `a + b`, `&a + &b`, etc.) and `FromStr`, so `a + b` and `"1/3".parse()` work exactly as they would for any other Rust numeric type, on top of the `plus`/`minus`/`multiply`/`divide_by`/`modulo`/`negate` methods every port shares.

65 `#[test]` functions live in [`src/tests.rs`](Rust/decirational/src/tests.rs) and [`src/capi.rs`](Rust/decirational/src/capi.rs) — `cargo test` to run them (the C API additionally has its own from-C test, see below).

#### Running it

The prebuilt Apptainer image needs no local Rust toolchain:

```bash
echo '0.1+0.2-0.3+1/3' | apptainer run Rust/Apptainer/decirational.sif
# 1/3 - exact throughout: 0.1+0.2-0.3 cancels perfectly (no IEEE 754 residue), then adds cleanly to 1/3
```

Or build it yourself, either via Apptainer ([`Rust/Apptainer/decirational.def`](Rust/Apptainer/decirational.def)):

```bash
cd Rust/Apptainer
apptainer build decirational.sif decirational.def
apptainer run decirational.sif
```

or directly with Cargo:

```bash
cd Rust/decirational
cargo build --release
echo '0.1+0.2-0.3+1/3' | ./target/release/decirational
```

#### Calling the package separately

Add a path (or git) dependency on `Rust/decirational` and use its types directly:

```rust
use decirational::{DecimalInteger, Rational};

let a: Rational<DecimalInteger> = "1/3".parse()?;
let b: Rational<DecimalInteger> = "1/6".parse()?;
let sum = &a + &b; // or a.plus(&b) - both are available
println!("{}", sum);                    // 1/2 - fraction form, via Display (Rust's toString() equivalent)
println!("{}", sum.to_decimal_string()); // 0.5 - decimal form, with {cyclic} repetends
```

Or drive the lexer/parser directly on a raw expression string, the same way the `decirational` binary does internally:

```rust
use decirational::{DecimalInteger, Lexer, Parser};

let lexer = Lexer::new(DecimalInteger::from_i32, DecimalInteger::parse);
let mut parser = Parser::<DecimalInteger>::new();
let result = parser.parse(lexer.get_tokens("(1+2)*|-4|^2/[7.5]")?)?;
```

Core types: the `CustomInteger` trait, implemented by `DecimalInteger` and `TightInteger` (interchangeable backends, see above); `Rational<T>` for exact fractions, with the same `plus`/`minus`/`multiply`/`divide_by`/`pow`/`reciprocal`, string parsing, and formatting methods (`to_fraction_string`, `to_mixed_string`, `to_decimal_string`, `to_truncate_decimal_string`, `to_round_decimal_string`, `to_ceil_decimal_string`, `to_floor_decimal_string`) as the Java version; `Lexer<T>` and `Parser<T>`; and `Token<T>`, a plain enum standing in for Java's Token class hierarchy.

#### Calling the raw C API

`Rust/decirational` also builds as a `cdylib`/`staticlib` (see `crate-type` in [`Cargo.toml`](Rust/decirational/Cargo.toml)) exposing a hand-written C ABI — [`src/capi.rs`](Rust/decirational/src/capi.rs), declared in [`capi/decirational.h`](Rust/decirational/capi/decirational.h) — for calling this from C, C++, or anything else that links against a C ABI (Python via `ctypes`/`cffi`, Ruby FFI, etc.), no Cargo required at the call site. It covers the whole calculator: evaluate an expression string in one call, or build/combine/format `Rational` values by hand through an opaque handle.

Every Rust panic (dividing by zero, an absurd `--precision`) is caught right at the boundary and turned into a null return plus `decirational_last_error()` — never allowed to unwind into C, which would be undefined behavior. That's verified against a real C program, [`capi/test_decirational.c`](Rust/decirational/capi/test_decirational.c), exercised against both the dynamic and the static library:

```bash
cd Rust/decirational
./run_capi_test.sh   # builds libdecirational.{so,a} and runs the C test against both
```

```c
#include "decirational.h"
#include <stdio.h>

int main(void) {
    // Exact throughout: 0.1+0.2-0.3 cancels perfectly (no IEEE 754 residue), then adds cleanly to 1/3.
    char *result = decirational_eval("0.1+0.2-0.3+1/3", DECIRATIONAL_BACKEND_DECIMAL,
                                      DECIRATIONAL_FORMAT_DEFAULT, 0);
    if (!result) {
        fprintf(stderr, "error: %s\n", decirational_last_error());
        return 1;
    }
    printf("%s\n", result);  // 1/3
    decirational_string_free(result);
    return 0;
}
```

```bash
gcc -o myprog myprog.c -Icapi -Ltarget/release -ldecirational
LD_LIBRARY_PATH=target/release ./myprog
```

For finer-grained control than one-shot `decirational_eval`, build and combine `Rational` handles directly:

```c
DecirationalRational *a = decirational_rational_parse("1/3", DECIRATIONAL_BACKEND_DECIMAL);
DecirationalRational *b = decirational_rational_parse("1/6", DECIRATIONAL_BACKEND_DECIMAL);
DecirationalRational *sum = decirational_rational_add(a, b);
char *s1 = decirational_rational_to_string(sum, DECIRATIONAL_FORMAT_DEFAULT, 0);  // "1/2" - fraction form
char *s2 = decirational_rational_to_string(sum, DECIRATIONAL_FORMAT_DECIMAL, 0);  // "0.5" - decimal form
decirational_string_free(s1);
decirational_string_free(s2);
decirational_rational_free(sum);
decirational_rational_free(a);
decirational_rational_free(b);
```

See the header for the full function list (construction, `add`/`sub`/`mul`/`div`/`pow`/`negate`/`abs`/`reciprocal`, `compare`, `is_zero`/`is_negative`/`is_integer`, and all the `--format` variants) and its conventions: null-on-error (check `decirational_last_error()`), `decirational_string_free`/`decirational_rational_free` for cleanup, and handles tagged internally by which backend built them — mixing a `decimal` and a `tight` handle in one call is a reported error, not undefined behavior.

### 🐹 Go

The Go implementation lives under [`Go/Decirational`](Go/Decirational) as the importable package `decirational`, with its CLI in [`cmd/decirational`](Go/Decirational/cmd/decirational). It targets Go 1.23+ (for the `clear`/`min`/`max` builtins) and has zero external dependencies. Fallible parsing/construction returns `(_, error)`; mid-computation arithmetic failures (division by zero, matching Go's own `/` operator) panic and are recovered once at `Parser.Parse`, so callers only ever see a single `error`.

60 test functions live across [`*_test.go`](Go/Decirational) — `go test ./...` to run them.

#### Running it

```bash
echo '0.1+0.2-0.3+1/3' | apptainer run Go/Apptainer/decirational.sif
# 1/3 - exact throughout: 0.1+0.2-0.3 cancels perfectly (no IEEE 754 residue), then adds cleanly to 1/3
```

Or build it yourself, either via Apptainer ([`Go/Apptainer/decirational.def`](Go/Apptainer/decirational.def)):

```bash
cd Go/Apptainer
apptainer build decirational.sif decirational.def
apptainer run decirational.sif
```

or directly with Go:

```bash
cd Go/Decirational
go build -o decirational ./cmd/decirational
echo '0.1+0.2-0.3+1/3' | ./decirational
```

#### Calling the package separately

The module lives in a subdirectory of this repo, not at its root, so its module path carries that subdirectory (`go get` resolves this the same way it would any repo where the module isn't at the root):

```bash
go get github.com/zhmgczh/Decirational/Go/Decirational
```

```go
import dec "github.com/zhmgczh/Decirational/Go/Decirational"

a, _ := dec.NewRational(dec.NewDecimalIntegerFromInt64(1), dec.NewDecimalIntegerFromInt64(3))
b, _ := dec.NewRational(dec.NewDecimalIntegerFromInt64(1), dec.NewDecimalIntegerFromInt64(6))
sum := a.Plus(b)
fmt.Println(sum)                    // 1/2 - fraction form, via String() (Go's toString() equivalent)
fmt.Println(sum.ToDecimalString())  // 0.5 - decimal form, with {cyclic} repetends
```

Or drive the lexer/parser directly on a raw expression string:

```go
lexer := dec.NewLexer[dec.DecimalInteger](dec.NewDecimalIntegerFromInt32, dec.ParseDecimalInteger)
parser := dec.NewParser[dec.DecimalInteger]()
tokens, _ := lexer.GetTokens("(1+2)*|-4|^2/[7.5]")
result, _ := parser.Parse(tokens)
```

Core types: the generic `CustomInteger[T]` interface, implemented by `DecimalInteger` and `TightInteger` (see above); `Rational[T]` with the same method set as the Java and Rust versions; `Lexer[T]` and `Parser[T]`; and `Token`, backed by small comparable types (`OperatorKind`, `ParenKind`, `FloorKind`, `AbsoluteKind`) plus a generic `Operand[T]`.

### ☕ Java

The Java implementation lives under [`Java/Decirational`](Java/Decirational). It targets JDK 21, has zero external dependencies, and every class (arbitrary-precision integers, rationals, lexer, parser) lives in a single `decirational` package — nothing to install beyond a JDK.

518 assertions live across [`Java/Decirational/test`](Java/Decirational/test) (also in the `decirational` package, so they call the API directly with no imports needed) — run them via `javac -d out src/*.java test/*.java && java -cp out decirational.AllTests`.

#### Running it

The prebuilt Apptainer image needs no local JDK:

```bash
echo '0.1+0.2-0.3+1/3' | apptainer run Java/Apptainer/decirational.sif
# 1/3 - exact throughout: 0.1+0.2-0.3 cancels perfectly (no IEEE 754 residue), then adds cleanly to 1/3
```

Or build it yourself, either via Apptainer ([`Java/Apptainer/decirational.def`](Java/Apptainer/decirational.def)):

```bash
cd Java/Apptainer
apptainer build decirational.sif decirational.def
apptainer run decirational.sif
```

or with a JDK 21+ toolchain directly:

```bash
cd Java/Decirational/src
javac -d out *.java
java -cp out decirational.Main
```

#### Calling the package separately

The classes live in the `decirational` package, so you can drop the `.java` files from `Java/Decirational/src` straight into your own project (or compile them into a `.jar`) and call the API directly, without going through the REPL:

```java
import decirational.DecimalInteger;
import decirational.Rational;

Rational<DecimalInteger> a = new Rational<>("1/3", DecimalInteger::new);
Rational<DecimalInteger> b = new Rational<>("1/6", DecimalInteger::new);
Rational<DecimalInteger> result = a.plus(b);
System.out.println(result);                    // 1/2 - fraction form, via toString()
System.out.println(result.to_decimal_string()); // 0.5 - decimal form, with {cyclic} repetends
```

Or drive the lexer/parser directly on a raw expression string, the same way `Main` does internally:

```java
import decirational.Lexer;
import decirational.Parser;
import decirational.TightInteger;

Lexer<TightInteger> lexer = new Lexer<>(TightInteger::new);
Parser<TightInteger> parser = new Parser<>(TightInteger::new);
Rational<TightInteger> result = parser.parse(lexer.get_tokens("(1+2)*|-4|^2/[7.5]"));
```

Core classes:

* `DecimalInteger` / `TightInteger` — arbitrary-precision signed integers (`CustomInteger<T>`), interchangeable backends for `Rational<T>` (see above).
* `Rational<T>` — exact fractions over a `CustomInteger<T>`: `plus`/`minus`/`multiply`/`divide_by`/`pow`/`reciprocal`; parsing of fractions, decimals, and repeating decimals from strings; and formatting via `toString`, `to_fraction_string`, `to_mixed_string`, `to_decimal_string`, `to_truncate_decimal_string`, `to_round_decimal_string`, `to_ceil_decimal_string`, `to_floor_decimal_string`.
* `Lexer<T>` — tokenizes an expression string into `Token`s.
* `Parser<T>` — a recursive-descent evaluator that turns tokens into a `Rational<T>`.

## 📚 API Reference

The per-language sections above cover how to build/run each binary and how the API looks in that language's idiom. This section is the language-agnostic method reference for the two core types shared identically across Rust, Go, and Java.

### `CustomInteger`: the shared DecimalInteger / TightInteger API

`DecimalInteger` and `TightInteger` are two interchangeable backends (see [`--integer`](#--integer-decimalinteger-vs-tightinteger) above) for the exact same interface — `CustomInteger<T>` in Java, `CustomInteger[T]` in Go, and the `CustomInteger` trait in Rust — so switching backends never changes which operations are available, only how fast they run. Method names below are Java/Rust; Go exposes the identical set in PascalCase (`is_zero` → `IsZero`, `divide_by_base` → `DivideByBase`, and so on):

| Method(s) | Meaning |
|---|---|
| `is_zero`, `is_one`, `is_unit_abs` | is the value `0`, `1`, or `±1` |
| `is_positive`, `is_negative` | sign checks (`0` is neither) |
| `negate`, `abs` | unary negation / absolute value |
| `plus`, `minus`, `multiply` | addition, subtraction, multiplication |
| `divide_by`, `modulo` | truncating-toward-zero integer division and its paired remainder — `a.divide_by(b) * b + a.modulo(b) == a`, the same pairing the calculator's `//`/`%` operators use |
| `divide_by_and_modulo` | both of the above in a single pass, for when you need both results |
| `gcd`, `lcm` | greatest common divisor / least common multiple |
| `pow(exponent)` | integer exponentiation |
| `multiply_base(n)` / `multiply_base_once`, `divide_by_base(n)` / `divide_by_base_once` | shift the value by `n` "digits" in its own base — decimal digits (×10ⁿ) for `DecimalInteger`, base-2³² words (×(2³²)ⁿ) for `TightInteger` — the fast path the parser uses internally for decimal-point placement instead of general-purpose multiplication/division |
| `to_tight_integer` / `to_decimal_integer` | convert losslessly to the other backend |
| `get_digit(0-9)` (`digit` in Rust) | the interned constant for a single digit, e.g. `DecimalInteger.get_digit(7)` is `7` |
| `compareTo`/`Compare`, `equals`/`Equals`, `toString`/`String`/`Display` | ordering, equality, and text conversion |

Construction covers every signed integer width native to that language (`byte`/`short`/`int`/`long` in Java, `int32`/`int64` in Go and Rust) plus parsing directly from a decimal string — the same literal syntax the calculator itself accepts (`new DecimalInteger("12345")` in Java, `DecimalInteger::parse("12345")` in Rust, `dec.ParseDecimalInteger("12345")` in Go). Raw digit/word arrays are also accepted (`new DecimalInteger(digits, negative)`, `DecimalInteger::from_digits`, `dec.NewDecimalInteger(digits, negative)`) for callers building a value digit-by-digit rather than from a string or a native integer.

### `Rational<T>`: the exact-fraction API

Every value the calculator computes is a `Rational<T>` — an exact fraction over a `CustomInteger` numerator and denominator, always kept reduced to lowest terms with the sign carried on the numerator only (the denominator is always positive). Method names below are Java/Rust; Go again exposes the identical set in PascalCase (`is_zero` → `IsZero`, `divide_by` → `DivideBy`, and so on):

| Method(s) | Meaning |
|---|---|
| `get_numerator`, `get_denominator` | the reduced numerator/denominator, sign included on the numerator |
| `get_numerator_abs`, `get_denominator_abs` | the same, with the sign stripped |
| `is_integer` | is the denominator `1` |
| `is_zero`, `is_positive`, `is_negative` | sign checks on the value as a whole |
| `negate`, `abs` | unary negation / absolute value |
| `reciprocal` | `1/x` — swaps numerator and denominator, carrying the sign back to the numerator; an error/panic for `0`, same as the calculator dividing by zero |
| `plus`, `minus`, `multiply`, `divide_by` | the calculator's `+ - * /`, always exact — no rounding at any intermediate step |
| `pow(exponent)` | exponentiation; a negative exponent is `reciprocal().pow(-exponent)`, matching `^` in the expression language |
| `compareTo`/`Compare`, `equals`/`Equals` | ordering and equality by value (`1/2` equals `2/4`) |
| `toString`/`String`/`Display`, `to_fraction_string`, `to_mixed_string`, `to_decimal_string`, `to_truncate_decimal_string`, `to_round_decimal_string`, `to_ceil_decimal_string`, `to_floor_decimal_string` | text conversion — one method per [`--format`](#--format-how-the-result-is-rendered) value above |

Construction accepts a numerator/denominator pair (`new Rational<>(n, d)` / `Rational::new` / `NewRational`, auto-reducing and rejecting a zero denominator), a bare integer (`from_integer`/`NewRationalFromInteger`, denominator `1`), or a string in any literal syntax the calculator itself accepts: a fraction, a decimal, or a repeating decimal (`new Rational<>("1.5{6}", DecimalInteger::new)` in Java, `dec.ParseRational[dec.DecimalInteger]("1.5{6}", dec.ParseDecimalInteger)` in Go, `"1.5{6}".parse::<Rational<DecimalInteger>>()` or the free function `parse_rational` in Rust — `Rational<T>` implements `FromStr` whenever `T` does, which both `DecimalInteger` and `TightInteger` do).

## ⚙️ Concurrency & Architecture

All three implementations are built specifically for modern, high-concurrency software architectures:
* **Immutable State**: `Rational` instances (and their underlying `DecimalInteger`/`TightInteger` values) are strictly immutable across all three languages. This ensures safe sharing across multi-threaded applications and goroutines without introducing mutex bottlenecks.
* **Allocation Efficiency**: The internal representations are heavily optimized to minimize heap allocations during tight-loop calculations, making them exceptionally well-suited for high-throughput processing pipelines or environments operating alongside GPU-accelerated workloads.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
