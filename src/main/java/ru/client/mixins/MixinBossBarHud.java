package ru.client.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.NoRender;

@Mixin(BossHealthOverlay.class)
public abstract class MixinBossBarHud {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$render(GuiGraphicsExtractor context, CallbackInfo ci) {
        NoRender nr = Modules.enabled(NoRender.class);
        if (nr != null && nr.bossBar()) ci.cancel();
    }
}
