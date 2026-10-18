agent counter = 0;
operation bump() { counter = counter + 1; }
mission() {
    bump();
    bump();
    bump();
    intel.log(counter);
}
