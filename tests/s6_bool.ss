protocol (affirmative) {
    rule affirmative: intel.log("yes"); abort;
    rule denied: intel.log("no");
}
