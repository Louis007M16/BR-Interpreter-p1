import glob, random, subprocess, sys, os, concurrent.futures as cf

random.seed(int(sys.argv[1]) if len(sys.argv) > 1 else 1)
N = int(sys.argv[2]) if len(sys.argv) > 2 else 300
seeds = [open(f).read() for f in glob.glob(sys.argv[3] if len(sys.argv) > 3 else "../tests/*.ss")]
JUNK = list('{}();,+-*/%=!<>"&|.:\\') + ["agent", "verify", "protocol", "rule", "default", "abort", "proceed",
        "operation", "mission", "extract", "penetrate", "infiltrate", "intel", "sync", "alt", "neg", "-.-."]

def mutate(src):
    s = src
    for _ in range(random.randint(1, 3)):
        if not s: break
        kind = random.choice(["del", "ins", "deltok", "swap", "dupline", "trunc"])
        i = random.randrange(len(s))
        if kind == "del":      s = s[:i] + s[i+1:]
        elif kind == "ins":    s = s[:i] + random.choice(JUNK) + s[i:]
        elif kind == "deltok":
            t = s.split(" "); 
            if len(t) > 1: t.pop(random.randrange(len(t)))
            s = " ".join(t)
        elif kind == "swap":
            t = s.split(" ")
            if len(t) > 2:
                a, b = random.sample(range(len(t)), 2); t[a], t[b] = t[b], t[a]
            s = " ".join(t)
        elif kind == "dupline":
            ls = s.split("\n"); k = random.randrange(len(ls)); ls.insert(k, ls[k]); s = "\n".join(ls)
        else:                  s = s[:i]
    return s

def run(args):
    n, src = args
    path = f"/tmp/mut_{n}.ss"
    open(path, "w").write(src)
    try:
        r = subprocess.run(["java", "-cp", ".", "Main", path], capture_output=True, text=True,
                           timeout=4, input="5\n7\n")
    except subprocess.TimeoutExpired:
        return (n, "timeout", src, "")
    bad = ("Exception" in r.stderr or "\tat " in r.stderr or "Error:" not in r.stderr and r.stderr.strip() and r.returncode not in (0,))
    if r.returncode not in (0, 65, 70) or "Exception" in r.stderr or "\tat " in r.stderr or "Exception" in r.stdout:
        return (n, f"CRASH exit={r.returncode}", src, r.stderr[:400])
    return (n, "ok", src, "")

mutants = [(n, mutate(random.choice(seeds))) for n in range(N)]
crashes = 0; timeouts = 0
with cf.ThreadPoolExecutor(max_workers=6) as ex:
    for n, status, src, err in ex.map(run, mutants):
        if status == "timeout": timeouts += 1
        elif status != "ok":
            crashes += 1
            if crashes <= 3:
                print(f"--- {status}\n{err}\n--- program:\n{src}\n")
print(f"{N} mutated programs: {crashes} crashes, {timeouts} timeouts (endless loops are fine)")