package ru.client.mixins;

import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.Ambience;

@Mixin(ClientClockManager.ClientClockInstance.class)
public abstract class MixinClientWorldProperties {

    @Inject(method = "totalTicks", at = @At("RETURN"), cancellable = true)
    private void lamivisuals$time(CallbackInfoReturnable<Long> cir) {
        Ambience a = Modules.enabled(Ambience.class);
        if (a == null) return;
        Long t = a.timeOfDay();
        if (t == null) return;
        long day = Math.floorDiv(cir.getReturnValue(), 24000L);
        cir.setReturnValue(day * 24000L + Math.floorMod(t, 24000L));
    }

    @Inject(method = "partialTick", at = @At("RETURN"), cancellable = true)
    private void lamivisuals$partial(CallbackInfoReturnable<Float> cir) {
        Ambience a = Modules.enabled(Ambience.class);
        if (a != null && a.timeOfDay() != null) cir.setReturnValue(0f);
    }
}
