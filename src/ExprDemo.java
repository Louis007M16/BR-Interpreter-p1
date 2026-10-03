//ExprDemo.java  temporary to test. type an expression, see the tree
import java.util.List;
import java.util.Scanner;

public class ExprDemo {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Type an expression (Ctrl+D to quit):");
        while (in.hasNextLine()) {
            String line = in.nextLine();
            Lexer lexer = new Lexer(line);
            List<Token> tokens = lexer.scanTokens();
            if (lexer.hadError) continue;

            Parser parser = new Parser(tokens);
            Expr expr = parser.parseExpression();
            if (parser.hadError) continue;

            System.out.println(new AstPrinter().print(expr));
        }
    }
}