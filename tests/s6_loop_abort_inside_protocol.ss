protocol (1) {
    rule 1:
        penetrate (agent i = 0; i < 5; i++) {
            verify (i == 2) { abort; }
            intel.log(i);
        }
        intel.log("after loop");
}
