/**
 * to run, copy together
 * javac *.java
java Main
 */
public class Main {
    public static void main(String[] args) {
       String source =
            "spawn x = 10.5 npc# a comment\n" +
            "alpha x > 5\n" +
            "  flex(\"big\")\n" +
            "slay\n";

        System.out.println("Source code: " + source);

        Lexer lexer = new Lexer(source);
        for (Token token : lexer.scanTokens()) {
            System.out.println(token);
        }
    }
}