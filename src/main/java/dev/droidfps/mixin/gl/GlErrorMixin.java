package dev.droidfps.mixin.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** glGetError, GL4ES/MobileGlues uzerinde her seferinde cevirici round-trip'i. Release'te gereksiz. */
@Mixin(GlStateManager.class)
public class GlErrorMixin {
    @Inject(method = "_getError()I", at = @At("HEAD"), cancellable = true, require = 0)
    private static void droidfps$skipGetError(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
