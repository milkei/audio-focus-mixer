package dev.milkei.audiofocusmixer;

/** Minimal privileged interface. Shizuku invokes destroy via reserved transaction code. */
interface IAppOpsService {
    void destroy() = 16777114;
    String readMode(String packageName) = 1;
    String writeMode(String packageName, String mode) = 2;
}
