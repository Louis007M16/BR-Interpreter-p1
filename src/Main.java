/**
 * to run, copy together
 * javac *.java
java Main
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
       if (args.length != 1) {
            System.err.println("Usage: java Main <file.br>");
            System.exit(64);
        }
 
        String source = Files.readString(Paths.get(args[0]));
 
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        if (lexer.hadError) System.exit(65);        // don't run a program that failed to lex
 
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (parser.hadError) System.exit(65);       // ...or one that failed to parse
 
        Interpreter interpreter = new Interpreter();
        interpreter.interpret(statements);
        if (interpreter.hadRuntimeError) System.exit(70);
    }
}