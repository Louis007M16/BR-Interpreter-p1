mission() {
    protocol (2) {
        rule 1: intel.log("one"); abort;
        rule 2: intel.log("two");
        rule 3: intel.log("three"); abort;
        default: intel.log("other");
    }
}