operation greetAgent(agent name) {
    intel.log("Welcome, Agent " + name + "!");
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
      intel.log("Mission status: ACTIVE");
  } otherwise {
      intel.log("Mission status: INACTIVE");
  }

  intel.log("\nMission countdown:");

  penetrate (agent i = 3; i > 0; i--) {
      intel.log(i);
  }

  intel.log("Mission launched!");

  verify (bool) {
      intel.log("Authorization: GRANTED\n");
  } otherwise {
      intel.log("Authorization: DENIED\n");
  }

  intel.log("\n=== SYSTEM SHUTDOWN ===\n");
}