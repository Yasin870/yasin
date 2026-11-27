package dev.droidfps.adaptive;

import dev.droidfps.config.DroidConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;

/**
 * SimpleOption.getValue() cikisini degistirir. Kullanicinin kayitli ayarina (options.txt) dokunmaz.
 * Ayar ekrani aciksken override kapali: yoksa dugmeler yanlis degeri gosterip dongude takilir.
 */
public final class OptionOverrides {
    private OptionOverrides() {}

    public static volatile boolean active;

    static { refresh(); }

    public static void refresh() {
        active = DroidConfig.optionOverrides
                && (AdaptiveController.level > 0
                || DroidConfig.fboTrim
                || DroidConfig.mipmapMax >= 0
                || DroidConfig.biomeBlendMax >= 0
                || DroidConfig.forceAoOff);
    }

    public static Object apply(SimpleOption<?> self, Object value) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return value;
        GameOptions o = mc.options;
        if (o == null || mc.currentScreen instanceof GameOptionsScreen) return value;
        int lvl = AdaptiveController.level;

        if (value instanceof Integer i) {
            if (DroidConfig.mipmapMax >= 0 && self == o.getMipmapLevels()) return Math.min(i, DroidConfig.mipmapMax);
            if (DroidConfig.biomeBlendMax >= 0 && self == o.getBiomeBlendRadius()) return Math.min(i, DroidConfig.biomeBlendMax);
            return value;
        }
        if (value instanceof Boolean b) {
            if (!b) return value;
            if (lvl >= 1 && self == o.getEntityShadows()) return Boolean.FALSE;
            if (DroidConfig.forceAoOff && self == o.getAo()) return Boolean.FALSE;
            return value;
        }
        if (value instanceof CloudRenderMode cm && self == o.getCloudRenderMode()) {
            if (lvl >= 2 && cm != CloudRenderMode.OFF) return CloudRenderMode.OFF;
            if (lvl >= 1 && cm == CloudRenderMode.FANCY) return CloudRenderMode.FAST;
            return value;
        }
        if (value instanceof ParticlesMode pm && self == o.getParticles()) {
            if (lvl >= 2 && pm != ParticlesMode.MINIMAL) return ParticlesMode.MINIMAL;
            if (lvl >= 1 && pm == ParticlesMode.ALL) return ParticlesMode.DECREASED;
            return value;
        }
        if (value instanceof GraphicsMode gm && DroidConfig.fboTrim
                && gm == GraphicsMode.FABULOUS && self == o.getGraphicsMode()) {
            return GraphicsMode.FANCY;
        }
        return value;
    }
}
