// Parser
import java.util.List;

class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    boolean hadError = false;


    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    // Expr parseExpression() {
    //     try {
    //         return expression();
    //     } catch (ParseError e) {
    //         return null;
    //     }
    // }


    // Statements

    // program to statements* EOF
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd) {
            try {
                statements.add(statements());
            } catch (ParseError e) {
                synchronize();
            }
        }
        return statements;
    }


    // statement to flexStatement (spawn, assignment, alpha... get added later)
    private Stmt statements() {
        if (match(TokenType.FLEX)) return flexStatement();
        throw error(peek(), "Expect statement."); 
    }

    // flexStatement to "flex" "(" expression ")"
    private Stmt flexStatement() {
        consume(TokenType.LPAREN, "Except '(' after 'flex'. ");
        Expr value = expression();
        consume(TokenType.RPAREN, "Expect ')' after value.");
        return new Stmt.Flex(value);
    }

    // After an error: throw away tokens until one that can start a statement.
    private void synchronize() {
        advance(); // always move forward at least one token, or we could loop forever
        while (!isAtEnd) {
            switch (peek().type) {
                case SPAWN: case FLEX:
                case ALPHA: case GRIND: case YAP: case VIBE_CHECK:
                case SELLING: case BOUNCE:
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
        if (match(TokenType.NOT, TokenType.BANG)) {
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

    // Primary to NUMBER | STRING | W | L | IDENTIFIER | "rizz" "(" ")" | "(" expression ")
    private Expr primary() {
        if (match(TokenType.W)) return new Expr.Literal(true);
        if (match(TokenType.L)) return new Expr.Literal(false);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.RIZZ)) {
            Token keyword = previous();
            consume(TokenType.LPAREN, "Expect '(' after 'rizz'.");
            consume(TokenType.RPAREN, "Expect ')' after 'rizz('.");
            return new Expr.Input(keyword);
        }

        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
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