package ru.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.NoRender;

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinInGameOverlayRenderer {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void lamivisuals$fire(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
        NoRender nr = Modules.enabled(NoRender.class);
        if (nr != null && nr.fire()) ci.cancel();
    }
}
