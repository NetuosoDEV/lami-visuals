package ru.client.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.impl.hud.ChatTab;
import ru.client.ui.clickgui.Ui;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;
import ru.client.utils.render.UiBlur;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    private static final String RENDER = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V";
    private static final int PAD = 3, RADIUS = 5;

    @Shadow private @Nullable Component header;
    @Shadow private @Nullable Component footer;

    @Unique private int lamivisuals$stage;
    @Unique private boolean lamivisuals$hasHeader, lamivisuals$hasFooter;
    @Unique private float lamivisuals$cardTop;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void lamivisuals$begin(CallbackInfo ci) {
        lamivisuals$stage = 0;
        lamivisuals$hasHeader = false;
        lamivisuals$hasFooter = false;
    }

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<FormattedCharSequence> lamivisuals$split(Font font, FormattedText text, int maxWidth) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        if (ChatTab.styledTab()) {
            int a = 0, b = lines.size();
            while (a < b && lamivisuals$blank(lines.get(a))) a++;
            while (b > a && lamivisuals$blank(lines.get(b - 1))) b--;
            if (a > 0 || b < lines.size()) lines = new ArrayList<>(lines.subList(a, b));
        }
        if (text == header) lamivisuals$hasHeader = !lines.isEmpty();
        else if (text == footer) lamivisuals$hasFooter = !lines.isEmpty();
        return lines;
    }

    @Unique
    private static boolean lamivisuals$blank(FormattedCharSequence line) {
        boolean[] any = {false};
        line.accept((index, style, codepoint) -> {
            if (!Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint)) any[0] = true;
            return !any[0];
        });
        return !any[0];
    }

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void lamivisuals$tabFill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        if (!ChatTab.styledTab()) {
            graphics.fill(x0, y0, x1, y1, color);
            return;
        }
        if (color != Integer.MIN_VALUE) {
            RenderUtils.drawRoundedRect(graphics, x0, y0, x1 - x0, y1 - y0, 2, ColorUtils.withAlpha(0xFFFFFFFF, 14));
            return;
        }
        int kind = lamivisuals$stage == 0 && header != null ? 0 : lamivisuals$stage <= 1 ? 1 : 2;
        lamivisuals$stage = kind + 1;
        if (kind == 0 && !lamivisuals$hasHeader || kind == 2 && !lamivisuals$hasFooter) return;
        boolean first = kind == 0 || kind == 1 && !lamivisuals$hasHeader;
        boolean last = kind == 2 || kind == 1 && !lamivisuals$hasFooter;
        float x = x0 - PAD, w = x1 - x0 + PAD * 2;
        float y = first ? y0 - PAD : y0;
        float h = (last ? y1 + PAD : y1) - y;
        float rTop = first ? RADIUS : 0, rBottom = last ? RADIUS : 0;
        boolean blur = ChatTab.blur() && UiBlur.available();
        if (blur) UiBlur.panel(graphics, x, y, w, h, rTop, rTop, rBottom, rBottom, 255, ChatTab.blurStrength());
        RenderUtils.drawRoundedRect(graphics, x, y, w, h, rTop, rTop, rBottom, rBottom, ColorUtils.withAlpha(Ui.bg(), blur ? 160 : 228));
        if (first) lamivisuals$cardTop = y;
        else RenderUtils.drawRect(graphics, x + 6, y, w - 12, 1, Ui.LINE);
        if (last) RenderUtils.drawRoundedOutline(graphics, x, lamivisuals$cardTop, w, y + h - lamivisuals$cardTop, RADIUS, 1, Ui.LINE_HI);
    }

    @ModifyConstant(method = RENDER, constant = @Constant(intValue = 13))
    private int lamivisuals$pingWidth(int original) {
        return ChatTab.styledTab() && ChatTab.tabPing() ? 30 : original;
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$ping(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
        if (!ChatTab.styledTab() || !ChatTab.tabPing()) return;
        ci.cancel();
        int ms = info.getLatency();
        String s = ms < 0 ? "?" : ms + "ms";
        int color = ms < 0 ? Theme.get().textDim()
                : ms < 80 ? 0xFF4ADE80 : ms < 150 ? 0xFFA3E635 : ms < 300 ? 0xFFFACC15 : ms < 600 ? 0xFFFB923C : 0xFFF87171;
        float scale = 0.8f;
        float w = Fonts.width(s, Fonts.BOLD) * scale;
        Fonts.drawScaled(graphics, s, xo + slotWidth - 2 - w, yo + 1, scale, color, Fonts.BOLD);
    }
}
