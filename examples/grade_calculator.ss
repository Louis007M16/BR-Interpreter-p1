
mission() {
    intel.log("=== Student Grade Calculator ===");

    intel.log("Enter your grade (0-100):");
    agent grade = intel.num();

    protocol (grade) {
        rule 100:
            intel.log("Perfect score!");
            abort;
        rule 90:
            intel.log("Excellent!");
            abort;
        default:
            verify (grade >= 90) {
                intel.log("Grade: A");
            } alternative (grade >= 80) {
                intel.log("Grade: B");
            } alternative (grade >= 70) {
                intel.log("Grade: C");
            } alternative (grade >= 60) {
                intel.log("Grade: D");
            } otherwise {
                intel.log("Grade: F");
            }
    }
}
