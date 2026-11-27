package dev.droidfps.mixin.client;

import dev.droidfps.config.DroidConfig;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * big.LITTLE telefonlarda cekirdek-1 adet arka plan thread'i render thread'ini yavas cekirdege itiyor.
 * Kullanici -Dmax.bg.threads verdiyse ona dokunmayiz.
 */
@Mixin(Util.class)
public class UtilMixin {
    @Inject(method = "getMaxBackgroundThreads()I", at = @At("HEAD"), cancellable = true, require = 0)
    private static void droidfps$threads(CallbackInfoReturnable<Integer> cir) {
        if (System.getProperty("max.bg.threads") != null) return;
        cir.setReturnValue(DroidConfig.backgroundThreads());
    }
}
