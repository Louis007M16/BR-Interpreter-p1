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
agent bool = confirmed   npc# affirmative= True denied=False
agent string = "Hello"
```
# Input & Output
**intel.log()**
```br
intel.log("Hello World!\n")  -.-. Outputs `Hello World!` to the console
```
**intel.in()**
```br
agent input = intel.in()  -.-. Parses input from user as a string
intel.int() -.-. integer only
intel.num() -.-. Parses integer and double input 
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
abort     -.-. break
```
```br
proceed   -.-. continue
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
operation greetAgent(name) {
    intel.log("Welcome, Agent " + name + "!\n")
    extract
}

mission() { -.-. main function
  -.-. ===================================
  -.-. SpyScript - Agent Registration
  -.-. ===================================


  agent string = "Shadow"
  agent number = 3
  agent bool = confirmed

  intel.log("=== SPYSCRIPT AGENT SYSTEM ===\n")
  intel.log("Enter your agent name: ")

  agent input = intel.in()

  greetAgent(input)

  verify (number > 0) {
      intel.log("Mission status: ACTIVE\n")
  } otherwise {
      intel.log("Mission status: INACTIVE\n")
  }

  intel.log("\nMission countdown:\n")

  penetrate (agent i = 3; i > 0; i--) {
      intel.log(i + "\n")
  }

  intel.log("Mission launched!\n")

  verify (bool) {
      intel.log("Authorization: GRANTED\n")
  } otherwise {
      intel.log("Authorization: DENIED\n")
  }

  intel.log("\n=== SYSTEM SHUTDOWN ===\n")
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