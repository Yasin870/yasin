package dev.droidfps.adaptive;

import dev.droidfps.DroidFps;
import dev.droidfps.config.DroidConfig;
import dev.droidfps.util.ThermalProbe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Saniyede bir FPS / bellek / sicaklik bakar, 0..3 arasi "performans seviyesi" belirler.
 * Seviye yukseldikce: parcacik, entity, block entity mesafesi ve gorsel efektler kisilir.
 * Render mesafesine BILEREK dokunmaz (Sodium'da degisiklik = tum chunk'lari yeniden olusturma).
 */
public final class AdaptiveController {
    private AdaptiveController() {}

    public static volatile int level = 0;
    public static int particlesThisTick = 0;

    private static final double[] PARTICLE      = {1.0, 0.60, 0.35, 0.15};
    private static final double[] PARTICLE_DIST = {1.0, 1.00, 0.75, 0.50};
    private static final double[] ENTITY        = {1.0, 0.80, 0.60, 0.45};
    private static final double[] BLOCK_ENTITY  = {1.0, 0.75, 0.50, 0.33};

    private static int tickCounter, badStreak, goodStreak, cooldown, warmup;

    public static double particleBudget()      { return DroidConfig.particleBudgetPerTick * PARTICLE[level]; }
    public static double particleMaxDistance() { return DroidConfig.particleMaxDistance * PARTICLE_DIST[level]; }
    public static double entityCullDistance()  { return DroidConfig.entityCullDistance * ENTITY[level]; }
    public static double blockEntityCullDistance() { return DroidConfig.blockEntityCullDistance * BLOCK_ENTITY[level]; }

    public static void onJoin() {
        reset();
        warmup = DroidConfig.warmupSeconds;
    }

    public static void reset() {
        level = 0;
        tickCounter = badStreak = goodStreak = cooldown = 0;
        OptionOverrides.refresh();
    }

    public static void tick(MinecraftClient mc) {
        particlesThisTick = 0;
        if (!DroidConfig.adaptive) return;
        if (mc.world == null || mc.player == null) return;
        if (++tickCounter < 20) return;
        tickCounter = 0;
        if (mc.isPaused() || !mc.isWindowFocused()) return;
        if (warmup > 0) { warmup--; return; }
        if (cooldown > 0) cooldown--;

        int fps = mc.getCurrentFps();
        double target = DroidConfig.targetFps;
        int cap = mc.options.getMaxFps().getValue();
        if (cap < 260) target = Math.min(target, cap * 0.8); // kullanici FPS siniri hedefin altindaysa
        double recover = Math.max(target * 1.05, Math.min(target * 1.6, cap < 260 ? cap * 0.92 : 1e9));

        Runtime rt = Runtime.getRuntime();
        boolean memHigh = (rt.totalMemory() - rt.freeMemory()) / (double) rt.maxMemory() > DroidConfig.memoryHighFraction;
        double temp = ThermalProbe.readCelsius();
        boolean hot = !Double.isNaN(temp) && temp >= DroidConfig.thermalLimitC;

        boolean bad = (fps > 0 && fps < target) || memHigh || hot;
        boolean good = !bad && fps >= recover;

        if (bad) { badStreak++; goodStreak = 0; }
        else if (good) { goodStreak++; badStreak = 0; }
        else { badStreak = 0; goodStreak = 0; }

        if (badStreak >= DroidConfig.badSeconds && level < 3 && cooldown == 0) {
            setLevel(mc, level + 1, fps);
        } else if (goodStreak >= DroidConfig.goodSeconds && level > 0 && cooldown == 0) {
            setLevel(mc, level - 1, fps);
        }
    }

    private static void setLevel(MinecraftClient mc, int n, int fps) {
        level = n;
        badStreak = goodStreak = 0;
        cooldown = 8;
        OptionOverrides.refresh();
        DroidFps.LOG.info("Performans seviyesi {} (fps={})", n, fps);
        if (DroidConfig.adaptiveNotify && mc.player != null) {
            mc.player.sendMessage(Text.literal("DroidFPS: performans seviyesi " + n + " (FPS " + fps + ")"), true);
        }
    }
}
