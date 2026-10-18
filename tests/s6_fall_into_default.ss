mission() { protocol (2) {
    rule 1: intel.log("a");
    rule 2: intel.log("b");
    default: intel.log("d");
}
}
