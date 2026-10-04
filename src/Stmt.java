
interface Stmt {
    record Flex(Expr expression) implements Stmt  {}
    record Spawn(Token name, Expr initializer) implements Stmt {}
    record Assign(Token name, Expr value) implements Stmt {}
} 