package ru.client.modules.impl.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class Crosshair extends Module {

    private final ModeSetting mode = register(new ModeSetting("Mode", "Cross", "Cross", "Dot", "Circle", "Cross + Dot"));
    private final NumberSetting gap = register(new NumberSetting("Gap", 2, 0, 8, 0.5));
    private final NumberSetting length = register(new NumberSetting("Length", 4, 1, 12, 0.5));
    private final NumberSetting thickness = register(new NumberSetting("Thickness", 1, 0.5, 4, 0.5));
    private final BooleanSetting dynamic = register(new BooleanSetting("Dynamic", true));
    private final BooleanSetting cooldownBar = register(new BooleanSetting("Cooldown Bar", true));
    private final BooleanSetting outline = register(new BooleanSetting("Outline", true));
    private final ModeSetting colorMode = register(new ModeSetting("Color Mode", "Theme", "Theme", "Custom"));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFFFFFFFF));

    public Crosshair() {
        super("Crosshair", "Свой прицел", Category.RENDER);
        color.visibleWhen(() -> colorMode.is("Custom"));
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (nullCheck() || !mc.options.getCameraType().isFirstPerson()) return;
        float cx = mc.getWindow().getGuiScaledWidth() / 2f, cy = mc.getWindow().getGuiScaledHeight() / 2f;
        float cooldown = mc.player.getAttackStrengthScale(td);
        float g = gap.getFloat() + (dynamic.isEnabled() ? (1f - cooldown) * 4f : 0f);
        float len = length.getFloat(), th = thickness.getFloat();
        int c = colorMode.is("Theme") ? Theme.get().accentA() : color.getArgb();

        boolean cross = mode.is("Cross") || mode.is("Cross + Dot");
        boolean dot = mode.is("Dot") || mode.is("Cross + Dot");

        if (cross) {
            bar(ctx, cx - th / 2f, cy - g - len, th, len, c);
            bar(ctx, cx - th / 2f, cy + g, th, len, c);
            bar(ctx, cx - g - len, cy - th / 2f, len, th, c);
            bar(ctx, cx + g, cy - th / 2f, len, th, c);
        }
        if (dot) {
            if (outline.isEnabled()) RenderUtils.drawCircle(ctx, cx, cy, th + 1f, 0xC0000000);
            RenderUtils.drawCircle(ctx, cx, cy, th, c);
        }
        if (mode.is("Circle")) {
            float r = g + len / 2f + 1;
            if (outline.isEnabled()) RenderUtils.drawRoundedOutline(ctx, cx - r - 0.5f, cy - r - 0.5f, r * 2 + 1, r * 2 + 1, r + 0.5f, th + 1, 0xC0000000);
            RenderUtils.drawRoundedOutline(ctx, cx - r, cy - r, r * 2, r * 2, r, th, c);
        }
        if (cooldownBar.isEnabled() && cooldown < 1f) {
            float w = 16;
            RenderUtils.drawRoundedRect(ctx, cx - w / 2f, cy + g + len + 4, w, 2, 1, 0x90000000);
            RenderUtils.drawRoundedRect(ctx, cx - w / 2f, cy + g + len + 4, w * cooldown, 2, 1, ColorUtils.withAlpha(c, 230));
        }
    }

    private void bar(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int c) {
        if (outline.isEnabled()) RenderUtils.drawRect(ctx, x - 0.5f, y - 0.5f, w + 1, h + 1, 0xC0000000);
        RenderUtils.drawRect(ctx, x, y, w, h, c);
    }
}
