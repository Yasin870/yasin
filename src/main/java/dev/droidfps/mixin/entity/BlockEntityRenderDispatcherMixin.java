package dev.droidfps.mixin.entity;

import dev.droidfps.adaptive.AdaptiveController;
import dev.droidfps.config.DroidConfig;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(
        method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void droidfps$cull(BlockEntity be, float tickDelta, MatrixStack matrices,
                               VertexConsumerProvider consumers, CallbackInfo ci) {
        if (!DroidConfig.blockEntityCull) return;
        if (be instanceof BeaconBlockEntity || be instanceof EndGatewayBlockEntity) return; // isin / kapi uzaktan gorunur
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        BlockPos p = be.getPos();
        double dx = p.getX() + 0.5 - cam.x, dy = p.getY() + 0.5 - cam.y, dz = p.getZ() + 0.5 - cam.z;
        double lim = AdaptiveController.blockEntityCullDistance();
        if (dx * dx + dy * dy + dz * dz > lim * lim) ci.cancel();
    }
}
