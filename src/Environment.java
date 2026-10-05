
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

    // define a variable in this scope
    void define(String name, Object value) {
        enviMap.put(name, value);
    }

    //questions is the name already declared in THIS scope (not the parents)?
    boolean isDefinedHere(String name) {
        return enviMap.containsKey(name);
    }

    // read a variable, this scope first, then the parents
    Object get(Token name) {
        if (enviMap.containsKey(name.lexeme)) {
            return enviMap.get(name.lexeme);
            
        }
        if (enclosing != null) { 
            return enclosing.get(name);
        };  
        
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    // change an existing variable,  this scope first, then the parents
    void assign(Token name, Object value) {
        if (enviMap.containsKey(name.lexeme)) {
            enviMap.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) { 
            enclosing.assign(name, value);
            return;
        };
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

}