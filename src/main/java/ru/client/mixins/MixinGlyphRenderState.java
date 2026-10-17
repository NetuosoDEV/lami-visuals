package ru.client.mixins;

import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.utils.render.SmoothFonts;

@Mixin(GlyphRenderState.class)
public abstract class MixinGlyphRenderState {

    @Shadow public abstract TextRenderable renderable();

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$smooth(CallbackInfoReturnable<TextureSetup> cir) {
        var view = renderable().textureView();
        if (SmoothFonts.isSmooth(view)) cir.setReturnValue(TextureSetup.singleTextureWithLightmap(view, SmoothFonts.sampler()));
    }
}
