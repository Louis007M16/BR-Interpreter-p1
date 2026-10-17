penetrate (agent i = 0; i < 3; i++) {
    protocol (i) {
        rule 1: abort;
        default: intel.log(i);
    }
}
