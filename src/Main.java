/**
 * to run, copy together
 * javac *.java
java Main
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
       if (args.length != 1) {
            System.err.println("Usage: java Main <file.br>");
            System.exit(64);
            return;
        }
 
        final String source;

        try {
            source = Files.readString(Paths.get(args[0]));
        } catch (IOException | SecurityException e) {
            System.err.println("Error: cannot read file '" + args[0] + "'.");
            System.exit(66);
            return;
        } catch (RuntimeException e) {
            System.err.println("Error: invalid file path.");
            System.exit(66);
            return;
        }

        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();

            if (lexer.hadError) {
                System.exit(65);
                return;
            }

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            if (parser.hadError) {
                System.exit(65);
                return;
            }

            Interpreter interpreter = new Interpreter();

            Thread worker = new Thread(
                null,
                () -> {
                    try {
                        interpreter.interpret(statements);
                    } catch (StackOverflowError e) {
                        System.err.println("Runtime error: maximum recursion depth exceeded.");
                        interpreter.hadRuntimeError = true;
                    } catch (RuntimeException e) {
                        System.err.println("Runtime error: program could not be completed.");
                        interpreter.hadRuntimeError = true;
                    }
                },
                "interpreter",
                256L * 1024 * 1024
            );

            worker.setUncaughtExceptionHandler((thread, error) -> {
                System.err.println("Runtime error: program terminated unexpectedly.");
                interpreter.hadRuntimeError = true;
            });

            worker.start();

            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Error: execution interrupted.");
                System.exit(70);
                return;
            }

            if (interpreter.hadRuntimeError) {
                System.exit(70);
            }
        } catch (StackOverflowError e) {
            System.err.println("Error: input is too deeply nested.");
            System.exit(65);
        } catch (RuntimeException e) {
            System.err.println("Error: program could not be processed.");
            System.exit(65);
        }
    }
}