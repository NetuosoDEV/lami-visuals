package ru.client.mixins;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.utils.render.LateRenderer;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererLate {

    @Inject(method = "prepareTranslucents", at = @At("HEAD"))
    private void lamivisuals$uploadLate(CallbackInfo ci) {
        LateRenderer.upload();
    }

    @Inject(method = "executeClassicTransparency", at = @At("TAIL"))
    private void lamivisuals$drawLate(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame, RenderPass renderPass, CallbackInfo ci) {
        LateRenderer.draw(renderPass);
    }

    @Inject(method = "executeOit", at = @At("RETURN"))
    private void lamivisuals$drawLateOit(CallbackInfo ci) {
        LateRenderer.drawOwnPass();
    }
}
