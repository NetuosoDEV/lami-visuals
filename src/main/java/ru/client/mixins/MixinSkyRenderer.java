package ru.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.utils.render.SkyShader;

@Mixin(SkyRenderer.class)
public abstract class MixinSkyRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void lamivisuals$prepareSky(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
        SkyShader.prepare();
    }

    @Inject(method = "renderSkyDisc", at = @At("TAIL"))
    private void lamivisuals$skyShader(RenderPass renderPass, Vector3fc skyColor, CallbackInfo ci) {
        SkyShader.draw(renderPass);
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$noVanillaStars(RenderPass renderPass, float starBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (SkyShader.active()) ci.cancel();
    }
}
