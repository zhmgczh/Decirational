use crate::custom_integer::CustomInteger;
use crate::rational::Rational;
use std::fmt;

/// The Rust counterpart of Java's Token hierarchy. Java represents each kind
/// of token as its own class/enum implementing a shared `Token` interface
/// (OperandType, Operator, Parenthesis, Floor, Absolute, plus the Operand
/// class carrying a value); Rust's sum-type enums let all of that collapse
/// into one type with no loss of the same distinctions the parser needs to
/// make - a clean fit for "choose the most suitable structure per language".
#[derive(Debug, Clone, PartialEq)]
pub enum Token<T: CustomInteger> {
    /// A literal that fit a 32-bit int (Java's OperandType::INTEGER).
    Integer(T),
    /// A literal too big for a 32-bit int (Java's OperandType::LARGE_INTEGER).
    LargeInteger(T),
    /// A decimal or repeating-decimal literal (Java's OperandType::RATIONAL).
    Rational(Rational<T>),
    Plus,
    Minus,
    Multiply,
    Divide,
    /// "//" - truncating integer division, paired with `Modulo` the same way
    /// `CustomInteger::divide_by`/`modulo` already pair.
    IntegerDivide,
    Modulo,
    Power,
    LeftParen,
    RightParen,
    LeftFloor,
    RightFloor,
    /// The same `|` character opens and closes an absolute-value group;
    /// which is which is resolved by the parser's grammar position, exactly
    /// as in the Java version.
    AbsoluteBar,
}

impl<T: CustomInteger> fmt::Display for Token<T> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Token::Integer(v) => write!(f, "INTEGER({})", v),
            Token::LargeInteger(v) => write!(f, "LARGE_INTEGER({})", v),
            Token::Rational(r) => write!(f, "RATIONAL({})", r),
            Token::Plus => write!(f, "+"),
            Token::Minus => write!(f, "-"),
            Token::Multiply => write!(f, "*"),
            Token::Divide => write!(f, "/"),
            Token::IntegerDivide => write!(f, "//"),
            Token::Modulo => write!(f, "%"),
            Token::Power => write!(f, "^"),
            Token::LeftParen => write!(f, "("),
            Token::RightParen => write!(f, ")"),
            Token::LeftFloor => write!(f, "["),
            Token::RightFloor => write!(f, "]"),
            Token::AbsoluteBar => write!(f, "|"),
        }
    }
}
