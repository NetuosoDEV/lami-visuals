package ru.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.CameraTweaks;
import ru.client.modules.impl.render.GlintColor;
import ru.client.utils.render.PostFx;
import ru.client.utils.render.ScreenFx;
import ru.client.utils.render.UiBlur;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$hurtCam(CameraRenderState cameraState, PoseStack pose, CallbackInfo ci) {
        CameraTweaks cam = Modules.enabled(CameraTweaks.class);
        if (cam != null && cam.noHurtCam()) ci.cancel();
    }

    @Inject(method = "extractOptions", at = @At("TAIL"))
    private void lamivisuals$glint(CallbackInfo ci) {
        var opts = ((GameRenderer) (Object) this).gameRenderState().optionsRenderState;
        opts.glintSpeed *= GlintColor.speedMul();
        opts.glintStrength = GlintColor.strength(opts.glintStrength);
    }

    @Inject(method = "applyPostEffects", at = @At("TAIL"))
    private void lamivisuals$hitFx(CallbackInfo ci) {
        PostFx.apply();
        ScreenFx.apply();
        UiBlur.capture();
    }
}
