//AstPrinter.java  
// debug for parser

class AstPrinter {

    String print(Expr expr) {
        if (expr instanceof Expr.Literal e) {
            return String.valueOf(e.value());
        }
        if (expr instanceof Expr.Variable e) {
            return e.name().lexeme;
        }
        if (expr instanceof Expr.Grouping e) {
            return parenthesize("group", e.expression());
        }
        if (expr instanceof Expr.Unary e) {
            return parenthesize(e.operator().lexeme, e.right());
        }
        if (expr instanceof Expr.Binary e) {
            return parenthesize(e.operator().lexeme, e.left(), e.right());
        }
        if (expr instanceof Expr.Logical e) {
            return parenthesize(e.operator().lexeme, e.left(), e.right());
        }
        if (expr instanceof Expr.Input) {
            return "(rizz)";
        }
        throw new IllegalArgumentException("Unknown expression type: " + expr);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder("(").append(name);
        for (Expr e : exprs) {
            sb.append(" ").append(print(e));
        }
        return sb.append(")").toString();
    }
}