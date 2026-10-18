
mission() {
    agent n = 29;
    agent divisor = 2;
    agent isPrime = affirmative;

    infiltrate (divisor * divisor <= n) {
        verify (n % divisor == 0) {
            isPrime = denied;
            abort;
        }

        divisor++;
    }

    verify (isPrime) {
        intel.log(n, "is prime");
    } otherwise {
        intel.log(n, "is not prime");
    }
}
