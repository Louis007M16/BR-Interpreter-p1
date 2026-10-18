//Signals.java  -- abort and proceed are not errors, so they do NOT extend RuntimeError.
// super(null, null, false, false) turns off the stack trace: faster, and nothing noisy can print.

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

// extract travels up to the operation call that is running, carrying the value
class ReturnSignal extends RuntimeException {
    final Object value;
 
    ReturnSignal(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}