package dev.droidfps.mixin.particle;

import dev.droidfps.adaptive.AdaptiveController;
import dev.droidfps.config.DroidConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tick basina butce + mesafe siniri. Particle nesnesi hic uretilmedigi icin GC baskisini da azaltir. */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void droidfps$cap(ParticleEffect effect, double x, double y, double z,
                              double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (!DroidConfig.particleCap) return;
        if (AdaptiveController.particlesThisTick++ >= AdaptiveController.particleBudget()) {
            cir.setReturnValue(null);
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.gameRenderer == null) return;
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        double lim = AdaptiveController.particleMaxDistance();
        if (dx * dx + dy * dy + dz * dz > lim * lim) cir.setReturnValue(null);
    }
}
