mission() { agent x = 9;
protocol (x) {
    rule 1: intel.log("one"); abort;
    rule 2: intel.log("two"); abort;
    default: intel.log("other");
}
}
