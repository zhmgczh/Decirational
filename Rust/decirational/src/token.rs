use crate::custom_integer::CustomInteger;
use crate::rational::Rational;
use std::fmt;

/// Every token kind the lexer can produce and the parser consumes.
#[derive(Debug, Clone, PartialEq)]
pub enum Token<T: CustomInteger> {
    /// A literal that fits a 32-bit int.
    Integer(T),
    /// A literal too big for a 32-bit int.
    LargeInteger(T),
    /// A decimal or repeating-decimal literal.
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
    /// which is which is resolved by the parser's grammar position.
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
