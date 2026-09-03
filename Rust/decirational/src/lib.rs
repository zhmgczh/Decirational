//! Decirational: an arbitrary-precision decimal-rational calculator engine.
//!
//! This crate is a faithful port of the Java `Decirational` implementation
//! (`Java/Decirational/src`). The arithmetic (schoolbook add/subtract/
//! multiply, binary-search long division, Euclidean gcd, base conversion),
//! the `Rational<T>` fraction type, and the expression lexer/parser all
//! implement the exact same algorithms; only the OOP structure was adapted
//! to Rust idioms (traits instead of interfaces, enums instead of Java's
//! Token class hierarchy, `Result` instead of unchecked exceptions).

mod arithmetic;
pub mod capi;
mod custom_integer;
mod decimal_integer;
mod lexer;
mod parser;
mod rational;
mod tight_integer;
mod token;

pub use custom_integer::{CustomInteger, DError, DResult};
pub use decimal_integer::DecimalInteger;
pub use lexer::Lexer;
pub use parser::Parser;
pub use rational::{parse_rational, Rational};
pub use tight_integer::TightInteger;
pub use token::Token;

#[cfg(test)]
mod tests;
