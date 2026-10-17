//Interpreter.java  -- walks the tree and does the work

import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class Interpreter {

    boolean hadRuntimeError = false;

    private Environment environment = new Environment(); //global

    // created ONCE, making a new reader per intel.in() call would lose buffered input
    private final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    // run a whole program; stop at the first runtime error
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            System.out.flush();
            System.err.println("[line " + error.token.line + "] Runtime error: " + error.getMessage());
            hadRuntimeError = true;
        }
    }



    // statements

    private void execute(Stmt stmt) {
        if (stmt instanceof Stmt.Log s) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < s.args().size(); i++) {
                if (i > 0) out.append(" ");
                out.append(stringify(evaluate(s.args().get(i))));
            }
            System.out.println(out);
            return;
        } else if (stmt instanceof Stmt.Agent s) {
            if (environment.isDefinedHere(s.name().lexeme)) {
                throw new RuntimeError(s.name(),  "Variable '" + s.name().lexeme + "' is already declared.");
            }
            Object value = evaluate(s.initializer());
            environment.define(s.name().lexeme, value);

        } else if (stmt instanceof Stmt.Assign s) {
            Object value = evaluate(s.value());
            environment.assign(s.name(), value);

        } else if (stmt instanceof Stmt.Block s) {
            executeBlock(s.statements(), new Environment(environment));

        } else if (stmt instanceof Stmt.Verify s) {
            Object condition = evaluate(s.condition());
            if (!(condition instanceof Boolean)) {
                throw new RuntimeError(s.keyword(), "Condition must be a boolean.");
            }

            if ((Boolean) condition) {
                execute(s.thenBranch());
            } else if (s.elseBranch() != null) {
                execute(s.elseBranch());
            }

        } else if (stmt instanceof Stmt.Abort s){
            throw new BreakSignal();

        } else if (stmt instanceof Stmt.Proceed s){
            throw new ContinueSignal();

        } else if (stmt instanceof Stmt.While s) {
            try {
                while (conditionTruth(s.keyword(), evaluate(s.condition()))) {
                    try {
                        execute(s.body());
                    } catch (ContinueSignal c) {
                        // proceed: just start the next pass
                    }
                }
            } catch (BreakSignal b) {
                // abort: leave the loop
            }
 
        } else if (stmt instanceof Stmt.For s) {
            Environment previous = this.environment;
            try {
                this.environment = new Environment(previous);       // the loop variable lives in here
                if (s.initializer() != null) execute(s.initializer());
 
                try {
                    // a missing condition means "always true"
                    while (s.condition() == null || conditionTruth(s.keyword(), evaluate(s.condition()))) {
                        try {
                            execute(s.body());
                        } catch (ContinueSignal c) {
                            // proceed: skip the rest of the body, but STILL run the increment
                        }
                        if (s.increment() != null) execute(s.increment());
                    }
                } catch (BreakSignal b) {
                    // abort: leave the loop
                }
            } finally {
                this.environment = previous;                        // loop scope disappears
            }
        } else if (stmt instanceof Stmt.Protocol s) {
            Object subject = evaluate(s.subject());                 // evaluated once
            List<Stmt.Section> sections = s.sections();
 
            // 1. where do we start? the first rule that matches...
            int start = -1;
            for (int n = 0; n < sections.size(); n++) {
                Stmt.Section section = sections.get(n);
                if (!section.isDefault() && isEqual(subject, section.value())) {
                    start = n;
                    break;
                }
            }
            // ...or, if none matches, the default (if there is one)
            if (start == -1) {
                for (int n = 0; n < sections.size(); n++) {
                    if (sections.get(n).isDefault()) {
                        start = n;
                        break;
                    }
                }
            }
 
            // 2. run from there. Without an abort it FALLS THROUGH into the next sections (like Java/C++)
            Environment previous = this.environment;
            try {
                this.environment = new Environment(previous);       // the whole protocol is one scope
                if (start != -1) {
                    try {
                        for (int n = start; n < sections.size(); n++) {
                            for (Stmt inner : sections.get(n).body()) {
                                execute(inner);
                            }
                        }
                    } catch (BreakSignal b) {
                        // abort: leave the protocol
                    }
                }
            } finally {
                this.environment = previous;
            }

        } else {
            throw new IllegalStateException("Unknown Statement:" + stmt);
        }
    }



    // Expressions 

    private Object evaluate(Expr expr) {
        if (expr instanceof Expr.Literal e) return e.value();
        if (expr instanceof Expr.Grouping e) return evaluate(e.expression());
        if (expr instanceof Expr.Unary e) return EvalUnary(e);
        if (expr instanceof Expr.Binary e) return EvalBinary(e);
        if (expr instanceof Expr.Logical e) return EvalLogical(e);
        if (expr instanceof Expr.Variable e) return environment.get(e.name());
        if (expr instanceof Expr.Input e) return readLine(e.keyword());
        throw new IllegalStateException("Unknown expression" + expr);
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
            throw new RuntimeError(op, "Operands must be a Number.");
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

    // execution block
    private void executeBlock (List<Stmt> statements, Environment scope) {
        Environment previous = this.environment;

        try {
            this.environment = scope;
            for (Stmt statement : statements) {
                execute(statement);
            } 
        } finally {
            this.environment = previous;
        }
    }

    // a loop condition must be a boolean
    private boolean conditionTruth(Token keyword, Object value) {
        if (value instanceof Boolean b) return b;
        throw new RuntimeError(keyword, "Condition must be a boolean.");
    }

    // the intel.in(), read one line from the keyboard (always a string)
    private String readLine(Token keyword) {
        try {
            String line = stdin.readLine();
            if (line == null) throw new RuntimeError(keyword, "No input available for intel.in().");
            return line;
        } catch (IOException ex) {
            throw new RuntimeError(keyword, "Could not read input.");
        }
    }

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

    // how a value is shown by intel.out() and string "+"
    private String stringify(Object value) {
        if (value instanceof Boolean b) return b ? "affirmative" : "denied";
        return String.valueOf(value);
    }

}