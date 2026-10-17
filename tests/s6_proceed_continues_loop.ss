penetrate (agent i = 0; i < 4; i++) {
    protocol (i) {
        rule 1: proceed;
        rule 2: intel.log("two"); abort;
    }
    intel.log(i);
}
