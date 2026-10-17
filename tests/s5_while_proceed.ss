agent i = 0;
infiltrate (i < 5) {
    i = i + 1;
    verify (i == 3) { proceed; }
    intel.log(i);
}
