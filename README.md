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
* **Thoroughly Tested**: 506 assertions in Java, 55 test functions in Go, and 60 in Rust, covering the same arithmetic edge cases, parsing rules, and error paths in every implementation.

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
$ echo '2^10 + |3-10| * [7.5]' | decirational --format=decimal
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

The Rust implementation lives under [`Rust/decirational`](Rust/decirational): a library crate (`decirational`) plus a `decirational` binary, with zero external dependencies. Fallible parsing/construction returns `Result<_, DError>` (Rust's idiomatic mechanism, unlike the unchecked exceptions Java throws or the panic/recover Go uses for the same cases); a handful of pure-arithmetic edge cases (dividing by zero, an absurd `--precision`) `panic!`, matching how Rust's own `/` operator behaves.

60 `#[test]` functions live in [`src/tests.rs`](Rust/decirational/src/tests.rs) and [`src/capi.rs`](Rust/decirational/src/capi.rs) — `cargo test` to run them (the C API additionally has its own from-C test, see below).

#### Running it

The prebuilt Apptainer image needs no local Rust toolchain:

```bash
echo '100/7' | apptainer run Rust/Apptainer/decirational.sif --format=decimal
# 14.{285714}
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
echo '100/7' | ./target/release/decirational --format=decimal
```

#### Calling the package separately

Add a path (or git) dependency on `Rust/decirational` and use its types directly:

```rust
use decirational::{DecimalInteger, Rational};

let a = Rational::new(DecimalInteger::from_i64(1), DecimalInteger::from_i64(3))?;
let b = Rational::new(DecimalInteger::from_i64(1), DecimalInteger::from_i64(6))?;
println!("{}", a.plus(&b).to_decimal_string()); // 0.5
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
    char *result = decirational_eval("100/7", DECIRATIONAL_BACKEND_DECIMAL,
                                      DECIRATIONAL_FORMAT_DECIMAL, 0);
    if (!result) {
        fprintf(stderr, "error: %s\n", decirational_last_error());
        return 1;
    }
    printf("%s\n", result);  // 14.{285714}
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
char *s = decirational_rational_to_string(sum, DECIRATIONAL_FORMAT_DECIMAL, 0); // "0.5"
decirational_string_free(s);
decirational_rational_free(sum);
decirational_rational_free(a);
decirational_rational_free(b);
```

See the header for the full function list (construction, `add`/`sub`/`mul`/`div`/`pow`/`negate`/`abs`/`reciprocal`, `compare`, `is_zero`/`is_negative`/`is_integer`, and all the `--format` variants) and its conventions: null-on-error (check `decirational_last_error()`), `decirational_string_free`/`decirational_rational_free` for cleanup, and handles tagged internally by which backend built them — mixing a `decimal` and a `tight` handle in one call is a reported error, not undefined behavior.

### 🐹 Go

The Go implementation lives under [`Go/Decirational`](Go/Decirational) as the importable package `decirational`, with its CLI in [`cmd/decirational`](Go/Decirational/cmd/decirational). It targets Go 1.23+ (for the `clear`/`min`/`max` builtins) and has zero external dependencies. Fallible parsing/construction returns `(_, error)`; mid-computation arithmetic failures (division by zero, matching Go's own `/` operator) panic and are recovered once at `Parser.Parse`, so callers only ever see a single `error`.

55 test functions live across [`*_test.go`](Go/Decirational) — `go test ./...` to run them.

#### Running it

```bash
echo '100/7' | apptainer run Go/Apptainer/decirational.sif --format=decimal
# 14.{285714}
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
echo '100/7' | ./decirational --format=decimal
```

#### Calling the package separately

```go
import dec "decirational"

a, _ := dec.NewRational(dec.NewDecimalIntegerFromInt64(1), dec.NewDecimalIntegerFromInt64(3))
b, _ := dec.NewRational(dec.NewDecimalIntegerFromInt64(1), dec.NewDecimalIntegerFromInt64(6))
fmt.Println(a.Plus(b).ToDecimalString()) // 0.5
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

The Java implementation lives under [`Java/Decirational`](Java/Decirational). It targets JDK 21, has zero external dependencies, and every class (arbitrary-precision integers, rationals, lexer, parser) is plain `.java` with no package declaration — nothing to install beyond a JDK.

506 assertions live across [`Java/Decirational/test`](Java/Decirational/test) — run them via `javac -d out src/*.java test/*.java && java -cp out AllTests`.

#### Running it

The prebuilt Apptainer image needs no local JDK:

```bash
echo '100/7' | apptainer run Java/Apptainer/decirational.sif --format=decimal
# 14.{285714}
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
java -cp out Main
```

#### Calling the package separately

The sources have no package declaration, so you can drop the `.java` files from `Java/Decirational/src` straight into your own project (or compile them into a `.jar`) and call the API directly, without going through the REPL:

```java
Rational<DecimalInteger> a = new Rational<>("123.4567890123456789", DecimalInteger.class);
Rational<DecimalInteger> b = new Rational<>("0.0000000000000001", DecimalInteger.class);
Rational<DecimalInteger> result = a.plus(b);
System.out.println(result);                    // fraction form, via toString()
System.out.println(result.to_decimal_string()); // decimal form, with {cyclic} repetends
```

Or drive the lexer/parser directly on a raw expression string, the same way `Main` does internally:

```java
Lexer<TightInteger> lexer = new Lexer<>(TightInteger.class, (Class<Rational<TightInteger>>) (Class<?>) Rational.class);
Parser<TightInteger> parser = new Parser<>(TightInteger.class);
Rational<TightInteger> result = parser.parse(lexer.get_tokens("(1+2)*|-4|^2/[7.5]"));
```

Core classes:

* `DecimalInteger` / `TightInteger` — arbitrary-precision signed integers (`CustomInteger<T>`), interchangeable backends for `Rational<T>` (see above).
* `Rational<T>` — exact fractions over a `CustomInteger<T>`: `plus`/`minus`/`multiply`/`divide_by`/`pow`/`reciprocal`; parsing of fractions, decimals, and repeating decimals from strings; and formatting via `toString`, `to_fraction_string`, `to_mixed_string`, `to_decimal_string`, `to_truncate_decimal_string`, `to_round_decimal_string`, `to_ceil_decimal_string`, `to_floor_decimal_string`.
* `Lexer<T>` — tokenizes an expression string into `Token`s.
* `Parser<T>` — a recursive-descent evaluator that turns tokens into a `Rational<T>`.

## ⚙️ Concurrency & Architecture

All three implementations are built specifically for modern, high-concurrency software architectures:
* **Immutable State**: `Rational` instances (and their underlying `DecimalInteger`/`TightInteger` values) are strictly immutable across all three languages. This ensures safe sharing across multi-threaded applications and goroutines without introducing mutex bottlenecks.
* **Allocation Efficiency**: The internal representations are heavily optimized to minimize heap allocations during tight-loop calculations, making them exceptionally well-suited for high-throughput processing pipelines or environments operating alongside GPU-accelerated workloads.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
