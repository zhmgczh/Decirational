use crate::arithmetic::{is_cyclic_begin, is_cyclic_end, is_decimal_point, is_digit};
use crate::custom_integer::{CustomInteger, DError, DResult};
use crate::decimal_integer::strip_whitespace;
use crate::rational::parse_rational;
use crate::token::Token;
use std::marker::PhantomData;

/// Tokenizes an expression string: every structural character (one of
/// "()[]|+-*/%^") is always its own token, and every maximal run of number
/// characters (digits, '.', '{', '}') becomes one operand token. Carries no
/// state of its own - `T::from_i32`/`T::parse` (see `CustomInteger`) build
/// operands directly, so there's nothing to store per instance.
pub struct Lexer<T: CustomInteger>(PhantomData<T>);

fn is_number_char(c: u8) -> bool {
    is_digit(c) || is_decimal_point(c) || is_cyclic_begin(c) || is_cyclic_end(c)
}

impl<T: CustomInteger> Default for Lexer<T> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T: CustomInteger> Lexer<T> {
    pub fn new() -> Self {
        Lexer(PhantomData)
    }

    fn parse_operand(&self, value: &str) -> DResult<Token<T>> {
        if let Ok(n) = value.parse::<i32>() {
            return Ok(Token::Integer(T::from_i32(n)));
        }
        if let Ok(large) = T::parse(value) {
            return Ok(Token::Integer(large));
        }
        match parse_rational(value) {
            Ok(r) => Ok(Token::Rational(r)),
            Err(_) => Err(DError::new(format!("{} is not a valid rational", value))),
        }
    }

    /// Tokenizes `expression`, returning an empty vector for blank input.
    pub fn get_tokens(&self, expression: &str) -> DResult<Vec<Token<T>>> {
        let expression = strip_whitespace(expression);
        if expression.is_empty() {
            return Ok(Vec::new());
        }
        let bytes = expression.as_bytes();
        let mut tokens = Vec::with_capacity(bytes.len());
        let mut i = 0;
        while i < bytes.len() {
            let c = bytes[i];
            // "//" (truncating integer division) needs one character of
            // lookahead, so it can't be a plain single-character match arm
            // like the rest of the structural tokens below.
            if c == b'/' {
                if i + 1 < bytes.len() && bytes[i + 1] == b'/' {
                    tokens.push(Token::IntegerDivide);
                    i += 2;
                } else {
                    tokens.push(Token::Divide);
                    i += 1;
                }
                continue;
            }
            let structural = match c {
                b'(' => Some(Token::LeftParen),
                b')' => Some(Token::RightParen),
                b'[' => Some(Token::LeftFloor),
                b']' => Some(Token::RightFloor),
                b'|' => Some(Token::AbsoluteBar),
                b'+' => Some(Token::Plus),
                b'-' => Some(Token::Minus),
                b'*' => Some(Token::Multiply),
                b'%' => Some(Token::Modulo),
                b'^' => Some(Token::Power),
                _ => None,
            };
            if let Some(tok) = structural {
                tokens.push(tok);
                i += 1;
                continue;
            }
            if is_number_char(c) {
                let start = i;
                while i < bytes.len() && is_number_char(bytes[i]) {
                    i += 1;
                }
                let value = &expression[start..i];
                tokens.push(self.parse_operand(value)?);
                continue;
            }
            return Err(DError::new(format!("illegal character {} in expression: {}", c as char, expression)));
        }
        Ok(tokens)
    }
}
