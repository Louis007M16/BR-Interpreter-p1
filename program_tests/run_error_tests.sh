#!/usr/bin/env bash

JAR="src/spyscript.jar" TMP="program_tests/tmp_error_test.ss" PASS=0 FAIL=0

run_test() { name="$1" expected="$2" code="$3"

printf '%s\n' "$code" > "$TMP"

java -jar "$JAR" "$TMP" > /tmp/spyscript_stdout.txt 2> /tmp/spyscript_stderr.txt
actual=$?

printf '\n===== %s =====\n' "$name"
printf 'Exit code: %s (expected %s)\n' "$actual" "$expected"

if [ -s /tmp/spyscript_stdout.txt ]; then
    echo "--- stdout ---"
    cat /tmp/spyscript_stdout.txt
fi

if [ -s /tmp/spyscript_stderr.txt ]; then
    echo "--- stderr ---"
    cat /tmp/spyscript_stderr.txt
fi

if [ "$actual" -eq "$expected" ]; then
    echo "RESULT: PASS"
    PASS=$((PASS + 1))
else
    echo "RESULT: FAIL"
    FAIL=$((FAIL + 1))
fi


}

run_test "Valid program" 0 'mission() { intel.log(1 + 2); }'

run_test "Unexpected character" 65 'mission() { intel.log(@); }'

run_test "Missing semicolon" 65 'mission() { agent x = 5 }'

run_test "Undefined variable" 70 'mission() { intel.log(unknownAgent); }'

run_test "Division by zero" 70 'mission() { intel.log(1 / 0); }'

run_test "Unterminated string" 65 'mission() { intel.log("hello); }'

rm -f "$TMP"

printf '\n============================\n' printf 'Passed: %s\n' "$PASS" printf 'Failed: %s\n' "$FAIL" printf '============================\n'