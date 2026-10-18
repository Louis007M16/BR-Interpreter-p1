
mission() {
    agent a = 10;
    agent b = 3;
    agent choice = 1;

    intel.log("=== SpyScript Calculator ===");
    intel.log("1 = Addition");
    intel.log("2 = Subtraction");
    intel.log("3 = Multiplication");
    intel.log("4 = Division");
    intel.log("Choose an operation:", choice);

    protocol (choice) {
        rule 1:
            intel.log("Addition:", a + b);
            abort;
        rule 2:
            intel.log("Subtraction:", a - b);
            abort;
        rule 3:
            intel.log("Multiplication:", a * b);
            abort;
        rule 4:
            intel.log("Division:", a / b);
            abort;
        default:
            intel.log("Invalid operation!");
    }
}
