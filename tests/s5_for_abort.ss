penetrate (agent i = 0; i < 10; i++) {
    verify (i == 3) { abort; }
    intel.log(i);
}
