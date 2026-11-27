package dev.droidfps.mixin.client;

import dev.droidfps.adaptive.AdaptiveController;
import dev.droidfps.config.DroidConfig;
import dev.droidfps.gl.GlCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    /** Her karenin basinda GL onbellegini sifirla (yuzey/context degisimine karsi guvenlik). */
    @Inject(method = "render(Z)V", at = @At("HEAD"), require = 0)
    private void droidfps$frameStart(boolean tick, CallbackInfo ci) {
        if (DroidConfig.glStateCache) GlCache.invalidate();
    }

    /** Yuksek seviyede glowing outline FBO gecisini atla. */
    @Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void droidfps$noOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DroidConfig.fboTrim && AdaptiveController.level >= 2) cir.setReturnValue(false);
    }
}
