package ru.client.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.SwingAnimations;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Inject(method = "getModifiedSwingDuration", at = @At("RETURN"), cancellable = true)
    private void lamivisuals$swingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        SwingAnimations sa = Modules.enabled(SwingAnimations.class);
        if (sa != null) cir.setReturnValue(Math.max(1, sa.durationTicks()));
    }
}
