operation greet(agent name) { extract "Hello, " + name + "!"; }
mission() { intel.log(greet("Ana")); }
