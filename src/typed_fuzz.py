#!/usr/bin/env python3
"""
Random expression tester for SpyScript: integers, decimals, booleans and text.
usage (from src, after javac *.java):   python3 typed_fuzz.py [seed] [how many]

It builds random typed expressions, works out the right answer with its OWN model of the
language rules, runs all of them in one SpyScript program, and compares line by line.
"""
import math, os, random, re, subprocess, sys
import concurrent.futures as cf

CP = os.environ.get("SPY_CP", ".")                  # where the compiled .class files are (the current folder by default)

# ---------------------------------------------------------------- the model of the language rules
def wrap(n):                                        # integers are 32-bit and wrap around, like Java
    n &= 0xFFFFFFFF
    return n - (1 << 32) if n >= (1 << 31) else n

class Err(Exception):                               # the SpyScript program would stop with a runtime error
    pass

def isnum(v):
    return type(v) in (int, float)

def show(v):                                        # how + shows a value inside text
    if type(v) is bool: return "affirmative" if v else "denied"
    return str(v)

def same(a, b):                                     # the == rule: numbers by value, everything else same type + same value
    if isnum(a) and isnum(b): return float(a) == float(b)
    return type(a) is type(b) and a == b

def arith(op, a, b):
    if type(a) is int and type(b) is int:           # int op int stays an int
        if op == "+": return wrap(a + b)
        if op == "-": return wrap(a - b)
        if op == "*": return wrap(a * b)
        if op in "/%":
            if b == 0: raise Err
            q = abs(a) // abs(b)
            q = q if (a >= 0) == (b >= 0) else -q   # division truncates toward zero
            return wrap(q) if op == "/" else wrap(a - q * b)
        return {"<": a < b, "<=": a <= b, ">": a > b, ">=": a >= b}[op]
    x, y = float(a), float(b)                       # anything with a decimal becomes a decimal
    if op == "+": return x + y
    if op == "-": return x - y
    if op == "*": return x * y
    if op in "/%":
        if y == 0: raise Err
        return x / y if op == "/" else math.fmod(x, y)
    return {"<": x < y, "<=": x <= y, ">": x > y, ">=": x >= y}[op]

def ev(n):
    k = n[0]
    if k == "lit": return n[1]
    if k == "neg":
        v = ev(n[1])
        if type(v) is int: return wrap(-v)
        if type(v) is float: return -v
        raise Err
    if k == "not":
        v = ev(n[1])
        if type(v) is not bool: raise Err
        return not v
    if k == "log":                                  # and / or: the right side only runs if the left did not decide
        _, op, l, r = n
        lv = ev(l)
        if type(lv) is not bool: raise Err
        if op == "or" and lv: return True
        if op == "and" and not lv: return False
        rv = ev(r)
        if type(rv) is not bool: raise Err
        return rv
    _, op, l, r = n                                 # "bin"
    a, b = ev(l), ev(r)
    if op == "==": return same(a, b)
    if op == "!=": return not same(a, b)
    if op == "+" and (type(a) is str or type(b) is str): return show(a) + show(b)
    if not (isnum(a) and isnum(b)): raise Err
    return arith(op, a, b)

# ---------------------------------------------------------------- random expression trees
def lit_int():
    v = random.randint(0, 30)
    return ("lit", v, str(v))

def lit_dbl():
    d = random.choice([1, 2])
    text = f"{random.uniform(0, 30):.{d}f}"
    return ("lit", float(text), text)

def lit_bool():
    v = random.random() < 0.5
    return ("lit", v, "affirmative" if v else "denied")

def lit_str():
    s = random.choice(["a", "hi", "spy", "007", "x y", ""])
    return ("lit", s, '"' + s + '"')

def gen_int(d=0):
    r = random.random()
    if d >= 4 or r < 0.35: return lit_int()
    if r < 0.5: return ("neg", gen_int(d + 1))
    return ("bin", random.choice("+-*/%"), gen_int(d + 1), gen_int(d + 1))

def gen_num(d=0):
    return gen_int(d) if random.random() < 0.5 else gen_dbl(d)

def gen_dbl(d=0):
    r = random.random()
    if d >= 4 or r < 0.25: return lit_dbl()
    if r < 0.37: return ("neg", gen_dbl(d + 1))
    op = random.choice("+-*/%")
    if random.random() < 0.5: return ("bin", op, gen_dbl(d + 1), gen_num(d + 1))
    return ("bin", op, gen_num(d + 1), gen_dbl(d + 1))

def gen_any(d):
    return random.choice([gen_int, gen_dbl, gen_bool, gen_str])(d)

def gen_bool(d=0):
    r = random.random()
    if d >= 4 or r < 0.2: return lit_bool()
    if r < 0.4: return ("bin", random.choice(["<", "<=", ">", ">="]), gen_num(d + 1), gen_num(d + 1))
    if r < 0.6:                                                     # == and != between any two things
        t = random.choice([gen_num, gen_bool, gen_str])
        u = t if random.random() < 0.8 else random.choice([gen_num, gen_bool, gen_str])
        return ("bin", random.choice(["==", "!="]), t(d + 1), u(d + 1))
    if r < 0.7: return ("not", gen_bool(d + 1))
    return ("log", random.choice(["and", "or"]), gen_bool(d + 1), gen_bool(d + 1))

def gen_str(d=0):
    if d >= 4 or random.random() < 0.3: return lit_str()
    pick = lambda: random.choice([gen_str, gen_int, gen_bool])(d + 1)   # (no decimals inside text: Java and Python print them differently)
    left, right = pick(), gen_str(d + 1)
    return ("bin", "+", left, right) if random.random() < 0.5 else ("bin", "+", right, left)

# ---------------------------------------------------------------- writing an expression as SpyScript text
def prec(n):
    k = n[0]
    if k == "lit": return 9
    if k == "neg": return 8
    if k == "not": return 3
    if k == "log": return 2 if n[1] == "and" else 1
    return {"*": 7, "/": 7, "%": 7, "+": 6, "-": 6, "<": 5, "<=": 5, ">": 5, ">=": 5, "==": 4, "!=": 4}[n[1]]

def part(child, need):                                              # parenthesise only when the grammar needs it (or by chance)
    s = render(child)
    return "(" + s + ")" if prec(child) < need or random.random() < 0.12 else s

def render(n):
    k = n[0]
    if k == "lit": return n[2]
    if k == "neg": return "- " + part(n[1], 8)
    if k == "not": return random.choice(["neg ", "!"]) + part(n[1], 3)
    p = prec(n)
    if k == "log":
        op = {"and": random.choice(["sync", "&&"]), "or": random.choice(["alt", "||"])}[n[1]]
    else:
        op = n[1]
    return part(n[2], p) + " " + op + " " + part(n[3], p + 1)       # left grouping: the right side needs a strictly higher level

# ---------------------------------------------------------------- hand-picked cases
FIXED = [                                           # (expression, exact expected output)
    ("1 / 2.0", "0.5"), ("7 / 2", "3"), ("7.0 / 2", "3.5"), ("0.1 + 0.2", "0.30000000000000004"),
    ("1 == 1.0", "affirmative"), ('"x" + 1.5', "x1.5"), ("1.0 * 3", "3.0"), ("10 % 3", "1"),
    ("- 7 % 3", "-1"), ("7 / - 2", "-3"), ("2 + 3 * 4", "14"), ("(2 + 3) * 4", "20"), ("10 - 4 - 2", "4"),
    ('"a" + 1 + 2', "a12"), ('1 + 2 + "a"', "3a"), ("affirmative == 1", "denied"), ('"1" == 1', "denied"),
    ("neg denied sync denied", "denied"), ("neg (denied sync denied)", "affirmative"), ("! affirmative alt affirmative", "affirmative"),
    ("denied sync (1 / 0 == 1)", "denied"), ("affirmative alt (1 % 0 == 1)", "affirmative"),
    ("denied && (1 / 0 == 1)", "denied"), ("affirmative alt (1 / 0 == 1) sync 1", "affirmative"), ("affirmative || (1.5 / 0 == 1)", "affirmative"),
]

ERRORS = [                                          # (expression, exact error message)
    ("1 + affirmative", "Operands must be a Number."), ('"a" - 1', "Operands must be a Number."),
    ("affirmative < 2", "Operands must be a Number."), ('"a" < "b"', "Operands must be a Number."),
    ("neg 5", "Operand must be a boolean."), ("1 sync affirmative", "Operand must be a boolean."),
    ("affirmative sync 1", "Operand must be a boolean."), ("- affirmative", "Operand must be a number."),
    ("1 / 0", "Division by zero."), ("1.5 / 0", "Division by zero."), ("5 % 0", "Modulo by zero."),
    ("(affirmative alt (1 / 0 == 1)) sync 1", "Operand must be a boolean."),
]

# ---------------------------------------------------------------- running
def run_program(text, name):
    path = f"/tmp/typed_fuzz_{name}.ss"
    open(path, "w").write(text)
    return subprocess.run(["java", "-cp", CP, "Main", path], capture_output=True, text=True, timeout=60)

def main():
    seed = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    count = int(sys.argv[2]) if len(sys.argv) > 2 else 400
    random.seed(seed)

    cases = []                                      # (text, expected value)
    for text, out in FIXED:
        cases.append((text, ("exact", out)))
    while len(cases) < count + len(FIXED):
        tree = random.choice([gen_int, gen_dbl, gen_bool, gen_str, gen_bool])()
        try:
            value = ev(tree)
        except Err:
            continue
        if type(value) is float and (math.isnan(value) or math.isinf(value) or abs(value) > 1e15):
            continue
        cases.append((render(tree), ("value", value)))

    program = "mission() {\n" + "\n".join(f"    intel.log({t});" for t, _ in cases) + "\n}\n"
    out = run_program(program, "main")
    lines = out.stdout.split("\n")[:-1]
    if out.stderr or len(lines) != len(cases):
        print("PROGRAM DID NOT RUN CLEANLY:", out.stderr[:300], f"({len(lines)} lines for {len(cases)} expressions)")
        sys.exit(1)

    bad = 0
    for (text, (kind, want)), got in zip(cases, lines):
        if kind == "exact":
            ok, shown = got == want, want
        elif type(want) is bool:
            shown = "affirmative" if want else "denied"; ok = got == shown
        elif type(want) is int:
            shown = str(want); ok = got == shown
        elif type(want) is str:
            shown = want; ok = got == shown
        else:                                                       # a decimal: must look like one, and match by value
            shown = repr(want)
            try:
                ok = re.search(r"[.E]", got) is not None and math.isclose(float(got), want, rel_tol=1e-12, abs_tol=1e-12)
            except ValueError:
                ok = False
        if not ok:
            bad += 1
            if bad <= 8: print(f"MISMATCH   {text}\n   expected {shown}\n   got      {got}")

    def check_error(item):                                           # each runtime error needs its own program (and its own file)
        n, (text, msg) = item
        r = run_program(f"mission() {{\n    intel.log({text});\n}}\n", f"err{n}")
        want = f"[line 2] Runtime error: {msg}"
        return None if r.stderr.strip() == want and r.stdout == "" else (text, want, r.stderr.strip() or r.stdout.strip())

    with cf.ThreadPoolExecutor(max_workers=4) as pool:
        problems = [p for p in pool.map(check_error, enumerate(ERRORS)) if p]
    for text, want, got in problems:
        bad += 1
        print(f"ERROR CASE  {text}\n   expected {want}\n   got      {got}")

    print(f"seed {seed}: {len(cases)} expressions and {len(ERRORS)} error cases checked, {bad} mismatches")
    sys.exit(1 if bad else 0)

main()