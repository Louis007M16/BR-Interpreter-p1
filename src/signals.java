//Signals.java  
// super(null, null, false, false) 

class BreakSignal extends RuntimeException {
    BreakSignal() {
        super(null, null, false, false);
    }
}

class ContinueSignal extends RuntimeException {
    ContinueSignal() {
        super(null, null, false, false);
    }
}

class ReturnSignal extends RuntimeException {
    final Object value;
 
    ReturnSignal(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}