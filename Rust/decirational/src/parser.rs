use crate::custom_integer::{CustomInteger, DError, DResult};
use crate::rational::Rational;
use crate::token::Token;

/// A recursive-descent evaluator over a token stream. Implements this grammar:
///
/// ```text
/// expression := term (('+'|'-') term)*
/// term       := unary (('*'|'/'|'%') unary)*
/// unary      := ('+'|'-')* power
/// power      := primary ('^' unary)?              // right-associative
/// primary    := operand | '(' expression ')' | '[' expression ']' (floor)
///             | '|' expression '|' (absolute value)
/// ```
// parse_unary is the one point every recursive production in this grammar
// passes through at least once per level of nesting: directly for a chain
// of unary +/- (parse_unary calling itself), for bracket/floor/absolute-
// value nesting (via the expression->term->unary->power->primary chain that
// runs once per level before the next '(', '[' or '|'), and for right-
// associative '^' chains (parse_power calling parse_unary for its exponent,
// which can lead straight back into parse_power). Rust's call stack has no
// bound of its own worth relying on - a deeply nested or chained expression
// overflows it with an uncatchable "fatal runtime error: stack overflow"
// (not a panic - catch_unwind cannot intercept it, so the whole process
// aborts) well before this limit, so this is checked well short of that:
// crafting a one-line expression this deep is trivial for an attacker, and
// no legitimate expression needs anywhere near it.
const MAX_EXPRESSION_DEPTH: u32 = 1000;

pub struct Parser<T: CustomInteger> {
    tokens: Vec<Token<T>>,
    pos: usize,
    depth: u32,
}

impl<T: CustomInteger> Default for Parser<T> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T: CustomInteger> Parser<T> {
    pub fn new() -> Self {
        Parser { tokens: Vec::new(), pos: 0, depth: 0 }
    }

    /// Evaluates `tokens` to a single Rational<T>.
    pub fn parse(&mut self, tokens: Vec<Token<T>>) -> DResult<Rational<T>> {
        if tokens.is_empty() {
            return Err(DError::new("no tokens to parse"));
        }
        self.tokens = tokens;
        self.pos = 0;
        self.depth = 0;
        let result = self.parse_expression()?;
        if self.pos != self.tokens.len() {
            return Err(DError::new(format!("unexpected token: {}", self.tokens[self.pos])));
        }
        Ok(result)
    }

    fn peek(&self) -> Option<&Token<T>> {
        self.tokens.get(self.pos)
    }

    fn check(&self, expected: &Token<T>) -> bool {
        self.peek() == Some(expected)
    }

    fn expect(&mut self, expected: Token<T>) -> DResult<()> {
        if !self.check(&expected) {
            return match self.peek() {
                None => Err(DError::new(format!("expected {} but found end of input", expected))),
                Some(found) => Err(DError::new(format!("expected {} but found {}", expected, found))),
            };
        }
        self.pos += 1;
        Ok(())
    }

    fn parse_expression(&mut self) -> DResult<Rational<T>> {
        let mut result = self.parse_term()?;
        loop {
            if self.check(&Token::Plus) {
                self.pos += 1;
                result = result.plus(&self.parse_term()?);
            } else if self.check(&Token::Minus) {
                self.pos += 1;
                result = result.minus(&self.parse_term()?);
            } else {
                break;
            }
        }
        Ok(result)
    }

    fn parse_term(&mut self) -> DResult<Rational<T>> {
        let mut result = self.parse_unary()?;
        loop {
            if self.check(&Token::Multiply) {
                self.pos += 1;
                result = result.multiply(&self.parse_unary()?);
            } else if self.check(&Token::Divide) {
                self.pos += 1;
                result = result.divide_by(&self.parse_unary()?)?;
            } else if self.check(&Token::IntegerDivide) {
                self.pos += 1;
                let right = self.parse_unary()?;
                result = integer_divide_rational(&result, &right)?;
            } else if self.check(&Token::Modulo) {
                self.pos += 1;
                let right = self.parse_unary()?;
                result = modulo_rational(&result, &right)?;
            } else {
                break;
            }
        }
        Ok(result)
    }

    fn parse_unary(&mut self) -> DResult<Rational<T>> {
        self.depth += 1;
        if self.depth > MAX_EXPRESSION_DEPTH {
            self.depth -= 1;
            return Err(DError::new(format!("expression nested too deeply (max depth {MAX_EXPRESSION_DEPTH})")));
        }
        let result = self.parse_unary_inner();
        self.depth -= 1;
        result
    }

    fn parse_unary_inner(&mut self) -> DResult<Rational<T>> {
        if self.check(&Token::Plus) {
            self.pos += 1;
            return self.parse_unary();
        }
        if self.check(&Token::Minus) {
            self.pos += 1;
            return Ok(self.parse_unary()?.negate());
        }
        self.parse_power()
    }

    fn parse_power(&mut self) -> DResult<Rational<T>> {
        let base = self.parse_primary()?;
        if self.check(&Token::Power) {
            self.pos += 1;
            let exponent = self.parse_unary()?;
            let e = to_int_exponent(&exponent)?;
            return base.pow(e);
        }
        Ok(base)
    }

    fn parse_primary(&mut self) -> DResult<Rational<T>> {
        let tok = match self.peek() {
            None => return Err(DError::new("unexpected end of input")),
            Some(t) => t.clone(),
        };
        match tok {
            Token::Integer(v) | Token::LargeInteger(v) => {
                self.pos += 1;
                Ok(Rational::from_integer(v))
            }
            Token::Rational(r) => {
                self.pos += 1;
                Ok(r)
            }
            Token::LeftParen => {
                self.pos += 1;
                let result = self.parse_expression()?;
                self.expect(Token::RightParen)?;
                Ok(result)
            }
            Token::LeftFloor => {
                self.pos += 1;
                let result = self.parse_expression()?;
                self.expect(Token::RightFloor)?;
                Ok(Rational::from_integer(floor_integer(&result)?))
            }
            Token::AbsoluteBar => {
                self.pos += 1;
                let result = self.parse_expression()?;
                self.expect(Token::AbsoluteBar)?;
                Ok(result.abs())
            }
            other => Err(DError::new(format!("unexpected token: {}", other))),
        }
    }
}

fn modulo_rational<T: CustomInteger>(a: &Rational<T>, b: &Rational<T>) -> DResult<Rational<T>> {
    if !a.is_integer() || !b.is_integer() {
        return Err(DError::new("modulo is only supported between integers"));
    }
    Ok(Rational::from_integer(a.numerator().modulo(b.numerator())?))
}

fn integer_divide_rational<T: CustomInteger>(a: &Rational<T>, b: &Rational<T>) -> DResult<Rational<T>> {
    if !a.is_integer() || !b.is_integer() {
        return Err(DError::new("integer division is only supported between integers"));
    }
    Ok(Rational::from_integer(a.numerator().divide_by(b.numerator())?))
}

fn to_int_exponent<T: CustomInteger>(exponent: &Rational<T>) -> DResult<i32> {
    if !exponent.is_integer() {
        return Err(DError::new(format!("exponent must be an integer: {}", exponent)));
    }
    exponent
        .numerator()
        .to_string()
        .parse::<i32>()
        .map_err(|_| DError::new(format!("exponent out of range: {}", exponent)))
}

fn floor_integer<T: CustomInteger>(r: &Rational<T>) -> DResult<T> {
    let numerator = r.numerator();
    if r.is_integer() {
        return Ok(numerator.clone());
    }
    let denominator = r.denominator();
    let (quotient, _) = numerator.divide_by_and_modulo(denominator)?;
    if numerator.is_negative() {
        return Ok(quotient.minus(&denominator.pow(0)?));
    }
    Ok(quotient)
}
