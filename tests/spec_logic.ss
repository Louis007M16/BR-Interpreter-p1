mission() {
    intel.log(affirmative sync denied);
    intel.log(affirmative alt denied);
    intel.log(neg denied);

    intel.log(affirmative && denied);
    intel.log(affirmative || denied);
    intel.log(!denied);
}