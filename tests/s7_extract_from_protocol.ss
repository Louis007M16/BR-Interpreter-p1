operation find(agent x) {
    protocol (x) {
        rule 1: extract "one";
        rule 2: extract "two";
        default: extract "many";
    }
}
mission() {
    intel.log(find(1));
    intel.log(find(2));
    intel.log(find(9));
}
