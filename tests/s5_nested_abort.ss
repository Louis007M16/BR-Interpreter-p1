agent a = 0;
infiltrate (a < 2) {
    a = a + 1;
    agent b = 0;
    infiltrate (affirmative) {
        b = b + 1;
        verify (b == 2) { abort; }
    }
    intel.log(a * 10 + b);
}
