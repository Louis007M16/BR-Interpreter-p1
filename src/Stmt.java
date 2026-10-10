import java.util.List;

interface Stmt {
    record Agent(Token name, Expr initializer) implements Stmt {}
    record Assign(Token name, Expr value) implements Stmt {}
    record Log(Token keyword, List<Expr> args) implements Stmt {}
} 