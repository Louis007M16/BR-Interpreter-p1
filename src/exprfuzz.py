import random, re, subprocess, sys

def wrap(n):
    n &= 0xFFFFFFFF
    return n - (1 << 32) if n >= (1 << 31) else n

class J:  # an int with Java/SpyScript semantics
    def __init__(self, v): self.v = wrap(v)
    def __add__(s, o): return J(s.v + o.v)
    def __sub__(s, o): return J(s.v - o.v)
    def __mul__(s, o): return J(s.v * o.v)
    def __truediv__(s, o):               # "/" : integer division, truncates toward zero
        if o.v == 0: raise ZeroDivisionError
        q = abs(s.v) // abs(o.v)
        return J(q if (s.v >= 0) == (o.v >= 0) else -q)
    def __mod__(s, o):                   # "%" : sign follows the left side (like Java)
        if o.v == 0: raise ZeroDivisionError
        r = abs(s.v) % abs(o.v)
        return J(r if s.v >= 0 else -r)
    def __neg__(s): return J(-s.v)

def gen(depth=0):
    r = random.random()
    if depth > 3 or r < 0.3:
        return str(random.randint(0, 30))
    if r < 0.45:
        return "(" + gen(depth + 1) + ")"
    if r < 0.55:
        return "- " + gen(depth + 1)
    return gen(depth + 1) + " " + random.choice("+-*/%") + " " + gen(depth + 1)

random.seed(int(sys.argv[1]) if len(sys.argv) > 1 else 1)
exprs, expected = [], []
while len(exprs) < 400:
    e = gen()
    try:
        v = eval(re.sub(r"\d+", lambda m: f"J({m.group()})", e))
    except ZeroDivisionError:
        continue
    if not isinstance(v, J):
        continue
    exprs.append(e); expected.append(str(v.v))

program = "mission() {\n" + "\n".join(f"    intel.log({e});" for e in exprs) + "\n}\n"
open("/tmp/fuzz_prog.ss", "w").write(program)
out = subprocess.run(["java", "-cp", ".", "Main", "/tmp/fuzz_prog.ss"], capture_output=True, text=True)
actual = out.stdout.split("\n")[:-1]
bad = 0
if out.stderr or len(actual) != len(exprs):
    print("PROGRAM DID NOT RUN CLEANLY:", out.stderr[:300], f"({len(actual)} lines for {len(exprs)} expressions)")
    sys.exit(1)
for e, x, a in zip(exprs, expected, actual):
    if x != a:
        bad += 1
        if bad <= 8: print(f"MISMATCH  {e}  expected {x}  got {a}")
print(f"checked {len(exprs)} expressions, {bad} mismatches")