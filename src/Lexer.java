//Lexer.java

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Lexer {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    boolean hadError = false;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("agent", TokenType.AGENT);
        keywords.put("intel", TokenType.INTEL);     
        keywords.put("verify", TokenType.VERIFY);
        keywords.put("alternative", TokenType.ALTERNATIVE);
        keywords.put("otherwise", TokenType.OTHERWISE);
        keywords.put("infiltrate", TokenType.INFILTRATE);
        keywords.put("penetrate", TokenType.PENETRATE);
        keywords.put("protocol", TokenType.PROTOCOL);
        keywords.put("rule", TokenType.RULE);
        keywords.put("default", TokenType.DEFAULT);
        keywords.put("abort", TokenType.ABORT);
        keywords.put("proceed", TokenType.PROCEED);
        keywords.put("affirmative", TokenType.AFFIRMATIVE);
        keywords.put("denied", TokenType.DENIED);
        keywords.put("sync", TokenType.AND);
        keywords.put("alt", TokenType.OR);
        keywords.put("neg", TokenType.NOT);
        keywords.put("mission", TokenType.MISSION);
        keywords.put("operation", TokenType.OPERATION);
        keywords.put("extract", TokenType.EXTRACT);
    }

    Lexer(String source) {
        this.source = source;
    }

    // scan tokens
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // one character token
            case '+': addToken(match('+') ? TokenType.PLUS_PLUS : TokenType.PLUS); break;
            case '-':
                if (source.startsWith(".-.", current)) {
                    // "-.-." is a comment: skip to the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('-')) {
                    addToken(TokenType.MINUS_MINUS);
                } else {
                    addToken(TokenType.MINUS);
                }
                break;

            case '&':
                if (match('&')) addToken(TokenType.AND);
                else error(line, "Unexpected character '&'. Did you mean '&&'?");
                break;

            case '|':
                if (match('|')) addToken(TokenType.OR);
                else error(line, "Unexpected character '|'. Did you mean '||'?");
                break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '/': addToken(TokenType.SLASH); break;
            case '{': addToken(TokenType.LBRACES); break;
            case '}': addToken(TokenType.RBRACES); break;
            case ',': addToken(TokenType.COMMA); break;
            case ';': addToken(TokenType.SEMI_COLON); break;
            case ':': addToken(TokenType.COLON); break;
            case '.': addToken(TokenType.DOT); break;
            // two caharacter token
            case '!': 
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.NOT);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // whitespacce 
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n'://newline, skip and count
                line++; 
                break;

            // String start
            case '"':
                string();
                break;
            
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    error(line, "Unexpected character");
                }
                break;
        }
    }

    // identifier
    private void identifier() {
        while (isAlphanumeric(peek())) advance();
        String text = source.substring(start, current);
        //keyword type
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    //number
    private void number() {
        while (isDigit(peek())) advance();

        boolean isDecimal = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isDecimal = true;
            advance(); //kunin ang '.'
            while (isDigit(peek())) advance();
        }

        String text = source.substring(start, current);
        if (isDecimal) {
            addToken(TokenType.NUMBER, Double.parseDouble(text));
        } else {
            try {
                addToken(TokenType.NUMBER, Integer.parseInt(text));
            } catch (NumberFormatException e) {
                error(line, "Number is too large: " + text);
            }
        }
    }

    //string
    private void string() {
        StringBuilder value = new StringBuilder();
        while (peek() != '"' && !isAtEnd()) {
            char c = advance();
            if (c == '\n') line++;
            if (c == '\\' && !isAtEnd()) {
                char next = advance();
                switch (next) {
                    case 'n':  value.append('\n'); break;
                    case 't':  value.append('\t'); break;
                    case '"':  value.append('"');  break;
                    case '\\': value.append('\\'); break;
                    default:
                        error(line, "Unknown escape sequence '\\" + next + "'.");
                }
            } else {
                value.append(c);
            }
        }

        if (isAtEnd()) {
            error(line, "Unterminated string.");
            return;
        }

        // closing ".
        advance();

        addToken(TokenType.STRING, value.toString());
    }


    //match
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // helper functions
    // isalpha
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            c == '_';
    }

    //isdigit
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // isalphanumeric
    private boolean isAlphanumeric(char c) {
        return isAlpha(c) || isDigit(c); 
    }

    // isatend
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //advance
    private char advance() {
        return source.charAt(current++);
    }

    //peek
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    //peeknext
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    //error
    private void error(int line, String message) {
        hadError = true;
        System.err.println("[line " + line + "] " + message);
    }


    // add token
    private void addToken(TokenType type) {
        addToken(type, null);
    } 

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }


}

