//! Decirational: an arbitrary-precision decimal-rational calculator engine.

mod arithmetic;
pub mod capi;
mod custom_integer;
mod decimal_integer;
mod lexer;
mod ops_macros;
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
