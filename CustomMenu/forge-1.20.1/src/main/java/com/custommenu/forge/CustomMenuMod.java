package com.custommenu.forge;

import com.custommenu.core.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

@Mod("custommenu")
public class CustomMenuMod {
    public static final CustomMenuConfig CONFIG = new CustomMenuConfig(FMLPaths.CONFIGDIR.get());

    // ---- görsel durumu (render thread) ----
    private static final AtomicReference<byte[]> pendingPng = new AtomicReference<>();
    private static final AtomicReference<GifAnim> pendingGif = new AtomicReference<>();
    private static ResourceLocation tex; private static int texW, texH;
    private static DynamicTexture dyn; private static GifAnim anim; private static long animStart; private static int shownFrame = -1;
    private static String loadedPath = ""; private static volatile boolean loading;

    public CustomMenuMod() {
        CONFIG.load();
        CONFIG.save();
        if (FMLEnvironment.dist == Dist.CLIENT) MinecraftForge.EVENT_BUS.register(new Client());
    }

    public static void applyAndSave() { CONFIG.save(); reload(); }

    /** Yeniden başlatmadan uygula. */
    public static void reload() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (tex != null) mc.getTextureManager().release(tex);
        } catch (Throwable ignored) { }
        tex = null; dyn = null; anim = null; shownFrame = -1; loadedPath = "";
        pendingPng.set(null); pendingGif.set(null);
        AudioPlayer.stop();
        CONFIG.load();
    }

    static ScreenType typeOf(Screen sc) {
        if (sc instanceof TitleScreen) return ScreenType.MAINMENU;
        if (sc instanceof PauseScreen) return ScreenType.PAUSE;
        if (sc instanceof WinScreen) return ScreenType.GAMEOVER; // Ender ejderhası sonrası bitiş/kredi ekranı
        return null;
    }

    /** Bu ekran için görsel aktifse ayarlarını döndürür, değilse null. */
    static ScreenSettings activeMedia(ScreenType t) {
        if (t == null || !CONFIG.enabled) return null;
        ScreenSettings s = CONFIG.get(t);
        return (s.enabled && s.showMedia && !s.mediaPath.isEmpty()) ? s : null;
    }

    /** Görseli (gerekirse) yükler; çizilebilir hale geldiyse true. */
    static boolean prepare(String path) {
        Minecraft mc = Minecraft.getInstance();
        byte[] png = pendingPng.getAndSet(null);
        if (png != null) {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
                texW = img.getWidth(); texH = img.getHeight(); anim = null; dyn = null;
                tex = mc.getTextureManager().register("custommenu_img", new DynamicTexture(img));
            } catch (Throwable ignored) { }
        }
        GifAnim g = pendingGif.getAndSet(null);
        if (g != null) {
            try {
                dyn = new DynamicTexture(g.w, g.h, false);
                texW = g.w; texH = g.h; anim = g; animStart = System.currentTimeMillis(); shownFrame = -1;
                tex = mc.getTextureManager().register("custommenu_img", dyn);
            } catch (Throwable ignored) { }
        }
        if (!path.equals(loadedPath) && !loading) {
            loading = true; loadedPath = path;
            Thread t = new Thread(() -> {
                try {
                    File f = new File(path);
                    if (MediaKind.detect(f.getName()) == MediaKind.GIF) {
                        pendingGif.set(GifAnim.load(f, 480, 48_000_000L));
                    } else {
                        BufferedImage bi = ImageIO.read(f);
                        if (bi != null) {
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            ImageIO.write(bi, "png", bo);
                            pendingPng.set(bo.toByteArray());
                        }
                    }
                } catch (Throwable ignored) { /* bozuk dosya çökertmez */ }
                loading = false;
            }, "CustomMenu-Loader");
            t.setDaemon(true); t.start();
        }
        return tex != null;
    }

    static void drawMedia(GuiGraphics g, ScreenSettings s, int sw, int sh) {
        if (anim != null && dyn != null) {
            int idx = anim.frameAt(System.currentTimeMillis() - animStart, s.loop);
            if (idx != shownFrame) {
                NativeImage px = dyn.getPixels();
                int[] fr = anim.frames[idx];
                if (px != null) {
                    for (int y = 0; y < anim.h; y++) for (int x = 0; x < anim.w; x++) px.setPixelRGBA(x, y, fr[y * anim.w + x]);
                    dyn.upload();
                }
                shownFrame = idx;
            }
        }
        float scale = s.fit.equals("fill") ? Math.max((float) sw / texW, (float) sh / texH)
                : s.fit.equals("stretch") ? -1 : Math.min((float) sw / texW, (float) sh / texH);
        int w = scale < 0 ? sw : Math.round(texW * scale), h = scale < 0 ? sh : Math.round(texH * scale);
        int x = Math.round((sw - w) * s.posX), y = Math.round((sh - h) * s.posY);
        g.blit(tex, x, y, w, h, 0, 0, texW, texH, texW, texH);
    }

    /** Ana menüdeki panorama çizicisinin yerine geçer; görsel hazırsa onu çizer, değilse normal panorama. */
    static class MediaPanorama extends PanoramaRenderer {
        private final PanoramaRenderer orig;
        MediaPanorama(PanoramaRenderer orig) { super(TitleScreen.CUBE_MAP); this.orig = orig; }
        @Override public void render(float partialTick, float alpha) {
            try {
                ScreenSettings s = activeMedia(ScreenType.MAINMENU);
                if (s != null && prepare(s.mediaPath)) {
                    Minecraft mc = Minecraft.getInstance();
                    GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                    drawMedia(g, s, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                    g.flush();
                    return;
                }
            } catch (Throwable ignored) { }
            orig.render(partialTick, alpha);
        }
    }

    static class Client {
        @SubscribeEvent
        public void onInit(ScreenEvent.Init.Post e) {
            Screen s = e.getScreen();
            if (s instanceof TitleScreen || s instanceof PauseScreen)
                e.addListener(Button.builder(Component.literal("Custom Menu"),
                        b -> Minecraft.getInstance().setScreen(new CustomMenuScreen(s))).bounds(4, 4, 90, 20).build());
            if (s instanceof TitleScreen) {
                try {
                    for (Field f : TitleScreen.class.getDeclaredFields()) {
                        if (f.getType() == PanoramaRenderer.class) {
                            f.setAccessible(true);
                            Object cur = f.get(s);
                            if (!(cur instanceof MediaPanorama)) f.set(s, new MediaPanorama((PanoramaRenderer) cur));
                        }
                    }
                } catch (Throwable ignored) { }
            }
        }

        /** ESC ve bitiş ekranı: ekranın arkasına çiz. (Ana menü MediaPanorama ile çizilir.) */
        @SubscribeEvent
        public void onRender(ScreenEvent.Render.Pre e) {
            try {
                ScreenType t = typeOf(e.getScreen());
                if (t == null || t == ScreenType.MAINMENU) return;
                ScreenSettings s = activeMedia(t);
                if (s == null || !prepare(s.mediaPath)) return;
                drawMedia(e.getGuiGraphics(), s, e.getScreen().width, e.getScreen().height);
            } catch (Throwable ignored) { }
        }

        /** Müzik: ilgili ekran açıkken çal, ekran kapanınca durdur. */
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            String path = ""; float vol = 1f; boolean loop = true;
            ScreenType t = typeOf(Minecraft.getInstance().screen);
            if (t != null && CONFIG.enabled) {
                ScreenSettings s = CONFIG.get(t);
                if (s.enabled && s.playSound && !s.audioPath.isEmpty()) { path = s.audioPath; vol = s.volume; loop = s.loop; }
            }
            AudioPlayer.update(path, vol, loop);
        }
    }
}
