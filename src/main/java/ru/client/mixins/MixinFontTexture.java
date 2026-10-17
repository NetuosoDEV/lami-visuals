package ru.client.mixins;

import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.utils.render.SmoothFonts;

import java.util.function.Supplier;

@Mixin(FontTexture.class)
public abstract class MixinFontTexture extends AbstractTexture {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lamivisuals$smooth(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo ci) {
        if (!SmoothFonts.wants(label.get())) return;
        this.sampler = SmoothFonts.sampler();
        SmoothFonts.register(this.textureView);
    }
}
