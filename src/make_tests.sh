#!/bin/bash
# usage (from src):   bash make_tests.sh          creates ../tests/s5_*.ss and s5_*.out
DIR="${1:-../tests}"
mkdir -p "$DIR"
mk() { cat > "$DIR/$1.ss"; printf '%s' "$2" > "$DIR/$1.out"; }   # mk name "expected" << program

mk s5_while_count $'0\n1\n2' << 'EOF'
agent i = 0;
infiltrate (i < 3) {
    intel.log(i);
    i = i + 1;
}
EOF

mk s5_while_never "done" << 'EOF'
infiltrate (denied) { intel.log("x"); }
intel.log("done");
EOF

mk s5_while_oneline "3" << 'EOF'
agent i = 0;
infiltrate (i < 3) i++;
intel.log(i);
EOF

mk s5_while_abort "x" << 'EOF'
infiltrate (affirmative) {
    intel.log("x");
    abort;
}
EOF

mk s5_while_proceed $'1\n2\n4\n5' << 'EOF'
agent i = 0;
infiltrate (i < 5) {
    i = i + 1;
    verify (i == 3) { proceed; }
    intel.log(i);
}
EOF

mk s5_nested_abort $'12\n22' << 'EOF'
agent a = 0;
infiltrate (a < 2) {
    a = a + 1;
    agent b = 0;
    infiltrate (affirmative) {
        b = b + 1;
        verify (b == 2) { abort; }
    }
    intel.log(a * 10 + b);
}
EOF

mk s5_abort_restores_scope "1" << 'EOF'
agent x = 1;
infiltrate (affirmative) {
    agent x = 2;
    abort;
}
intel.log(x);
EOF

mk s5_e_while_nonbool "[line 1] Runtime error: Condition must be a boolean." << 'EOF'
infiltrate (1) { }
EOF

mk s5_for_basic $'0\n1\n2' << 'EOF'
penetrate (agent i = 0; i < 3; i++) { intel.log(i); }
EOF

mk s5_for_down $'3\n2\n1' << 'EOF'
penetrate (agent i = 3; i > 0; i--) { intel.log(i); }
EOF

mk s5_for_step $'0\n2\n4' << 'EOF'
penetrate (agent i = 0; i < 6; i = i + 2) { intel.log(i); }
EOF

mk s5_e_for_scope "[line 2] Runtime error: Undefined variable 'i'." << 'EOF'
penetrate (agent i = 0; i < 2; i++) { }
intel.log(i);
EOF

mk s5_for_proceed $'0\n1\n3\n4' << 'EOF'
penetrate (agent i = 0; i < 5; i++) {
    verify (i == 2) { proceed; }
    intel.log(i);
}
EOF

mk s5_for_abort $'0\n1\n2' << 'EOF'
penetrate (agent i = 0; i < 10; i++) {
    verify (i == 3) { abort; }
    intel.log(i);
}
EOF

mk s5_for_shadow "100" << 'EOF'
agent i = 100;
penetrate (agent i = 0; i < 2; i++) { }
intel.log(i);
EOF

mk s5_for_body_agent $'1\n4\n9' << 'EOF'
penetrate (agent i = 1; i <= 3; i++) {
    agent sq = i * i;
    intel.log(sq);
}
EOF

mk s5_for_empty_parts "3" << 'EOF'
agent n = 0;
penetrate (; n < 3; ) {
    n++;
}
intel.log(n);
EOF

mk s5_for_nested $'1\n2\n2\n4' << 'EOF'
penetrate (agent i = 1; i <= 2; i++) {
    penetrate (agent j = 1; j <= 2; j++) {
        intel.log(i * j);
    }
}
EOF

mk s5_for_forever_abort "out" << 'EOF'
penetrate (;;) { abort; }
intel.log("out");
EOF

mk s5_incr_statement "4" << 'EOF'
agent x = 5;
x++;
x--;
x--;
intel.log(x);
EOF

mk s5_p_abort_outside "[line 1] Error at 'abort': 'abort' can only be used inside a loop." << 'EOF'
abort;
EOF

mk s5_p_proceed_outside "[line 1] Error at 'proceed': 'proceed' can only be used inside a loop." << 'EOF'
proceed;
EOF

mk s5_p_while_paren "[line 1] Error at 'affirmative': Expect '(' after 'infiltrate'." << 'EOF'
infiltrate affirmative intel.log("a");
EOF

mk s5_p_loop_agent_body "[line 1] Error at 'agent': Declaration needs a block." << 'EOF'
infiltrate (affirmative) agent x = 1;
EOF