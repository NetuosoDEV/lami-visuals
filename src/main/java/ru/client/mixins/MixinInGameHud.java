package ru.client.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.hud.Potions;
import ru.client.modules.impl.render.Crosshair;
import ru.client.modules.impl.render.NoRender;

@Mixin(Hud.class)
public abstract class MixinInGameHud {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$crosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Modules.enabled(Crosshair.class) != null) ci.cancel();
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$effects(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        Potions potions = Modules.enabled(Potions.class);
        if (potions != null && potions.hideVanilla()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar",
            at = @At("HEAD"), cancellable = true)
    private void lamivisuals$scoreboard(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        NoRender nr = Modules.enabled(NoRender.class);
        if (nr != null && nr.scoreboard()) ci.cancel();
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$vignette(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
        NoRender nr = Modules.enabled(NoRender.class);
        if (nr != null && nr.vignette()) ci.cancel();
    }

    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$overlay(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo ci) {
        NoRender nr = Modules.enabled(NoRender.class);
        if (nr != null && nr.pumpkin() && texture.getPath().contains("pumpkinblur")) ci.cancel();
    }
}
