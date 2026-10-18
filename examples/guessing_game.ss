
mission() {
    agent secret = 7;
    agent guess = 0;

    intel.log("Guess the number between 1 and 10!");

    infiltrate (guess != secret) {
        intel.log("Enter your guess:");
        guess = intel.int();

        verify (guess < secret) {
            intel.log("Too low!");
        } alternative (guess > secret) {
            intel.log("Too high!");
        } otherwise {
            intel.log("Correct!");
        }
    }

    intel.log("You guessed it!");
}
