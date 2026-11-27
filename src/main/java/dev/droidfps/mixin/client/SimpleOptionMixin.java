package dev.droidfps.mixin.client;

import dev.droidfps.adaptive.OptionOverrides;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin {
    @Inject(method = "getValue()Ljava/lang/Object;", at = @At("RETURN"), cancellable = true, require = 0)
    private void droidfps$override(CallbackInfoReturnable<Object> cir) {
        if (!OptionOverrides.active) return;
        Object orig = cir.getReturnValue();
        if (orig == null) return;
        Object v = OptionOverrides.apply((SimpleOption<?>) (Object) this, orig);
        if (v != orig && !v.equals(orig)) cir.setReturnValue(v);
    }
}
