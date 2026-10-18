#!/bin/bash
# usage (from src):   bash wrap_old_tests.sh        (run ONCE after moving to Stage 7)
# Before Stage 7 a program was just a list of statements. Now everything must sit inside mission() { }.
# This wraps every old test that has no 'mission' yet. Line numbers stay the same.
DIR="${1:-../tests}"
for f in "$DIR"/*.ss; do
  grep -q 'mission' "$f" && continue
  { printf 'mission() { '; cat "$f"; [ -n "$(tail -c1 "$f")" ] && printf '\n'; printf '}\n'; } > "$f.tmp" && mv "$f.tmp" "$f"
  name=$(basename "$f" .ss)

  # a test expecting an error "at end" changes, because the added '}' now closes things earlier
  if [ "$name" = "s6_p_missing_brace" ]; then
    printf '%s' "[line 3] Error at end: Expect '}' after block." > "$DIR/$name.out"
  elif grep -q "at end" "$DIR/$name.out" 2>/dev/null; then
    echo "CHECK BY HAND (expects an error at the end of the file): $name"
  fi
done
echo "old tests wrapped in mission() { }"