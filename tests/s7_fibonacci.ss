operation fib(agent n) {
    verify (n < 2) { extract n; }
    extract fib(n - 1) + fib(n - 2);
}
mission() { intel.log(fib(10)); }
