
mission() {
    intel.log("Factorial and Fibonacci");

    intel.log("Factorial of 5:");
    intel.log(factorial(5));

    intel.log("First 10 Fibonacci numbers:");
    agent i = 0;

    infiltrate (i < 10) {
        intel.log(fibonacci(i));
        i++;
    }
}

operation factorial(agent n) {
    verify (n <= 1) {
        extract 1;
    }
    extract n * factorial(n - 1);
}

operation fibonacci(agent n) {
    verify (n <= 1) {
        extract n;
    }
    extract fibonacci(n - 1) + fibonacci(n - 2);
}
