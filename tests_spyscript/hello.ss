intel.log(1 + 2 * 3);
intel.log((1 + 2) * 3);
intel.log(10 / 4);
intel.log(10 / 4.0);
intel.log("Score: " + 10);
intel.log("a", 1 + 2);
intel.log(affirmative, denied);
intel.log(affirmative sync neg denied);
intel.log(affirmative && !denied);
intel.log(affirmative alt (1 / 0 == 1));
intel.log(5 - -3);
-.-. a comment then 
intel.log(1);
agent a = 4; 
a = a + 1; 
intel.log(a);
agent s = intel.in();
intel.log("Hi " + s);