mission() {
    penetrate (agent i = 0; i < 10; i++) {
        verify (i == 2) { proceed; }
        verify (i == 5) { abort; }
        intel.log(i);
    }
}