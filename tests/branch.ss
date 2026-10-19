mission() {
    agent x = 5;
    verify (x > 10) {
        intel.log("first");
    } alternative (x > 3) {
        intel.log("second");
    } otherwise {
        intel.log("third");
    }
}