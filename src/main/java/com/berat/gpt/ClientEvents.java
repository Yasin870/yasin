package com.berat.gpt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = MinecraftGpt.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** Minecraft ayarlarina "Minecraft GPT" dugmesi ekler. */
    @SubscribeEvent
    public static void onOptionsInit(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof OptionsScreen screen) {
            Button b = Button.builder(Component.literal("Minecraft GPT"),
                    btn -> Minecraft.getInstance().setScreen(new GptConfigScreen(screen)))
                    .bounds(6, screen.height - 26, 110, 20).build();
            e.addListener(b);
        }
    }

    /** Hafif ekran ustu glitch efekti: seritler ve karanlik titremeler (gercek FPS dusurmez). */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        long now = System.currentTimeMillis();
        if (now > ClientHooks.glitchUntil) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();
        float inten = ClientHooks.intensity;
        Random r = new Random(now / 70L);
        int bars = 3 + (int) (inten * 9.0F);
        for (int i = 0; i < bars; i++) {
            int y = r.nextInt(h);
            int bh = 1 + r.nextInt(3 + (int) (inten * 6.0F));
            int shift = r.nextInt(Math.max(1, w / 6));
            int color = r.nextBoolean() ? 0x66FF0000 : 0x55000000;
            g.fill(shift, y, w, Math.min(h, y + bh), color);
        }
        if (r.nextInt(7) == 0) {
            g.fill(0, 0, w, h, 0x99000000);
        }
    }
}
