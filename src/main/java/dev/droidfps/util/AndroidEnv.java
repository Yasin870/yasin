package dev.droidfps.util;

/** PojavLauncher / Zalith / Amethyst ortam tespiti (env degiskenleri). */
public final class AndroidEnv {
    private AndroidEnv() {}

    public static boolean isAndroid() {
        return System.getenv("POJAV_RENDERER") != null
                || System.getenv("POJAV_NATIVEDIR") != null
                || System.getenv("ANDROID_ROOT") != null
                || System.getProperty("os.name", "").toLowerCase().contains("android");
    }

    public static String renderer() {
        String r = System.getenv("POJAV_RENDERER");
        return r == null ? "bilinmiyor" : r;
    }
}
