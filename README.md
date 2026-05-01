# Decirational

## High-Performance Decimal Rational Calculator

Decirational is a highly optimized, cross-platform calculator engine designed exclusively for exact arithmetic within the decimal rational field. It completely bypasses the precision loss inherent in IEEE 754 floating-point representations, making it ideal for financial systems, highly concurrent data processing, and precise mathematical modeling.

To ensure maximum interoperability and performance flexibility, this project provides independent, functionally identical implementations in **Rust**, **Go**, and **Java**, in which the Rust and Go implementations provide C APIs.

## 🚀 Features

* **Exact Decimal Arithmetic**: Absolute precision for all operations in the decimal rational field (addition, subtraction, multiplication, exact division, and integer modulo).
* **High Concurrency Ready**: Thread-safe, immutable architecture designed for lock-free read operations and high-throughput multi-threaded environments.
* **Implementation Parity**: Strict functional and API consistency across Rust, Go, and Java.
* **Zero Dependencies**: Utilizes native big-number abstractions or highly audited, lightweight implementations to maintain secure, high-performance execution.

## 📦 Implementations

### 🦀 Rust
<!--The Rust implementation leverages memory-safe, zero-cost abstractions. It excels in system-level task integration and high-performance utility development where predictable latency and strict memory management are critical.

```rust
use decirational::DecimalRational;

fn main() {
    let a = DecimalRational::from_str("123.4567890123456789").unwrap();
    let b = DecimalRational::from_str("0.0000000000000001").unwrap();
    let result = a + b;
    println!("Result: {}", result);
}
```-->To be developed.

### 🐹 Go
<!--The Go implementation is heavily optimized for concurrent workloads, providing excellent performance for networked microservices and goroutine-based workflows. It provides a clean, idiomatic API wrapping highly optimized arbitrary-precision logic.

```go
package main

import (
    "fmt"
    "[github.com/decirational/go/decirational](https://github.com/decirational/go/decirational)"
)

func main() {
    a, _ := decirational.FromString("123.4567890123456789")
    b, _ := decirational.FromString("0.0000000000000001")
    result := a.Add(b)
    fmt.Printf("Result: %s\n", result.String())
}
```-->To be developed.

### ☕ Java
<!--The Java implementation offers seamless integration into enterprise applications with highly tuned JVM memory allocation strategies.

```java
import com.decirational.DecimalRational;

public class Main {
    public static void main(String[] args) {
        DecimalRational a = new DecimalRational("123.4567890123456789");
        DecimalRational b = new DecimalRational("0.0000000000000001");
        DecimalRational result = a.add(b);
        System.out.println("Result: " + result.toString());
    }
}
```-->Developing.

## ⚙️ Concurrency & Architecture

All three implementations are built specifically for modern, high-concurrency software architectures:
* **Immutable State**: `DecimalRational` instances are strictly immutable across all three languages. This ensures safe sharing across multi-threaded applications and goroutines without introducing mutex bottlenecks.
* **Allocation Efficiency**: The internal representations are heavily optimized to minimize heap allocations during tight-loop calculations, making them exceptionally well-suited for high-throughput processing pipelines or environments operating alongside GPU-accelerated workloads.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.