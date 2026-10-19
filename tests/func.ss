operation fact(agent n) {
    verify (n <= 1) { extract 1; }
    extract n * fact(n - 1);
}

mission() {
    intel.log(fact(10));
    greet("Louis");
}

operation greet(agent name) {
    intel.log("hi", name);
}