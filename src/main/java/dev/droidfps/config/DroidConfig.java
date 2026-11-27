package dev.droidfps.config;

public final class DroidConfig {
    private DroidConfig() {}

    public static boolean adaptive = true;
    public static double targetFps = 45.0;
    public static int badSeconds = 3;
    public static int goodSeconds = 5;
    public static int warmupSeconds = 10;
    public static double memoryHighFraction = 0.85;
    public static double thermalLimitC = 42.0;
    public static boolean adaptiveNotify = true;

    public static boolean particleCap = true;
    public static double particleBudgetPerTick = 150.0;
    public static double particleMaxDistance = 32.0;

    public static boolean entityCull = true;
    public static double entityCullDistance = 32.0;

    public static boolean blockEntityCull = true;
    public static double blockEntityCullDistance = 48.0;

    public static boolean optionOverrides = true;
    public static int mipmapMax = -1;
    public static int biomeBlendMax = -1;
    public static boolean forceAoOff = false;
    public static boolean fboTrim = true;

    public static boolean glStateCache = true;

    public static int backgroundThreads() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
    }

    public static void load() {
    }
}
