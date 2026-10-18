
mission() {
    agent n = 1;

    infiltrate (n <= 30) {
        verify (n % 15 == 0) {
            intel.log("FizzBuzz");
        } alternative (n % 3 == 0) {
            intel.log("Fizz");
        } alternative (n % 5 == 0) {
            intel.log("Buzz");
        } otherwise {
            intel.log(n);
        }

        n++;
    }
}
