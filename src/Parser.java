// Parser
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;


class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    boolean hadError = false;

    private int loopDepth = 0;
    private int switchDepth = 0;

    private boolean sawMission = false;
    private final Set<String> operationNames = new HashSet<>();



    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }



    // Statements

    List<Stmt> parse() {
        List<Stmt> declarations = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                declarations.add(topLevel());
            } catch (ParseError e) {
                synchronizeTopLevel();
            }
        }
        if (!sawMission && !hadError) {
            error(peek(), "Program needs a 'mission()' entry point.");
        }
        return declarations;
    }


    private Stmt topLevel() {
        if (match(TokenType.AGENT)) return agentStatement();          // a global variable
        if (match(TokenType.OPERATION)) return operationDeclaration();
        if (match(TokenType.MISSION)) return missionDeclaration();
        throw error(peek(), "Only 'agent', 'operation' and 'mission' declarations are allowed at the top level.");
    }


    // After a top-level error: skip to the next top-level keyword, but only OUTSIDE any { } pair
    private void synchronizeTopLevel() {
        int depth = 0;
        do {
            Token t = advance();
            if (t.type == TokenType.LBRACES) depth++;
            else if (t.type == TokenType.RBRACES && depth > 0) depth--;
        } while (!isAtEnd() && !(depth == 0 && (check(TokenType.AGENT)
                || check(TokenType.OPERATION) || check(TokenType.MISSION))));
    }


    // operation name ( agent a, agent b ) { body }
    private Stmt operationDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect operation name.");
        if (!operationNames.add(name.lexeme)) {
            error(name, "Operation '" + name.lexeme + "' is already declared.");     // reported, not thrown
        }
 
        consume(TokenType.LPAREN, "Expect '(' after operation name.");
        List<Token> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                consume(TokenType.AGENT, "Expect 'agent' before parameter name.");
                Token param = consume(TokenType.IDENTIFIER, "Expect parameter name.");
                for (Token earlier : params) {
                    if (earlier.lexeme.equals(param.lexeme)) {
                        error(param, "Duplicate parameter '" + param.lexeme + "'.");
                    }
                }
                params.add(param);
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expect ')' after parameters.");
        consume(TokenType.LBRACES, "Expect '{' before operation body.");
        Stmt.Block body = (Stmt.Block) blockStatement();
        return new Stmt.Function(name, params, body);
    }
 

    // mission ( ) { body }
    private Stmt missionDeclaration() {
        Token keyword = previous();
        if (sawMission) error(keyword, "Only one 'mission()' is allowed.");           // reported, not thrown
        sawMission = true;
 
        consume(TokenType.LPAREN, "Expect '(' after 'mission'.");
        consume(TokenType.RPAREN, "Expect ')' after 'mission('.");
        consume(TokenType.LBRACES, "Expect '{' before mission body.");
        Stmt.Block body = (Stmt.Block) blockStatement();
        return new Stmt.Mission(keyword, body);
    }
 

    // extract ;     extract expression ;
    private Stmt extractStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMI_COLON)) value = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' after 'extract'.");
        return new Stmt.Return(keyword, value);
    }
 

    // greet(1, 2);      (the name was already consumed)
    private Stmt callStatement() {
        Token name = previous();
        consume(TokenType.LPAREN, "Expect '(' after operation name.");
        Expr call = finishCall(name);
        consume(TokenType.SEMI_COLON, "Expect ';' after operation call.");
        return new Stmt.Expression(call);
    }
 

    // the part of a call after "name(" :  arguments ")"
    private Expr finishCall(Token name) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expect ')' after arguments.");
        return new Expr.Call(name, arguments);
    }


    // statement to Statement (verify... get added later)
    private Stmt statements() {
        if (match(TokenType.INTEL)) return intelStatement();
        if (match(TokenType.AGENT)) return agentStatement();
        if (match(TokenType.IDENTIFIER)) {
            if (check(TokenType.LPAREN)) return callStatement();
            return assignmentCase();
        };
        if (match(TokenType.EXTRACT)) return extractStatement();
        if (match(TokenType.VERIFY)) return verifyStatement();
        if (match(TokenType.LBRACES)) return blockStatement();
        if (match(TokenType.INFILTRATE)) return infiltrateStatement();
        if (match(TokenType.PENETRATE)) return penetrateStatement();
        if (match(TokenType.ABORT)) return abortStatement();
        if (match(TokenType.PROCEED)) return proceedStatement();
        if (match(TokenType.PROTOCOL)) return protocolStatement();
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
        Stmt stmt = assignmentCore();
        consume(TokenType.SEMI_COLON, "Expect ';' after assignment.");
        return stmt;
    }

    private Stmt.Assign assignmentCore() {
        Token name = previous();

        if(match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token op = previous();
            TokenType kind = (op.type == TokenType.PLUS_PLUS) ? TokenType.PLUS : TokenType.MINUS;
            Token arithmetic = new Token(kind, op.lexeme.substring(0, 1), null, op.line, op.column);
            Expr value = new Expr.Binary(new Expr.Variable(name), arithmetic, new Expr.Literal(1));
            return new Stmt.Assign(name, value);
        }

        consume(TokenType.EQUAL, "Expect '=' after variable name.");
        Expr value = expression();
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
        if (!check(TokenType.RBRACES)) advance(); // always move forward at least one token, or we could loop forever
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMI_COLON) return;
            switch (peek().type) {
                case AGENT: case INTEL:
                case VERIFY: case INFILTRATE: case PENETRATE: case PROTOCOL:
                case ABORT: case PROCEED: case EXTRACT: case OPERATION: case MISSION:
                case RBRACES:
                case RULE: case DEFAULT:
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
            Token name = previous();
            if (match(TokenType.LPAREN)) return finishCall(name);
            return new Expr.Variable(name);
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
            String kind = name.lexeme;

            if (kind.equals("in") || kind.equals("int") || kind.equals("num")) {
                consume(TokenType.LPAREN, "Expect '(' after 'intel.in'.");
                consume(TokenType.RPAREN, "Expect ')' after 'intel.in('.");
                return new Expr.Input(keyword, kind);
            }
            if (kind.equals("log")) {
                throw error(name, "'intel.log' is a statement and has no value.");
            }
            throw error(name, "Unknown intel function '" + kind + "'.");
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


    // loops

    // infiltrate (condition) body
    private Stmt infiltrateStatement() {
        Token keyword = previous();
        consume(TokenType.LPAREN, "Expect '(' after 'infiltrate'.");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition.");
        Stmt body = loopBody();
        return new Stmt.While(keyword, condition, body);
    }


    // penetrate ( initializer ; condition ; increment ) body    (each part may be empty)
    private Stmt penetrateStatement() {
        Token keyword = previous();
        consume(TokenType.LPAREN, "Expect '(' after 'penetrate'.");
 
        Stmt initializer;
        if (match(TokenType.SEMI_COLON)) {
            initializer = null;                          // empty
        } else if (match(TokenType.AGENT)) {
            initializer = agentStatement();              // eats its own ';'
        } else if (match(TokenType.IDENTIFIER)) {
            initializer = assignmentCase();              // eats its own ';'
        } else {
            throw error(peek(), "Expect loop initializer.");
        }
 
        Expr condition = null;
        if (!check(TokenType.SEMI_COLON)) condition = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' after loop condition.");
 
        Stmt increment = null;
        if (!check(TokenType.RPAREN)) {
            consume(TokenType.IDENTIFIER, "Expect variable name in the loop increment.");
            increment = assignmentCore();                // no ';' here, the header ends with ')'
        }
        consume(TokenType.RPAREN, "Expect ')' after for clauses.");
 
        Stmt body = loopBody();
        return new Stmt.For(keyword, initializer, condition, increment, body);
    }


    // a loop body: counts as "inside a loop" while it is being parsed
    private Stmt loopBody() {
        loopDepth++;
        try {
            return body();
        } finally {
            loopDepth--;        // restored even if the body throws a ParseError
        }
    }


    private Stmt abortStatement() {
        Token keyword = previous();
        if (loopDepth == 0 && switchDepth == 0) 
        throw error(keyword, "'abort' can only be used inside a loop or 'protocol'.");
        consume(TokenType.SEMI_COLON, "Expect ';' after 'abort'.");
        return new Stmt.Abort(keyword);
    }
 

    private Stmt proceedStatement() {
        Token keyword = previous();
        if (loopDepth == 0) throw error(keyword, "'proceed' can only be used inside a loop.");
        consume(TokenType.SEMI_COLON, "Expect ';' after 'proceed'.");
        return new Stmt.Proceed(keyword);
    }


    // Protocol / rule / default

    // protocol ( expression ) { ( rule literal : statements | default : statements )* }
    private Stmt protocolStatement() {
        Token keyword = previous();
        consume(TokenType.LPAREN, "Expect '(' after 'protocol'.");
        Expr subject = expression();
        consume(TokenType.RPAREN, "Expect ')' after protocol value.");
        consume(TokenType.LBRACES, "Expect '{' before protocol body.");
 
        List<Stmt.Section> sections = new ArrayList<>();
        List<Object> seen = new ArrayList<>();      // rule values already used, to catch duplicates
        boolean sawDefault = false;
 
        switchDepth++;
        try {
            while (!check(TokenType.RBRACES) && !isAtEnd()) {
                Token label = peek();
                try {
                    if (match(TokenType.RULE)) {
                        Object value = ruleValue();
                        consume(TokenType.COLON, "Expect ':' after rule value.");
 
                        // reported but not thrown, so parsing carries on and one mistake = one message
                        if (sawDefault) error(label, "'default' must be the last label in a protocol.");
                        Object key = value;
                        if (value instanceof Number num) key = num.doubleValue();   // 1 and 1.0 are the same rule
                        if (seen.contains(key)) error(label, "Duplicate rule value.");
                        seen.add(key);
 
                        sections.add(new Stmt.Section(label, value, false, sectionBody()));
 
                    } else if (match(TokenType.DEFAULT)) {
                        consume(TokenType.COLON, "Expect ':' after 'default'.");
                        if (sawDefault) error(label, "Only one 'default' is allowed in a protocol.");
                        sawDefault = true;
                        sections.add(new Stmt.Section(label, null, true, sectionBody()));
 
                    } else {
                        // statements before the first label: report it, then read and drop them
                        error(peek(), "Expect 'rule' or 'default' inside a protocol.");
                        sectionBody();
                    }
                } catch (ParseError e) {
                    // a broken label: skip to the next label or the closing brace
                    while (!isAtEnd() && !check(TokenType.RULE)
                            && !check(TokenType.DEFAULT) && !check(TokenType.RBRACES)) {
                        advance();
                    }
                }
            }
        } finally {
            switchDepth--;
        }
 
        consume(TokenType.RBRACES, "Expect '}' after protocol body.");
        return new Stmt.Protocol(keyword, subject, sections);
    }
 

    // the value after "rule": a literal only (number, negative number, string, affirmative, denied)
    private Object ruleValue() {
        if (match(TokenType.AFFIRMATIVE)) return true;
        if (match(TokenType.DENIED)) return false;
        if (match(TokenType.STRING)) return previous().literal;
 
        boolean negative = match(TokenType.MINUS);
        if (match(TokenType.NUMBER)) {
            Object number = previous().literal;
            if (!negative) return number;
            if (number instanceof Integer i) return -i;
            return -(Double) number;
        }
        throw error(peek(), "Expect a literal value after 'rule'.");
    }
 
 
    // statements after a label, up to the next rule / default / closing brace.
    // One scope covers the whole protocol, so 'agent' is allowed here (not a one-line body).
    private List<Stmt> sectionBody() {
        List<Stmt> body = new ArrayList<>();
        while (!check(TokenType.RULE) && !check(TokenType.DEFAULT)
                && !check(TokenType.RBRACES) && !isAtEnd()) {
            try {
                body.add(statements());
            } catch (ParseError e) {
                synchronize();
            }
        }
        return body;
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
        System.err.println("[line " + token.line + ", col " + token.column + "] Error " + where + ": " + message);
        return new ParseError();
    }

}