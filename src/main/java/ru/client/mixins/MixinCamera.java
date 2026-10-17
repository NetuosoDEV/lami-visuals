package ru.client.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.FreeLook;
import ru.client.modules.impl.render.Zoom;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void lamivisuals$fov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = Modules.get(Zoom.class);
        if (zoom != null) cir.setReturnValue(cir.getReturnValue() * zoom.fovMultiplier());
    }

    @Inject(method = "alignWithEntity", at = @At("HEAD"))
    private void lamivisuals$freeLookFrame(float partialTicks, CallbackInfo ci) {
        FreeLook.onCameraFrame();
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float lamivisuals$freeLookYaw(Entity entity, float partialTicks, Operation<Float> original) {
        return FreeLook.cameraYaw(entity, original.call(entity, partialTicks));
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float lamivisuals$freeLookPitch(Entity entity, float partialTicks, Operation<Float> original) {
        return FreeLook.cameraPitch(entity, original.call(entity, partialTicks));
    }
}
