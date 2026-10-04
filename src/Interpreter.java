//Interpreter.java  -- walks the tree and does the work

import java.util.List;

class Interpreter {

    boolean hadRuntimeError = false;

    // run a whole program; stop at the first runtime error
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            System.err.println("[line " + error.token.line + "] Runtime error: " + error.getMessage());
            hadRuntimeError = true;
        }
    }



    // statements

    private void execute(Stmt stmt) {
        if (stmt instanceof Stmt.Flex e) {
            Object value = evaluate(e.expression());
            System.out.print(stringify(value)); 
        } else {
            throw new IllegalStateException("Unkown Statement:" + stmt);
        }
    }



    // Expressions 

    private Object evaluate(Expr expr) {
        if (expr instanceof Expr.Literal e) return e.value();
        if (expr instanceof Expr.Grouping e) return evaluate(e.expression());
        if (expr instanceof Expr.Unary e) return EvalUnary(e);
        if (expr instanceof Expr.Binary e) return EvalBinary(e);
        if (expr instanceof Expr.Logical e) return EvalLogical(e);
        if (expr instanceof Expr.Variable e){
            throw new RuntimeError(e.name(), "Variables are not implemented yet.");
        };
        if (expr instanceof Expr.Input e) {
            throw new RuntimeError(e.keyword(), "rizz() is not implemented yet.");  
        }
        throw new IllegalStateException("Unkown expression" + expr);
    }

    private Object EvalUnary(Expr.Unary e) {
        Object right = evaluate(e.right());
        Token op = e.operator();

        switch (op.type) {
            case MINUS:
                if (right instanceof Integer i) return -i;
                if (right instanceof Double d) return -d;
                throw new RuntimeError(op, "Operand must be a number.");
            case NOT:
            case BANG:
                return !requireBoolean(op, right);
            default:
                throw new IllegalStateException("Bad Unary operator: " + op.type);
        }
    }

    private Object EvalLogical(Expr.Logical e) {
        Token op = e.operator();
        boolean left = requireBoolean(op, evaluate(e.left()));

        // short-circuit: only evaluate the right side if the left didn't already decide it
        if (op.type == TokenType.OR && left) return true;
        if (op.type == TokenType.AND && !left) return false;

        return requireBoolean(op, evaluate(e.right()));
    }

    private Object EvalBinary(Expr.Binary e) {
        Object left = evaluate(e.left());
        Object right = evaluate(e.right());
        Token op = e.operator();

        // equality works on anything
        if (op.type == TokenType.EQUAL_EQUAL) return isEqual(left, right);
        if (op.type == TokenType.BANG_EQUAL) return !isEqual(left, right);
        
        // "+" joins text if either side is a string
        if (op.type == TokenType.PLUS && (left instanceof String || right instanceof String)) {
            return stringify(left) + stringify(right);
        }

        // everything else needs two numbers
        if (!(left instanceof Number) || !(right instanceof Number)) {
            throw new RuntimeError(op, "Operands must be a Number");
        }

        // int op int stays an int, anything involving a decimal becomes a decimal
        if (left instanceof Integer a && right instanceof Integer b) {
            return intMath(op, a, b);
        }

        return doubleMath(op, ((Number) left).doubleValue(), ((Number) right).doubleValue());
    }

    private Object intMath(Token op, int a, int b) { 
        switch(op.type) {
            case PLUS:   return a + b;
            case MINUS:  return a - b;
            case STAR:   return a * b;
            case SLASH: 
                if (b == 0) throw new  RuntimeError(op, "Division by zero.");
                return a / b;
            case PERCENT:
                if (b == 0) throw new RuntimeError(op, "Modulo by zero.");
                return a % b;
            case LESS: return a < b;
            case LESS_EQUAL: return a <= b;
            case GREATER:   return a > b;
            case GREATER_EQUAL: return a >= b;
            default:
                throw new IllegalStateException("Bad binary operator: " + op.type);
        }
    }

    private Object doubleMath(Token op, double a, double b) {
        switch (op.type) {
            case PLUS:  return a + b;
            case MINUS: return a - b;
            case STAR:  return a * b;
            case SLASH:
                if (b == 0) throw new RuntimeError(op, "Division by zero.");
                return a / b;
            case PERCENT:
                if (b == 0) throw new RuntimeError(op, "Modulo by zero.");
                return a % b;
            case LESS:          return a < b;
            case LESS_EQUAL:    return a <= b;
            case GREATER:       return a > b;
            case GREATER_EQUAL: return a >= b;
            default:
                throw new IllegalStateException("Bad binary operator: " + op.type);
        }
    }



    // helper functions

    private boolean requireBoolean(Token op, Object value) {
        if (value instanceof Boolean b) return b;
        throw new RuntimeError(op, "Operand must be a boolean.");
    }

    private boolean isEqual(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return x.doubleValue() == y.doubleValue(); //1 == 1.0 is true
        }
        return a.equals(b); // different types is false
    }

    // how a value is shown by flex() and string "+"
    private String stringify(Object value) {
        if (value instanceof Boolean b) return b ? "W" : "L";
        return String.valueOf(value);
    }

}