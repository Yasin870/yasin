package com.custommenu.forge;

import com.custommenu.core.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

@Mod("custommenu")
public class CustomMenuMod {
    public static final CustomMenuConfig CONFIG = new CustomMenuConfig(FMLPaths.CONFIGDIR.get());

    // Yüklü tek bir resim (şimdilik ekran başına bir resim, sadece PNG/JPG)
    private static final AtomicReference<byte[]> pendingPng = new AtomicReference<>();
    private static ResourceLocation tex; private static int texW, texH;
    private static String loadedPath = ""; private static volatile boolean loading;

    public CustomMenuMod() {
        CONFIG.load();
        CONFIG.save();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT)
            MinecraftForge.EVENT_BUS.register(new Client());
    }

    public static void applyAndSave() { CONFIG.save(); reload(); }

    public static void reload() { // yeniden başlatmadan uygula
        Minecraft mc = Minecraft.getInstance();
        if (tex != null) { mc.getTextureManager().release(tex); tex = null; }
        loadedPath = "";
        CONFIG.load();
    }

    static class Client {
        @SubscribeEvent
        public void onInit(ScreenEvent.Init.Post e) { // ayarlara her zaman erişim (mod kapalıyken de)
            Screen s = e.getScreen();
            if (s instanceof TitleScreen || s instanceof PauseScreen)
                e.addListener(Button.builder(Component.literal("Custom Menu"),
                        b -> Minecraft.getInstance().setScreen(new CustomMenuScreen(s))).bounds(4, 4, 90, 20).build());
        }

        @SubscribeEvent
        public void onRender(ScreenEvent.Render.Pre e) {
            try {
                ScreenType type = typeOf(e.getScreen());
                if (type == null || !CONFIG.enabled) return;
                ScreenSettings s = CONFIG.get(type);
                if (!s.enabled || !s.showMedia || s.mediaPath.isEmpty()) return;
                ensureLoaded(s.mediaPath);
                if (tex == null) return;
                int sw = e.getScreen().width, sh = e.getScreen().height;
                float scale = s.fit.equals("fill") ? Math.max((float) sw / texW, (float) sh / texH)
                        : s.fit.equals("stretch") ? -1 : Math.min((float) sw / texW, (float) sh / texH);
                int w = scale < 0 ? sw : Math.round(texW * scale), h = scale < 0 ? sh : Math.round(texH * scale);
                int x = Math.round((sw - w) * s.posX), y = Math.round((sh - h) * s.posY);
                e.getGuiGraphics().blit(tex, x, y, w, h, 0, 0, texW, texH, texW, texH);
            } catch (Throwable ignored) { /* bozuk medya oyunu çökertmesin */ }
        }

        private ScreenType typeOf(Screen sc) {
            if (sc instanceof TitleScreen) return ScreenType.MAINMENU;
            if (sc instanceof PauseScreen) return ScreenType.PAUSE;
            if (sc instanceof DeathScreen) return ScreenType.GAMEOVER;
            return null;
        }

        private void ensureLoaded(String path) {
            byte[] png = pendingPng.getAndSet(null);
            if (png != null) { // ana thread'de dokuyu oluştur
                try {
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
                    texW = img.getWidth(); texH = img.getHeight();
                    tex = Minecraft.getInstance().getTextureManager()
                            .register("custommenu_img", new DynamicTexture(img));
                } catch (Throwable ignored) { }
            }
            if (!path.equals(loadedPath) && !loading) {
                loading = true; loadedPath = path;
                Thread t = new Thread(() -> { // arka planda oku
                    try {
                        BufferedImage bi = ImageIO.read(new File(path));
                        if (bi != null) {
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            ImageIO.write(bi, "png", bo);
                            pendingPng.set(bo.toByteArray());
                        }
                    } catch (Throwable ignored) { }
                    loading = false;
                }, "CustomMenu-Loader");
                t.setDaemon(true); t.start();
            }
        }
    }
}
