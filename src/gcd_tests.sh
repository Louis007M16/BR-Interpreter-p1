#!/bin/bash
# usage (from src):  bash gcd_test.sh
# Runs a SpyScript gcd program on many pairs and compares each answer with Python's math.gcd.
cat > /tmp/gcd.ss << 'SS'
operation gcd(agent a, agent b) {
    infiltrate (b != 0) {
        agent t = a % b;
        a = b;
        b = t;
    }
    extract a;
}
mission() {
    agent x = intel.int();
    agent y = intel.int();
    intel.log(gcd(x, y));
}
SS
bad=0; n=0
for a in 0 1 12 18 100 7 144 97 1000; do
  for b in 1 8 27 75 13 36 97 250; do
    expected=$(python3 -c "import math; print(math.gcd($a,$b))")
    actual=$(printf '%s\n%s\n' $a $b | java -cp . Main /tmp/gcd.ss 2>&1)
    n=$((n + 1))
    if [ "$actual" != "$expected" ]; then
      echo "MISMATCH gcd($a,$b): expected $expected, got $actual"
      bad=$((bad + 1))
    fi
  done
done
echo "gcd: $n pairs checked, $bad mismatches"