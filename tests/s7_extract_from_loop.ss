operation firstOver(agent limit) {
    penetrate (agent i = 1; i < 100; i++) {
        verify (i * i > limit) { extract i; }
    }
    extract -1;
}
mission() {
    intel.log(firstOver(50));
    intel.log(firstOver(100000));
}
