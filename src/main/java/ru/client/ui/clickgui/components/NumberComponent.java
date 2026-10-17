package ru.client.ui.clickgui.components;

import ru.client.utils.render.Fonts;
import ru.client.ui.UiSounds;

import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.RenderUtils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class NumberComponent extends SettingComponent<NumberSetting> {

    private final Animation fill = new Animation(18f, 0f);
    private final int decimals;
    private boolean dragging;
    private double soundValue;

    public NumberComponent(NumberSetting setting) {
        super(setting);
        double step = setting.getStep();
        this.decimals = step >= 1 || step <= 0 ? 0 : (int) Math.ceil(-Math.log10(step));
        fill.snap((float) percent());
    }

    private double percent() {
        double range = setting.getMax() - setting.getMin();
        return range <= 0 ? 0 : (setting.get() - setting.getMin()) / range;
    }

    @Override public float getHeight() { return 22; }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        float bx = x + 4, bw = width - 8, by = y + 15;

        if (dragging) {
            double pct = Mth.clamp((mx - bx) / bw, 0, 1);
            setting.set(setting.getMin() + (setting.getMax() - setting.getMin()) * pct);
            if (setting.get() != soundValue) {
                soundValue = setting.get();
                UiSounds.sliderStep((float) percent());
            }
        }

        fill.setTarget((float) percent());
        float f = fill.update();

        String value = String.format(Locale.ROOT, "%." + decimals + "f", setting.get());
        label(ctx, setting.getDisplayName(), x + 4, x + width - 4 - Fonts.width(value), y + 3, t.textDim());
        textRight(ctx, value, x + width - 4, y + 3, t.text());

        RenderUtils.drawRoundedRect(ctx, bx, by, bw, 3, 1.5f, 0xFF2A2A33);
        RenderUtils.drawRoundedHorizontalGradient(ctx, bx, by, bw * f, 3, 1.5f, accent(), accent2());
        RenderUtils.drawCircle(ctx, bx + bw * f, by + 1.5f, dragging ? 3.5f : 3f, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && inside(mx, my, x, y, width, getHeight())) {
            dragging = true;
            soundValue = setting.get();
            UiSounds.sliderGrab();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        if (dragging) UiSounds.sliderRelease();
        dragging = false;
    }
}
