package ru.client.mixins;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.client.modules.impl.render.ChunkAnimator;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererChunks {

    @Redirect(method = "extractSectionDrawGroups", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getVisibility(JJ)F"))
    private float lamivisuals$chunkAnimation(SectionRenderDispatcher.RenderSection section, long now, long fadeDuration) {
        return ChunkAnimator.visibility(section, now, fadeDuration);
    }
}
