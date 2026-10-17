package ru.client.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.client.modules.impl.hud.ChatTab;
import ru.client.ui.clickgui.Ui;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0))
    private void lamivisuals$input(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        if (!ChatTab.chatInput()) {
            graphics.fill(x0, y0, x1, y1, color);
            return;
        }
        RenderUtils.drawRoundedRect(graphics, x0, y0 - 1, x1 - x0, y1 - y0 + 2, 4, ColorUtils.withAlpha(Ui.bg(), 225));
        RenderUtils.drawRoundedOutline(graphics, x0, y0 - 1, x1 - x0, y1 - y0 + 2, 4, 1, ColorUtils.withAlpha(Ui.accent(), 140));
    }
}
