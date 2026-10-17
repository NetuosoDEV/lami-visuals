package ru.client.ui.clickgui.components;

import ru.client.utils.render.Fonts;
import ru.client.ui.UiSounds;

import ru.client.modules.settings.ModeSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ModeComponent extends SettingComponent<ModeSetting> {

    private static final float ROW = 14, ITEM = 12;

    private boolean open;
    private final Animation openAnim = new Animation(14f, 0f);

    public ModeComponent(ModeSetting setting) { super(setting); }

    private boolean dropdown() { return setting.getModes().size() > 2; }

    @Override
    public float getHeight() {
        return ROW + (dropdown() ? (setting.getModes().size() * ITEM + 4) * openAnim.get() : 0);
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        openAnim.setTarget(open ? 1f : 0f);
        float a = openAnim.update();
        float right = x + width - 4;
        if (dropdown()) {
            float cx = right - 3, cy = y + 7, d = 2.2f * (1f - 2f * a);
            RenderUtils.drawLine(ctx, cx - 2.5f, cy - d / 2f, cx, cy + d / 2f, 1.1f, t.textDim());
            RenderUtils.drawLine(ctx, cx, cy + d / 2f, cx + 2.5f, cy - d / 2f, 1.1f, t.textDim());
            right -= 10;
        }
        String value = setting.displayValue();
        label(ctx, setting.getDisplayName(), x + 4, right - Fonts.width(value), y + 3, t.textDim());
        textRight(ctx, value, right, y + 3, accent());
        if (!dropdown() || a < 0.01f) return;

        float listH = (setting.getModes().size() * ITEM + 4) * a;
        float ly = y + ROW;
        RenderUtils.drawRoundedRect(ctx, x + 2, ly, width - 4, listH, 4, 0xFF101016);
        ctx.enableScissor((int) x, (int) ly, (int) Math.ceil(x + width), (int) Math.ceil(ly + listH));
        List<String> modes = setting.getModes();
        for (int i = 0; i < modes.size(); i++) {
            float iy = ly + 2 + i * ITEM;
            String m = modes.get(i);
            boolean sel = setting.is(m);
            boolean hover = inside(mx, my, x + 2, iy, width - 4, ITEM);
            if (sel) RenderUtils.drawRoundedHorizontalGradient(ctx, x + 4, iy, width - 8, ITEM, 3,
                    ColorUtils.withAlpha(accent(), 80), ColorUtils.withAlpha(accent2(), flat() ? 80 : 30));
            else if (hover) RenderUtils.drawRoundedRect(ctx, x + 4, iy, width - 8, ITEM, 3, 0x14FFFFFF);
            text(ctx, Fonts.trim(setting.display(m), (int) (width - 16)), x + 9, iy + 2, sel ? t.text() : ColorUtils.lerp(t.textDim(), t.text(), hover ? 0.6f : 0f));
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, x, y, width, ROW)) {
            if (dropdown() && button == 0) {
                open = !open;
                UiSounds.dropdown(open);
            } else if (button == 0 || button == 1) {
                UiSounds.click();
                setting.cycle(button == 0);
            }
            return true;
        }
        if (dropdown() && open && openAnim.get() > 0.9f && button == 0) {
            List<String> modes = setting.getModes();
            for (int i = 0; i < modes.size(); i++) {
                float iy = y + ROW + 2 + i * ITEM;
                if (inside(mx, my, x + 2, iy, width - 4, ITEM)) {
                    UiSounds.click();
                    setting.set(modes.get(i));
                    open = false;
                    return true;
                }
            }
        }
        return false;
    }
}
