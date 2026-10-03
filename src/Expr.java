//Expr.java
//  every kind of expression the language has

interface Expr {
    record Literal(Object value) implements Expr {}                       // 10, 3.5, "hi", W, L
    record Variable(Token name) implements Expr {}                        // variable name
    record Grouping(Expr expression) implements Expr {}                   // ( ... )
    record Unary(Token operator, Expr right) implements Expr {}           // -x, not x
    record Binary(Expr left, Token operator, Expr right) implements Expr {}   // + - * / % == != < <= > >=
    record Logical(Expr left, Token operator, Expr right) implements Expr {}  // and, or (short-circuit)
    record Input(Token keyword) implements Expr {}                        // rizz()
}