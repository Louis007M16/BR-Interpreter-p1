// Parser
import java.util.List;
import java.util.ArrayList;


class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    boolean hadError = false;


    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError e) {
            return null;
        }
    }


    // Statements

    // program to statements* EOF
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(statements());
            } catch (ParseError e) {
                synchronize();
            }
        }
        return statements;
    }


    // statement to Statement (alpha... get added later)
    private Stmt statements() {
        if (match(TokenType.INTEL)) return intelStatement();
        if (match(TokenType.AGENT)) return agentStatement();
        if (match(TokenType.IDENTIFIER)) return assignmentCase();
        if (match(TokenType.VERIFY)) return verifyStatement();
        if (match(TokenType.LBRACES)) return blockStatement();
        throw error(peek(), "Expect statement.");  
    } 

    // intel . log ( args ) ;
    private Stmt intelStatement() {
        Token keyword = previous();
        consume(TokenType.DOT, "Expect '.' after 'intel'.");
        Token name = consume(TokenType.IDENTIFIER, "Expect function name after 'intel.'.");

        if (!name.lexeme.equals("log")) {
            throw error(name, "'intel." + name.lexeme + "' can't be used as a statement.");
        }

        consume(TokenType.LPAREN, "Expect '(' after 'intel.log'.");
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                args.add(expression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expect ')' after arguments.");
        consume(TokenType.SEMI_COLON, "Expect ';' after 'intel.log(...)'.");
        return new Stmt.Log(keyword, args);
    }

    //assignmentCase
    private Stmt assignmentCase() {
        Token name = previous();
        consume(TokenType.EQUAL, "Expect '=' after variable name.");
        Expr value = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' after assignment.");
        return new Stmt.Assign(name, value);
    }

    // agent IDENTIFIER = expression
    private Stmt agentStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        consume(TokenType.EQUAL, "Expect '=' after variable name.");
        Expr value = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' after variable declaration.");
        return new Stmt.Agent(name, value);
    }

    // After an error: throw away tokens until one that can start a statement.
    private void synchronize() {
        advance(); // always move forward at least one token, or we could loop forever
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMI_COLON) return;
            switch (peek().type) {
                case AGENT: case INTEL:
                case VERIFY: case INFILTRATE: case PENETRATE: case PROTOCOL:
                case ABORT: case PROCEED: case EXTRACT: case OPERATION: case MISSION:
                case RBRACES:
                    return;
                    default:
                        advance();
            }
        }
    }



    // grammar rules. start from loosest operator 

    // expression to or
    private Expr expression() {
        return or();
    }

    // or to and
    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // and to logicNot
    private Expr and() {
        Expr expr = logicNot();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = logicNot();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    //logicNot to "not" | !, the word | and for equal
    private Expr logicNot() {
        if (match(TokenType.NOT)) {
            Token operator = previous();
            Expr right = logicNot();
            return new Expr.Unary(operator, right);
        }
        return equality();
    }

    // equality to comparison "==" | "!="
    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right); 
        }
        return expr;
    }

    // comparison to term "<" | "<=" | ">" | ">="
    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, 
                    TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term to factor "+" | "-" 
    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // factor to unary "*" | "/" | "%"
    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // unary to "-" unary | primary. for negative
    private Expr unary() {
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    // Primary to NUMBER | STRING | true | false | IDENTIFIER | "intel" "(" ")" | "(" expression ")
    private Expr primary() {
        if (match(TokenType.AFFIRMATIVE)) return new Expr.Literal(true);
        if (match(TokenType.DENIED)) return new Expr.Literal(false);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(TokenType.INTEL)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'intel'.");
            Token name = consume(TokenType.IDENTIFIER, "Expect function name after 'intel.'.");

            if (name.lexeme.equals("in")) {
                consume(TokenType.LPAREN, "Expect '(' after 'intel.in'.");
                consume(TokenType.RPAREN, "Expect ')' after 'intel.in('.");
                return new Expr.Input(keyword);
            }
            if (name.lexeme.equals("log")) {
                throw error(name, "'intel.log' is a statement and has no value.");
            }
            throw error(name, "Unknown intel function '" + name.lexeme + "'.");
        }

        throw error(peek(), "Expect expression.");
    }


    // if statements
    
    private Stmt blockStatement() {
        List<Stmt> forBstatements = new ArrayList<>();
        while (!check(TokenType.RBRACES) && !isAtEnd()) {
            try {
                forBstatements.add(statements());
            } catch (ParseError e) {
                synchronize();
            }
        }
        consume(TokenType.RBRACES, "Expect '}' after block.");
        return new Stmt.Block(forBstatements);
    } 

    private Stmt verifyStatement() {
        return verifyChain(true);
    }

    private Stmt verifyChain(boolean first) {
        Token keyword = previous();             // 'verify' or 'alternative'
        consume(TokenType.LPAREN, "Expect '(' after '" + (first ? "verify" : "alternative") + "'.");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition.");

        boolean braced = check(TokenType.LBRACES);
        if (!first && !braced) {
            throw error(peek(), "Expect '{' before alternative body.");
        }
        Stmt thenBranch = body();

        Stmt elseBranch = null;
        if (check(TokenType.ALTERNATIVE) || check(TokenType.OTHERWISE)) {
            if (!braced) {
                throw error(peek(), "Use braces when 'verify' has 'alternative' or 'otherwise'.");
            }
            if (match(TokenType.ALTERNATIVE)) {
                elseBranch = verifyChain(false);
            } else {
                advance();                      // the 'otherwise'
                consume(TokenType.LBRACES, "Expect '{' before otherwise body.");
                elseBranch = blockStatement();
            }
        }
        return new Stmt.Verify(keyword, condition, thenBranch, elseBranch);
    }

    private Stmt body() { // for one line if 
        if (check(TokenType.AGENT)) throw error(peek(), "Declaration needs a block.");
        return statements();
    }

    // helper functions

    // if current token is types, eaet it  and return true
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // like the match he consume the token, but its an error if the token isnt there
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
 
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
 
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
 
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
 
    private Token peek() {
        return tokens.get(current);
    }
 
    private Token previous() {
        return tokens.get(current - 1);
    }
 
    private ParseError error(Token token, String message) {
        hadError = true;
        String where = (token.type == TokenType.EOF) ? "at end" : "at '" + token.lexeme + "'";
        System.err.println("[line " + token.line + "] Error " + where + ": " + message);
        return new ParseError();
    }

}