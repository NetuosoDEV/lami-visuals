package ru.client.ui.clickgui.legacy;

import ru.client.core.I18n;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.modules.Module;
import ru.client.modules.settings.Setting;
import ru.client.ui.clickgui.components.BindComponent;
import ru.client.ui.clickgui.components.Component;
import ru.client.ui.clickgui.components.SettingComponents;
import ru.client.ui.UiSounds;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

final class LegacyModuleButton extends Component {

    private static final float ROW = 16;

    private final Module module;
    private final List<Component> children = new ArrayList<>();
    private final Animation expandAnim = new Animation(12f, 0f);
    private final Animation toggleAnim;
    private final Animation hoverAnim = new Animation(14f, 0f);
    private boolean expanded;

    LegacyModuleButton(Module module) {
        this.module = module;
        for (Setting<?> s : module.getSettings()) children.add(SettingComponents.create(s));
        children.add(new BindComponent(() -> I18n.tr("Клавиша"), module::getKey, module::setKey));
        toggleAnim = new Animation(10f, module.isToggled() ? 1f : 0f);
    }

    private float childrenHeight() {
        float h = 0;
        for (Component c : children) if (c.isVisible()) h += c.getHeight();
        return h;
    }

    @Override
    public float getHeight() { return ROW + childrenHeight() * expandAnim.get(); }

    @Override
    public void render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Theme t = Theme.get();
        boolean hover = inside(mx, my, x, y, width, ROW);
        hoverAnim.setTarget(hover ? 1f : 0f);
        toggleAnim.setTarget(module.isToggled() ? 1f : 0f);
        float ha = hoverAnim.update(), ta = toggleAnim.update(), ea = expandAnim.update();

        if (ha > 0.01f) RenderUtils.drawRect(ctx, x, y, width, ROW, ColorUtils.withAlpha(0xFFFFFF, (int) (14 * ha)));
        if (ta > 0.01f) {
            RenderUtils.drawRoundedHorizontalGradient(ctx, x + 3, y + 1.5f, width - 6, ROW - 3, 3,
                    ColorUtils.multAlpha(t.accentA(), ta * 0.85f), ColorUtils.multAlpha(t.accentB(), ta * 0.85f));
        }

        int textColor = ColorUtils.lerp(t.textDim(), t.text(), Math.max(ta, ha * 0.6f));
        text(ctx, module.getDisplayName(), x + 8, y + 4, textColor);
        text(ctx, expanded ? "-" : "+", x + width - 12, y + 4, ColorUtils.lerp(t.textDim(), t.text(), ta));

        if (hover) LegacyGui.setTooltip(module.getDisplayDescription());

        if (ea < 0.01f) return;

        float ch = childrenHeight() * ea;
        RenderUtils.drawRect(ctx, x, y + ROW, width, ch, t.settingBg());
        RenderUtils.drawVerticalGradient(ctx, x + 2, y + ROW + 1, 1, Math.max(0, ch - 2), t.accentA(), t.accentB());

        boolean clip = ea < 0.999f;
        if (clip) ctx.enableScissor((int) x, (int) (y + ROW), (int) Math.ceil(x + width), (int) Math.ceil(y + ROW + ch));

        boolean mouseInChildren = inside(mx, my, x, y + ROW, width, ch);
        int cmx = mouseInChildren ? mx : -10000, cmy = mouseInChildren ? my : -10000;
        float cy = y + ROW;
        for (Component c : children) {
            if (!c.isVisible()) continue;
            c.setBounds(x + 4, cy, width - 6);
            c.render(ctx, cmx, cmy, delta);
            cy += c.getHeight();
        }

        if (clip) ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, x, y, width, ROW)) {
            if (button == 0) {
                module.toggle();
                UiSounds.toggle(module.isToggled());
            } else if (button == 1) {
                expanded = !expanded;
                UiSounds.dropdown(expanded);
                expandAnim.setTarget(expanded ? 1f : 0f);
            }
            return true;
        }
        if (!expanded || expandAnim.get() < 0.9f) return false;
        for (Component c : children) {
            if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        for (Component c : children) c.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.keyPressed(key, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean isTyping() {
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.isTyping()) return true;
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.charTyped(chr, modifiers)) return true;
        return false;
    }
}
