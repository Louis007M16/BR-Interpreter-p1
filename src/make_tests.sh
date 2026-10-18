#!/bin/bash
# usage (from src):   bash make_tests.sh          creates ../tests/s7_*.ss and s7_*.out
DIR="${1:-../tests}"
mkdir -p "$DIR"
mk() { cat > "$DIR/$1.ss"; printf '%s' "$2" > "$DIR/$1.out"; }   # mk name "expected" << program

# ---------------- mission and top level ----------------
mk s7_mission_runs "hello" << 'EOF'
mission() { intel.log("hello"); }
EOF

mk s7_mission_extract "a" << 'EOF'
mission() {
    intel.log("a");
    extract;
    intel.log("b");
}
EOF

mk s7_global_agent "10" << 'EOF'
agent g = 10;
mission() { intel.log(g); }
EOF

mk s7_global_order "2" << 'EOF'
agent a = 1;
agent b = a + 1;
mission() { intel.log(b); }
EOF

mk s7_p_no_mission "[line 2] Error at end: Program needs a 'mission()' entry point." << 'EOF'
agent x = 1;
EOF

mk s7_p_two_missions "[line 2] Error at 'mission': Only one 'mission()' is allowed." << 'EOF'
mission() { }
mission() { }
EOF

mk s7_p_top_level_statement "[line 1] Error at 'intel': Only 'agent', 'operation' and 'mission' declarations are allowed at the top level." << 'EOF'
intel.log("x");
mission() { }
EOF

# ---------------- operations ----------------
mk s7_add "5" << 'EOF'
operation add(agent a, agent b) { extract a + b; }
mission() { intel.log(add(2, 3)); }
EOF

mk s7_call_before_definition "8" << 'EOF'
mission() { intel.log(twice(4)); }
operation twice(agent n) { extract n * 2; }
EOF

mk s7_no_params "hi" << 'EOF'
operation hello() { intel.log("hi"); }
mission() { hello(); }
EOF

mk s7_nested_calls "10" << 'EOF'
operation add(agent a, agent b) { extract a + b; }
mission() { intel.log(add(add(1, 2), add(3, 4))); }
EOF

mk s7_args_use_callers_scope "8" << 'EOF'
operation show(agent v) { intel.log(v); }
mission() {
    agent q = 7;
    show(q + 1);
}
EOF

mk s7_string_operation "Hello, Ana!" << 'EOF'
operation greet(agent name) { extract "Hello, " + name + "!"; }
mission() { intel.log(greet("Ana")); }
EOF

mk s7_bool_operation $'affirmative\ndenied' << 'EOF'
operation isEven(agent n) { extract n % 2 == 0; }
mission() {
    intel.log(isEven(4));
    intel.log(isEven(5));
}
EOF

mk s7_globals_readable_and_writable "3" << 'EOF'
agent counter = 0;
operation bump() { counter = counter + 1; }
mission() {
    bump();
    bump();
    bump();
    intel.log(counter);
}
EOF

mk s7_param_shadows_global $'2\n100' << 'EOF'
agent n = 100;
operation f(agent n) {
    n = n + 1;
    extract n;
}
mission() {
    intel.log(f(1));
    intel.log(n);
}
EOF

mk s7_locals_are_private "[line 1] Runtime error: Undefined variable 'secret'." << 'EOF'
operation peek() { intel.log(secret); }
mission() { agent secret = 1; peek(); }
EOF

mk s7_param_redeclared "[line 1] Runtime error: Variable 'a' is already declared." << 'EOF'
operation f(agent a) { agent a = 1; }
mission() { f(1); }
EOF

# ---------------- extract ----------------
mk s7_factorial $'120\n3628800' << 'EOF'
operation fact(agent n) {
    verify (n <= 1) { extract 1; }
    extract n * fact(n - 1);
}
mission() {
    intel.log(fact(5));
    intel.log(fact(10));
}
EOF

mk s7_fibonacci "55" << 'EOF'
operation fib(agent n) {
    verify (n < 2) { extract n; }
    extract fib(n - 1) + fib(n - 2);
}
mission() { intel.log(fib(10)); }
EOF

mk s7_extract_from_loop $'8\n-1' << 'EOF'
operation firstOver(agent limit) {
    penetrate (agent i = 1; i < 100; i++) {
        verify (i * i > limit) { extract i; }
    }
    extract -1;
}
mission() {
    intel.log(firstOver(50));
    intel.log(firstOver(100000));
}
EOF

mk s7_extract_from_protocol $'one\ntwo\nmany' << 'EOF'
operation find(agent x) {
    protocol (x) {
        rule 1: extract "one";
        rule 2: extract "two";
        default: extract "many";
    }
}
mission() {
    intel.log(find(1));
    intel.log(find(2));
    intel.log(find(9));
}
EOF

mk s7_scope_restored_after_extract $'2\n99' << 'EOF'
operation f() {
    agent local = 1;
    infiltrate (affirmative) {
        agent inner = 2;
        extract inner;
    }
}
mission() {
    agent local = 99;
    intel.log(f());
    intel.log(local);
}
EOF

mk s7_deep_recursion "4501500" << 'EOF'
operation sum(agent n) {
    verify (n == 0) { extract 0; }
    extract n + sum(n - 1);
}
mission() { intel.log(sum(3000)); }
EOF

mk s7_input_in_operation "Hi Ana" << 'EOF'
operation ask() { extract intel.in(); }
mission() { intel.log("Hi " + ask()); }
EOF
printf 'Ana\n' > "$DIR/s7_input_in_operation.in"

# ---------------- runtime errors ----------------
mk s7_e_void_value "[line 2] Runtime error: Operation 'nothing' did not return a value." << 'EOF'
operation nothing() { }
mission() { agent x = nothing(); }
EOF

mk s7_e_undefined_operation "[line 1] Runtime error: Undefined operation 'nope'." << 'EOF'
mission() { nope(); }
EOF

mk s7_e_too_few_arguments "[line 2] Runtime error: Operation 'add' expects 2 arguments but got 1." << 'EOF'
operation add(agent a, agent b) { extract a + b; }
mission() { add(1); }
EOF

mk s7_e_too_many_arguments "[line 2] Runtime error: Operation 'add' expects 2 arguments but got 3." << 'EOF'
operation add(agent a, agent b) { extract a + b; }
mission() { add(1, 2, 3); }
EOF

mk s7_e_one_argument_wording "[line 2] Runtime error: Operation 'f' expects 1 argument but got 0." << 'EOF'
operation f(agent a) { }
mission() { f(); }
EOF

mk s7_e_infinite_recursion "[line 1] Runtime error: Stack overflow: too many nested operation calls." << 'EOF'
operation f() { f(); }
mission() { f(); }
EOF

# ---------------- parse errors ----------------
mk s7_p_param_needs_agent "[line 1] Error at 'a': Expect 'agent' before parameter name." << 'EOF'
operation f(a) { }
mission() { }
EOF

mk s7_p_param_name_missing "[line 1] Error at ')': Expect parameter name." << 'EOF'
operation f(agent) { }
mission() { }
EOF

mk s7_p_duplicate_param "[line 1] Error at 'a': Duplicate parameter 'a'." << 'EOF'
operation f(agent a, agent a) { }
mission() { }
EOF

mk s7_p_duplicate_operation "[line 2] Error at 'f': Operation 'f' is already declared." << 'EOF'
operation f() { }
operation f() { }
mission() { }
EOF

mk s7_p_call_missing_paren "[line 1] Error at ';': Expect ')' after arguments." << 'EOF'
mission() { agent x = add(1, 2; }
EOF