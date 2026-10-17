package ru.client.ui.clickgui.components;

import ru.client.ui.UiSounds;

import com.mojang.blaze3d.platform.InputConstants;
import ru.client.modules.settings.ColorSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class ColorComponent extends SettingComponent<ColorSetting> {

    private static final float ROW = 14, PAD = 4, SV_H = 56, BAR_H = 6, HEX_H = 12;
    private static final float PICKER_H = PAD + SV_H + PAD + BAR_H + PAD + BAR_H + PAD + HEX_H + PAD;

    private final Animation expandAnim = new Animation(12f, 0f);
    private boolean expanded, dragSV, dragHue, dragAlpha, editing;

    private float hue, sat, bri;
    private int alpha, lastValue;
    private final StringBuilder hexBuffer = new StringBuilder(8);

    private float px, pw, svY, hueY, alphaY, hexY;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        syncFromSetting();
    }

    private void syncFromSetting() {
        int c = setting.getArgb();
        float[] hsb = Color.RGBtoHSB(ColorUtils.red(c), ColorUtils.green(c), ColorUtils.blue(c), null);
        if (hsb[1] > 0.001f && hsb[2] > 0.001f) hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        alpha = ColorUtils.alpha(c);
        lastValue = c;
    }

    private void apply() {
        int c = ColorUtils.withAlpha(Color.HSBtoRGB(hue, sat, bri), alpha);
        setting.set(c);
        lastValue = c;
    }

    @Override
    public float getHeight() { return ROW + PICKER_H * expandAnim.get(); }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        if (setting.getArgb() != lastValue) syncFromSetting();
        float ea = expandAnim.update();

        label(ctx, setting.getDisplayName(), x + 4, x + width - 18, y + 3, t.textDim());
        float swX = x + width - 18;
        RenderUtils.drawRoundedRect(ctx, swX, y + 3, 14, 8, 2, setting.getArgb());
        RenderUtils.drawRoundedOutline(ctx, swX, y + 3, 14, 8, 2, 1, 0x50FFFFFF);

        if (ea < 0.01f) return;

        px = x + 4;
        pw = width - 8;
        float top = y + ROW;
        svY = top + PAD;
        hueY = svY + SV_H + PAD;
        alphaY = hueY + BAR_H + PAD;
        hexY = alphaY + BAR_H + PAD;

        if (dragSV) {
            sat = Mth.clamp((mx - px) / pw, 0f, 1f);
            bri = 1f - Mth.clamp((my - svY) / SV_H, 0f, 1f);
            apply();
        }
        if (dragHue) {
            hue = Mth.clamp((mx - px) / pw, 0f, 0.999f);
            apply();
        }
        if (dragAlpha) {
            alpha = Math.round(Mth.clamp((mx - px) / pw, 0f, 1f) * 255);
            apply();
        }

        boolean clip = ea < 0.999f;
        if (clip) ctx.enableScissor((int) x, (int) top, (int) Math.ceil(x + width), (int) Math.ceil(top + PICKER_H * ea));

        int hueColor = Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
        int opaque = setting.getArgb() | 0xFF000000;

        RenderUtils.drawGradientRect(ctx, px, svY, pw, SV_H, 0xFFFFFFFF, hueColor, 0xFF000000, 0xFF000000);
        float cx = px + sat * pw, cy = svY + (1f - bri) * SV_H;
        RenderUtils.drawCircle(ctx, cx, cy, 3.5f, 0xFFFFFFFF);
        RenderUtils.drawCircle(ctx, cx, cy, 2.5f, opaque);

        float seg = pw / 6f;
        for (int i = 0; i < 6; i++) {
            int c1 = Color.HSBtoRGB(i / 6f, 1f, 1f) | 0xFF000000;
            int c2 = Color.HSBtoRGB((i + 1) / 6f, 1f, 1f) | 0xFF000000;
            RenderUtils.drawHorizontalGradient(ctx, px + i * seg, hueY, seg, BAR_H, c1, c2);
        }
        RenderUtils.drawRect(ctx, px + hue * pw - 1, hueY - 1, 2, BAR_H + 2, 0xFFFFFFFF);

        RenderUtils.drawRect(ctx, px, alphaY, pw, BAR_H, 0xFF3A3A44);
        RenderUtils.drawHorizontalGradient(ctx, px, alphaY, pw, BAR_H, ColorUtils.withAlpha(opaque, 0), opaque);
        RenderUtils.drawRect(ctx, px + (alpha / 255f) * pw - 1, alphaY - 1, 2, BAR_H + 2, 0xFFFFFFFF);

        RenderUtils.drawRoundedRect(ctx, px, hexY, pw, HEX_H, 3, editing ? 0xFF26262E : 0xFF1C1C22);
        if (editing) RenderUtils.drawRoundedOutline(ctx, px, hexY, pw, HEX_H, 3, 1, accent());
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = editing ? "#" + hexBuffer + (caret ? "_" : "") : setting.toHex();
        text(ctx, shown, px + 4, hexY + 2, editing ? t.text() : t.textDim());

        if (clip) ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, x, y, width, ROW) && (button == 0 || button == 1)) {
            expanded = !expanded;
            UiSounds.dropdown(expanded);
            expandAnim.setTarget(expanded ? 1f : 0f);
            if (!expanded) editing = false;
            return true;
        }
        if (!expanded || expandAnim.get() < 0.9f || button != 0) return false;

        if (inside(mx, my, px, svY, pw, SV_H)) { dragSV = true; return true; }
        if (inside(mx, my, px, hueY - 1, pw, BAR_H + 2)) { dragHue = true; return true; }
        if (inside(mx, my, px, alphaY - 1, pw, BAR_H + 2)) { dragAlpha = true; return true; }
        if (inside(mx, my, px, hexY, pw, HEX_H)) {
            editing = true;
            hexBuffer.setLength(0);
            hexBuffer.append(setting.toHex().substring(1));
            return true;
        }
        editing = false;
        return false;
    }

    @Override
    public boolean isTyping() { return editing; }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        dragSV = dragHue = dragAlpha = false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!editing) return false;
        switch (key) {
            case InputConstants.KEY_BACKSPACE -> { if (!hexBuffer.isEmpty()) hexBuffer.setLength(hexBuffer.length() - 1); }
            case InputConstants.KEY_RETURN, 88 -> commitHex();
            case InputConstants.KEY_ESCAPE -> editing = false;
            default -> {}
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!editing) return false;
        if (Character.digit(chr, 16) >= 0 && hexBuffer.length() < 8) hexBuffer.append(Character.toUpperCase(chr));
        return true;
    }

    private void commitHex() {
        int len = hexBuffer.length();
        if (len == 6 || len == 8) {
            try {
                setting.fromHex(hexBuffer.toString());
                syncFromSetting();
            } catch (NumberFormatException ignored) {}
        }
        editing = false;
    }
}
