package dev.droidfps.mixin.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.droidfps.gl.GlCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mojang'in onbellege almadigi viewport ve framebuffer bind cagrilari. */
@Mixin(GlStateManager.class)
public class GlStateCacheMixin {
    @Inject(method = "_viewport(IIII)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void droidfps$viewport(int x, int y, int w, int h, CallbackInfo ci) {
        if (GlCache.viewportRedundant(x, y, w, h)) ci.cancel();
    }

    @Inject(method = "_glBindFramebuffer(II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void droidfps$bindFbo(int target, int fb, CallbackInfo ci) {
        if (GlCache.framebufferRedundant(target, fb)) ci.cancel();
    }

    @Inject(method = "_glDeleteFramebuffers(I)V", at = @At("HEAD"), require = 0)
    private static void droidfps$deleteFbo(int fb, CallbackInfo ci) {
        GlCache.invalidate();
    }
}
