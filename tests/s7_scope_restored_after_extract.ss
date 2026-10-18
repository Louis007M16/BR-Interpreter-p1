operation f() {
    agent local = 1;
    infiltrate (affirmative) {
        agent inner = 2;
        extract inner;
    }
}
mission() {
    agent local = 99;
    intel.log(f());
    intel.log(local);
}
