package ru.client.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.hud.ShulkerPreview;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen {

    @Shadow protected Slot hoveredSlot;

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$shulkerPreview(GuiGraphicsExtractor ctx, int x, int y, CallbackInfo ci) {
        ShulkerPreview sp = Modules.enabled(ShulkerPreview.class);
        if (sp == null || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!self.getMenu().getCarried().isEmpty()) return;
        if (sp.render(ctx, hoveredSlot.getItem(), x, y)) ci.cancel();
    }
}
