#!/bin/bash
# run from src:   javac *.java   then   ./run_tests.sh
pass=0; fail=0
for f in ../tests/*.br; do
  base="${f%.br}"
  input=/dev/null
  [ -f "$base.in" ] && input="$base.in"
  actual=$(java -cp . Main "$f" < "$input" 2>&1)
  expected=$(cat "$base.out")
  if [ "$actual" == "$expected" ]; then
    pass=$((pass + 1))
  else
    fail=$((fail + 1))
    echo "FAIL: $f"
    echo "  expected: $expected"
    echo "  got:      $actual"
  fi
done
echo "passed $pass, failed $fail"
[ "$fail" -eq 0 ]