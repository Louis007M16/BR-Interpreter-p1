mission() { protocol ("hi") {
    rule "hi": intel.log("hi"); abort;
    default: intel.log("other");
}
}
