package dev.droidfps.mixin.entity;

import dev.droidfps.adaptive.AdaptiveController;
import dev.droidfps.config.DroidConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    /** distanceSq: kameraya olan mesafenin karesi. */
    @Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void droidfps$cull(double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (!DroidConfig.entityCull) return;
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity) return;
        double size = self.getBoundingBox().getAverageSideLength();
        double eff = AdaptiveController.entityCullDistance() + size * 8.0; // buyuk mob'lar daha uzaktan gorunsun
        if (distanceSq > eff * eff) cir.setReturnValue(false);
    }
}
