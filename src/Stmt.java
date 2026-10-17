import java.util.List;

interface Stmt {
    record Agent(Token name, Expr initializer) implements Stmt {}
    record Assign(Token name, Expr value) implements Stmt {}
    record Log(Token keyword, List<Expr> args) implements Stmt {}
    record Block(List<Stmt> statements) implements Stmt {}
    record Verify(Token keyword, Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}
    record While(Token keyword, Expr condition, Stmt body) implements Stmt {}
    record For(Token keyword, Stmt initializer, Expr condition, Stmt increment, Stmt body) implements Stmt {}
    record Abort(Token keyword) implements Stmt {}
    record Proceed(Token keyword) implements Stmt {}
} 