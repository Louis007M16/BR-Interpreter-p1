operation isEven(agent n) { extract n % 2 == 0; }
mission() {
    intel.log(isEven(4));
    intel.log(isEven(5));
}
