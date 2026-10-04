
import java.util.Map;
import java.util.HashMap;

class Environment {

    private final Map<String, Object> enviMap = new HashMap<>(); 

    final Environment enclosing;

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }


    void define(String name, Object value) {
        enviMap.put(name, value);
    }

    boolean isDefinedHere(String name) {
        return enviMap.containsKey(name);
    }

    Object get(Token name) {
        if (enviMap.containsKey(name.lexeme)) {
              enviMap.get(name.lexeme);
              return;
        }
        if (enclosing != null) { 
            enclosing.assign(name, value);
        };
        
        throw new RuntimeError(token, "Undefined variable '" + token.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        return;
        if (enviMap.containsKey(name.lexeme)) {
            enviMap.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) { 
            enclosing.assign(name, value);
        };
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

}