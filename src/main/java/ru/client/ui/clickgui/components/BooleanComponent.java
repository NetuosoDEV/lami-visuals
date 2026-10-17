package ru.client.ui.clickgui.components;

import ru.client.ui.UiSounds;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class BooleanComponent extends SettingComponent<BooleanSetting> {

    private final Animation anim;

    public BooleanComponent(BooleanSetting setting) {
        super(setting);
        anim = new Animation(14f, setting.isEnabled() ? 1f : 0f);
    }

    @Override public float getHeight() { return 14; }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        anim.setTarget(setting.isEnabled() ? 1f : 0f);
        float a = anim.update();

        label(ctx, setting.getDisplayName(), x + 4, x + width - 24, y + 3, ColorUtils.lerp(t.textDim(), t.text(), a));

        float sw = 16, sh = 8, sx = x + width - sw - 4, sy = y + 3;
        RenderUtils.drawRoundedRect(ctx, sx, sy, sw, sh, 4, ColorUtils.lerp(0xFF2A2A33, accent(), a));
        RenderUtils.drawCircle(ctx, sx + 4 + (sw - 8) * a, sy + 4, 3, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && inside(mx, my, x, y, width, getHeight())) {
            setting.toggle();
            UiSounds.toggle(setting.isEnabled());
            return true;
        }
        return false;
    }
}
