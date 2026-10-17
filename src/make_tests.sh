#!/bin/bash
# usage (from src):   bash make_tests.sh          creates ../tests/s6_*.ss and s6_*.out
DIR="${1:-../tests}"
mkdir -p "$DIR"
mk() { cat > "$DIR/$1.ss"; printf '%s' "$2" > "$DIR/$1.out"; }   # mk name "expected" << program

# Stage 5 test whose message changed: abort is now also allowed inside a protocol
printf '%s' "[line 1] Error at 'abort': 'abort' can only be used inside a loop or 'protocol'." > "$DIR/s5_p_abort_outside.out"

mk s6_match_second "two" << 'EOF'
agent x = 2;
protocol (x) {
    rule 1: intel.log("one"); abort;
    rule 2: intel.log("two"); abort;
    default: intel.log("other");
}
EOF

mk s6_default "other" << 'EOF'
agent x = 9;
protocol (x) {
    rule 1: intel.log("one"); abort;
    rule 2: intel.log("two"); abort;
    default: intel.log("other");
}
EOF

mk s6_fallthrough $'a\nb\nc' << 'EOF'
agent x = 1;
protocol (x) {
    rule 1: intel.log("a");
    rule 2: intel.log("b");
    rule 3: intel.log("c"); abort;
    rule 4: intel.log("d");
}
EOF

mk s6_fall_into_default $'b\nd' << 'EOF'
protocol (2) {
    rule 1: intel.log("a");
    rule 2: intel.log("b");
    default: intel.log("d");
}
EOF

mk s6_no_match_no_default "after" << 'EOF'
protocol (5) {
    rule 1: intel.log("one");
}
intel.log("after");
EOF

mk s6_empty "ok" << 'EOF'
protocol (1) { }
intel.log("ok");
EOF

mk s6_string "hi" << 'EOF'
protocol ("hi") {
    rule "hi": intel.log("hi"); abort;
    default: intel.log("other");
}
EOF

mk s6_bool "yes" << 'EOF'
protocol (affirmative) {
    rule affirmative: intel.log("yes"); abort;
    rule denied: intel.log("no");
}
EOF

mk s6_negative "neg" << 'EOF'
agent x = -1;
protocol (x) {
    rule -1: intel.log("neg");
}
EOF

mk s6_int_equals_decimal "two" << 'EOF'
protocol (2.0) {
    rule 2: intel.log("two");
}
EOF

mk s6_type_mismatch "none" << 'EOF'
protocol ("1") {
    rule 1: intel.log("num");
    default: intel.log("none");
}
EOF

mk s6_subject_expression "two" << 'EOF'
agent x = 1;
protocol (x + 1) {
    rule 2: intel.log("two");
}
EOF

mk s6_nested_statements "big" << 'EOF'
agent x = 5;
protocol (x) {
    rule 5:
        verify (x > 3) { intel.log("big"); }
        abort;
}
EOF

mk s6_abort_keeps_loop_going $'0\n2' << 'EOF'
penetrate (agent i = 0; i < 3; i++) {
    protocol (i) {
        rule 1: abort;
        default: intel.log(i);
    }
}
EOF

mk s6_proceed_continues_loop $'0\ntwo\n2\n3' << 'EOF'
penetrate (agent i = 0; i < 4; i++) {
    protocol (i) {
        rule 1: proceed;
        rule 2: intel.log("two"); abort;
    }
    intel.log(i);
}
EOF

mk s6_loop_abort_inside_protocol $'0\n1\nafter loop' << 'EOF'
protocol (1) {
    rule 1:
        penetrate (agent i = 0; i < 5; i++) {
            verify (i == 2) { abort; }
            intel.log(i);
        }
        intel.log("after loop");
}
EOF

mk s6_scope_ends $'5\n[line 2] Runtime error: Undefined variable \'t\'.' << 'EOF'
protocol (1) { rule 1: agent t = 5; intel.log(t); }
intel.log(t);
EOF

mk s6_e_undefined_subject "[line 1] Runtime error: Undefined variable 'zz'." << 'EOF'
protocol (zz) { }
EOF

mk s6_p_duplicate "[line 3] Error at 'rule': Duplicate rule value." << 'EOF'
protocol (1) {
    rule 1: intel.log("a");
    rule 1: intel.log("b");
}
EOF

mk s6_p_duplicate_1_and_1_0 "[line 3] Error at 'rule': Duplicate rule value." << 'EOF'
protocol (1) {
    rule 1: intel.log("a");
    rule 1.0: intel.log("b");
}
EOF

mk s6_p_default_not_last "[line 3] Error at 'rule': 'default' must be the last label in a protocol." << 'EOF'
protocol (1) {
    default: intel.log("d");
    rule 1: intel.log("a");
}
EOF

mk s6_p_statement_before_label "[line 1] Error at 'intel': Expect 'rule' or 'default' inside a protocol." << 'EOF'
protocol (1) { intel.log("x"); }
EOF

mk s6_p_non_literal "[line 2] Error at 'y': Expect a literal value after 'rule'." << 'EOF'
agent y = 1;
protocol (1) { rule y: intel.log("a"); }
EOF

mk s6_p_missing_colon "[line 1] Error at 'intel': Expect ':' after rule value." << 'EOF'
protocol (1) { rule 1 intel.log("a"); }
EOF

mk s6_p_missing_brace "[line 2] Error at end: Expect '}' after protocol body." << 'EOF'
protocol (1) { rule 1: intel.log("a");
EOF

mk s6_p_proceed_in_protocol_only "[line 1] Error at 'proceed': 'proceed' can only be used inside a loop." << 'EOF'
protocol (1) { rule 1: proceed; }
EOF

mk s6_p_two_defaults "[line 3] Error at 'default': Only one 'default' is allowed in a protocol." << 'EOF'
protocol (1) {
    default: intel.log("a");
    default: intel.log("b");
}
EOF