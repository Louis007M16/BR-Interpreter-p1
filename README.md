# SpyScript-Interpreter
Making an interpreter using java programming language, that will execute basic programming language basic operations, variable declaration and  if-else statement, functions(No Class yet). This project is a programming language school project(Requirements are based on school project).
# Syntax and Features
# Comments
```br
-.-. This is a comment.
```
# Variable
```br
agent number = 10
agent bool = affirmative   npc# affirmative= True denied=False
agent string = "Hello"
```
# Input & Output
**intel.log()**
```br
intel.log("Hello World!\n");  -.-. Outputs `Hello World!` to the console
```
**intel.in()**
```br
agent input = intel.in();  -.-. Parses input from user as a string
intel.int();-.-. integer only
intel.num(); -.-. Parses integer and double input 
```
**If-else Statements**
```br
verify (condition) {
  -.-. body
} otherwise {
  -.-. body
}

```
**Else if Statements**
```br
verify (condition) {
  -.-. body
} alternative (condition) {
  -.-. body
} otherwise {
  -.-. body
}
```
# Looping
**While loop**
```br
infiltrate (condition) {
  -.-. body
}
```
**For loop**
```br
penetrate (agent i = 1; i < 10; i++) {
  -.-. body
}
```
# Switch
```br
protocol (expr) {
  rule 1:
    -.-. body
  default:
    -.-. body
}
```
# Break / Continue
```br
abort;     -.-. break
```
```br
proceed;   -.-. continue
```

# Function
**Main Function**
```br
  mission() {
    -.-. Program
  }
```
**Declaration**
```br
operation name(args) {
  -.-. body
}
```
**Function Call**
```br
name(args);
```
```br
extract   -.-. return
```
# Example Program:
```br
operation greetAgent(agent name) {
    intel.log("Welcome, Agent " + name + "!\n");
    extract name;
}

mission() { -.-. main function
  -.-. ===================================
  -.-. SpyScript - Agent Registration
  -.-. ===================================


  agent string = "Shadow";
  agent number = 3;
  agent bool = affirmative;

  intel.log("=== SPYSCRIPT AGENT SYSTEM ===\n");
  intel.log("Enter your agent name: ");

  agent input = intel.in();

  greetAgent(input);

  verify (number > 0) {
      intel.log("Mission status: ACTIVE\n");
  } otherwise {
      intel.log("Mission status: INACTIVE\n");
  }

  intel.log("\nMission countdown:\n");

  penetrate (agent i = 3; i > 0; i--) {
      intel.log(i + "\n");
  }

  intel.log("Mission launched!\n");

  verify (bool) {
      intel.log("Authorization: GRANTED\n");
  } otherwise {
      intel.log("Authorization: DENIED\n");
  }

  intel.log("\n=== SYSTEM SHUTDOWN ===\n");
}
```
**Output**
```br
  === SPYSCRIPT AGENT SYSTEM ===
  Enter your agent name: Louis
  Welcome, Agent Louis!
  Mission status: ACTIVE

  Mission countdown:
  3
  2
  1
  Mission launched!
  Authorization: GRANTED

=== SYSTEM SHUTDOWN ===
```
**How to run the example program:**
```br
in terminal
javac *.java
java Main ../program_tests/example.ss
or
java -jar spyscript.jar ../program_tests/example.ss < ../program_tests/example.in
```
# KEYWORDS
```br
agent, intel, verify, alternative, otherwise, infiltrate, penetrate, protocol, rule, default, abort, proceed, affirmative, denied, sync, alt, neg, mission, operation, extract, -.-.(comment),
intel.log, intel.in, intel.int and intel.num.
```
**Operation**
```br
+ - * / % == != < <= > >= ++ --, and logic as sync/&&, alt/||, neg/!.
```
**String**
```br
double quotes, with escapes \n \t \" \\
```
# Build/Rebuild JAR
```br
mkdir -p classes
javac -d classes src/*.java
jar --create --file spyscript.jar --main-class Main -C classes .
```
**Run test**
```br
java -jar spyscript.jar program_tests/example.ss < program_tests/example.in
```

# How to run(Jar)
```br
java -jar spyscript.jar program.ss
-.-. java 17+ required
-.-. my version: openjdk version "25.0.4.1" 2026-08-18 LTS
```

## Interpreter Interface (for IDE integration)

Run a program:

```
java -jar spyscript.jar program.ss
```

Requires Java 17 or newer.

| Behavior | Detail |
|---|---|
| Normal output | Written to **stdout** |
| Errors | Written to **stderr**. Syntax: `[line N, col M] Error at 'x': message`. Runtime: `[line N, col M] Runtime error: message` |
| Regex for locations | `\[line (\d+), col (\d+)\]` |
| Exit codes | `0` success, `64` no file given, `65` syntax error, `66` file unreadable, `70` runtime error |
| Input | `intel.in()`, `intel.int()` and `intel.num()` read one line from **stdin** and print no prompt |
| Infinite loops | Nothing stops them, so the IDE needs a **Stop** button that kills the process |
| Column | Counts characters, so a tab is 1 column |
| Messages without a location | Usage, file-not-found, recursion-depth and similar errors have no `[line ...]` prefix, so show them as plain text |

### Error examples

```
[line 3, col 12] Error at ';': Expect expression.
[line 5, col 15] Runtime error: Expected a whole number but got 'f'.
Error: cannot read file 'missing.ss'.
```