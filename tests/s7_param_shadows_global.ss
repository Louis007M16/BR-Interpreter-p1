agent n = 100;
operation f(agent n) {
    n = n + 1;
    extract n;
}
mission() {
    intel.log(f(1));
    intel.log(n);
}
