package com.berat.gpt;

import net.minecraft.client.Minecraft;

/** Yalnizca istemci tarafi. Sunucu tarafi kod bunu sadece FMLEnvironment.dist == CLIENT iken cagirir. */
public final class ClientHooks {
    public static volatile long glitchUntil = 0L;
    public static volatile float intensity = 0.0F;

    private ClientHooks() {}

    public static void glitch(long ms, float inten) {
        glitchUntil = System.currentTimeMillis() + ms;
        intensity = inten;
    }

    /** Kontrollu kapanis: dunya zaten kaydedildi, Minecraft kendi kapatma yolunu kullanir. */
    public static void closeGame() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(mc::stop);
    }
}
