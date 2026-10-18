mission() {
    verify (6 > 10) {
        intel.log("wowowiwaw");
    } alternative ( 6 < 10) {
        intel.log("abrakadabra");
    } otherwise {
        intel.log("engkkk");
    }
}