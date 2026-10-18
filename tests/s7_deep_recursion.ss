operation sum(agent n) {
    verify (n == 0) { extract 0; }
    extract n + sum(n - 1);
}
mission() { intel.log(sum(3000)); }
