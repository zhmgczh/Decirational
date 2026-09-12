//! Forwarding macros that give `DecimalInteger`, `TightInteger`, and
//! `Rational<T>` the standard `std::ops` operators (`+ - * / % -`) and
//! `FromStr`, on top of the existing `plus`/`minus`/`multiply`/`divide_by`/
//! `modulo`/`negate` methods those types already have. Each macro emits the
//! four value/reference combinations (`T op T`, `T op &T`, `&T op T`,
//! `&T op &T`) so callers can write `a + b` without giving up the
//! reference-taking form (`&a + &b`) that avoids cloning an operand just to
//! keep using it afterward.
//!
//! `divide_by`/`modulo` are fallible (division by zero), but `Div`/`Rem`
//! aren't - so `forward_checked_binop!` panics on `Err`, exactly like Rust's
//! own `/` and `%` operators panic on integer division by zero (this is
//! already documented crate-wide behavior; see the panics called out in
//! lib.rs's doc comment and the README).

macro_rules! forward_binop {
    ($Trait:ident, $method:ident, $call:ident, $Type:ty $(, $gen:ident : $bound:path)?) => {
        impl<$($gen: $bound)?> std::ops::$Trait for $Type {
            type Output = $Type;
            fn $method(self, rhs: $Type) -> $Type { (&self).$call(&rhs) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<&$Type> for $Type {
            type Output = $Type;
            fn $method(self, rhs: &$Type) -> $Type { (&self).$call(rhs) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<$Type> for &$Type {
            type Output = $Type;
            fn $method(self, rhs: $Type) -> $Type { self.$call(&rhs) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<&$Type> for &$Type {
            type Output = $Type;
            fn $method(self, rhs: &$Type) -> $Type { self.$call(rhs) }
        }
    };
}

macro_rules! forward_checked_binop {
    ($Trait:ident, $method:ident, $call:ident, $Type:ty $(, $gen:ident : $bound:path)?) => {
        impl<$($gen: $bound)?> std::ops::$Trait for $Type {
            type Output = $Type;
            fn $method(self, rhs: $Type) -> $Type { (&self).$call(&rhs).unwrap_or_else(|e| panic!("{}", e)) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<&$Type> for $Type {
            type Output = $Type;
            fn $method(self, rhs: &$Type) -> $Type { (&self).$call(rhs).unwrap_or_else(|e| panic!("{}", e)) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<$Type> for &$Type {
            type Output = $Type;
            fn $method(self, rhs: $Type) -> $Type { self.$call(&rhs).unwrap_or_else(|e| panic!("{}", e)) }
        }
        impl<$($gen: $bound)?> std::ops::$Trait<&$Type> for &$Type {
            type Output = $Type;
            fn $method(self, rhs: &$Type) -> $Type { self.$call(rhs).unwrap_or_else(|e| panic!("{}", e)) }
        }
    };
}

macro_rules! forward_unop {
    ($Trait:ident, $method:ident, $call:ident, $Type:ty $(, $gen:ident : $bound:path)?) => {
        impl<$($gen: $bound)?> std::ops::$Trait for $Type {
            type Output = $Type;
            fn $method(self) -> $Type { (&self).$call() }
        }
        impl<$($gen: $bound)?> std::ops::$Trait for &$Type {
            type Output = $Type;
            fn $method(self) -> $Type { self.$call() }
        }
    };
}

pub(crate) use forward_binop;
pub(crate) use forward_checked_binop;
pub(crate) use forward_unop;
