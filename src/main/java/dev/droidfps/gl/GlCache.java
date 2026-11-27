package dev.droidfps.gl;

/**
 * Sadece Mojang'in zaten onbellege almadigi cagrilar icin kucuk onbellek.
 * Buffer / VAO / program bind'i BILEREK yok: Sodium gibi modlar bunlari GlStateManager'i
 * atlayarak dogrudan bagladigi icin onbellek bayatlar ve ekran bozulur.
 * Her karenin basinda invalidate() cagrilir, boylece yuzey/context degisimi sorun olmaz.
 */
public final class GlCache {
    private GlCache() {}

    private static final int GL_FRAMEBUFFER = 0x8D40;

    private static int vx = -1, vy = -1, vw = -1, vh = -1;
    private static int fbo = -1;

    public static boolean viewportRedundant(int x, int y, int w, int h) {
        if (x == vx && y == vy && w == vw && h == vh) return true;
        vx = x; vy = y; vw = w; vh = h;
        return false;
    }

    public static boolean framebufferRedundant(int target, int fb) {
        if (target != GL_FRAMEBUFFER) { fbo = -1; return false; }
        if (fb == fbo) return true;
        fbo = fb;
        return false;
    }

    public static void invalidate() {
        vx = vy = vw = vh = -1;
        fbo = -1;
    }
}
