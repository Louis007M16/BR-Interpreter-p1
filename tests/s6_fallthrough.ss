mission() { agent x = 1;
protocol (x) {
    rule 1: intel.log("a");
    rule 2: intel.log("b");
    rule 3: intel.log("c"); abort;
    rule 4: intel.log("d");
}
}
