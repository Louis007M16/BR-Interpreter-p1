-.-. SpyScript Error and Syntax Test Suite

mission() { -.-. TEST 1: Valid arithmetic intel.log("TEST 1: Arithmetic"); intel.log(10 + 5); intel.log(10 / 2);

-.-. TEST 2: Valid if-else
intel.log("TEST 2: Verify");
agent score = 80;
verify (score >= 75) {
    intel.log("PASS");
} otherwise {
    intel.log("FAIL");
}

-.-. TEST 3: Undefined variable (runtime error)
intel.log("TEST 3: Undefined variable");
intel.log(missingAgent);


}