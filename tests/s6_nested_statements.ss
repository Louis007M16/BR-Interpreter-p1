mission() { agent x = 5;
protocol (x) {
    rule 5:
        verify (x > 3) { intel.log("big"); }
        abort;
}
}
