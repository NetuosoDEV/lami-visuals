package ru.client.ui.clickgui;

import ru.client.core.I18n;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.Setting;
import ru.client.ui.UiSounds;
import ru.client.ui.clickgui.components.BindComponent;
import ru.client.ui.clickgui.components.Component;
import ru.client.ui.clickgui.components.SettingComponents;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ModuleCard {

    static final float HEAD = 40, RADIUS = 6, PAD = 12, GAP = 2;
    private static final float ROW_Y = 13.5f;

    private static ModuleCard listening;

    final Module module;
    private final boolean alwaysOn;
    private final List<Component> children = new ArrayList<>();
    private final Animation expandAnim;
    private final Animation toggleAnim;
    private final Animation hoverAnim = new Animation(16f, 0f);
    private boolean expanded;
    private boolean descTrimmed;

    float x, y, w;

    private float chipX, chipW, chevX = Float.NaN;

    ModuleCard(Module module) {
        this.module = module;
        this.alwaysOn = module.getCategory() == Category.COLORS;
        for (Setting<?> s : module.getSettings()) children.add(SettingComponents.create(s));
        expanded = alwaysOn;
        expandAnim = new Animation(14f, expanded ? 1f : 0f);
        toggleAnim = new Animation(14f, module.isToggled() ? 1f : 0f);
    }

    private boolean hasSettings() {
        for (Component c : children) if (c.isVisible()) return true;
        return false;
    }

    private float childrenHeight() {
        float h = 0;
        for (Component c : children) if (c.isVisible()) h += c.getHeight() + GAP;
        return h + 12;
    }

    float height() { return HEAD + childrenHeight() * expandAnim.get(); }

    void tickAnim() { expandAnim.update(); }

    boolean descriptionTrimmed() { return descTrimmed; }

    boolean isListening() { return listening == this; }

    static void stopListening() { listening = null; }

    void render(GuiGraphicsExtractor ctx, int mx, int my, float delta, boolean mouseInView) {
        boolean hover = mouseInView && Ui.inside(mx, my, x, y, w, HEAD);
        hoverAnim.setTarget(hover ? 1f : 0f);
        toggleAnim.setTarget(alwaysOn || module.isToggled() ? 1f : 0f);
        float ha = hoverAnim.update(), ta = toggleAnim.update(), ea = expandAnim.get();
        float h = height();

        RenderUtils.drawRoundedRect(ctx, x, y, w, h, RADIUS, ColorUtils.lerp(Ui.surface(), Ui.surfaceHover(), ha));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, RADIUS, 1, ColorUtils.lerp(Ui.LINE, Ui.LINE_HI, Math.max(ha, ea * 0.6f)));

        float right = x + w - PAD, cy = y + ROW_Y;
        if (!alwaysOn) {
            float sw = 22, sh = 12;
            Ui.toggle(ctx, right - sw, cy - sh / 2f, sw, sh, ta);
            right -= sw + 6;
            if (hasSettings()) {
                float cs = 16;
                chevX = right - cs;
                boolean onChev = mouseInView && Ui.inside(mx, my, chevX, cy - cs / 2f, cs, cs);
                if (onChev) RenderUtils.drawRoundedRect(ctx, chevX, cy - cs / 2f, cs, cs, 4, 0x14FFFFFF);
                Icons.draw(ctx, Icons.Glyph.CHEVRON, chevX + cs / 2f, cy, onChev || ea > 0.5f ? Ui.text() : Ui.dim(), ea);
                right = chevX - 4;
            } else {
                chevX = Float.NaN;
            }
            int key = module.getKey();
            boolean bound = key != -1, wait = isListening();
            String label = wait ? "..." : bound ? BindComponent.keyName(key) : I18n.tr("Клавиша");
            chipW = Fonts.width(label) + 10;
            chipX = right - chipW;
            if (bound || wait || hover) {
                float chH = 13, chY = cy - chH / 2f;
                boolean onChip = mouseInView && Ui.inside(mx, my, chipX, chY, chipW, chH);
                RenderUtils.drawRoundedRect(ctx, chipX, chY, chipW, chH, 3, wait ? ColorUtils.withAlpha(Ui.accent(), 60) : onChip ? 0x16FFFFFF : 0x0AFFFFFF);
                RenderUtils.drawRoundedOutline(ctx, chipX, chY, chipW, chH, 3, 1, wait ? ColorUtils.withAlpha(Ui.accent(), 180) : Ui.LINE);
                Fonts.draw(ctx, label, chipX + 5, cy - 4, wait ? Ui.text() : bound ? Ui.dim() : Ui.faint());
            }
            if (bound || wait) right = chipX - 6;
        }

        float tx = x + PAD;
        int nameColor = alwaysOn ? Ui.text() : ColorUtils.lerp(ColorUtils.lerp(Ui.dim(), Ui.text(), 0.5f + 0.25f * ha), Ui.text(), ta);
        float nameMax = (alwaysOn ? x + w - PAD : right) - tx;
        Fonts.drawBold(ctx, Fonts.trim(module.getDisplayName(), (int) nameMax, Fonts.BOLD), tx, y + 9, nameColor);
        String desc = module.getDisplayDescription();
        String shown = Fonts.trim(desc, (int) (x + w - PAD - tx));
        descTrimmed = !shown.equals(desc);
        Fonts.draw(ctx, shown, tx, y + 23, Ui.dim());

        if (ea > 0.01f) {
            float top = y + HEAD, ch = h - HEAD;
            RenderUtils.drawRect(ctx, x + PAD, top, w - PAD * 2, 1, Ui.LINE);
            ctx.enableScissor((int) x, (int) top, (int) Math.ceil(x + w), (int) Math.ceil(top + ch));
            boolean mouseInChildren = mouseInView && Ui.inside(mx, my, x, top, w, ch);
            int cmx = mouseInChildren ? mx : -10000, cmy = mouseInChildren ? my : -10000;
            float yy = top + 6;
            for (Component c : children) {
                if (!c.isVisible()) continue;
                c.setBounds(x + PAD - 4, yy, w - PAD * 2 + 8);
                c.render(ctx, cmx, cmy, delta);
                yy += c.getHeight() + GAP;
            }
            ctx.disableScissor();
        }
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (Ui.inside(mx, my, x, y, w, HEAD)) {
            if (alwaysOn) return true;
            float cy = y + ROW_Y;
            if (button == 0 && Ui.inside(mx, my, chipX, cy - 7, chipW, 14)) {
                listening = isListening() ? null : this;
                Ui.click();
                return true;
            }
            listening = null;
            boolean onChev = !Float.isNaN(chevX) && Ui.inside(mx, my, chevX, cy - 8, 16, 16);
            if ((button == 1 || (button == 0 && onChev)) && hasSettings()) {
                expanded = !expanded;
                expandAnim.setTarget(expanded ? 1f : 0f);
                UiSounds.dropdown(expanded);
            } else if (button == 0) {
                module.toggle();
                UiSounds.toggle(module.isToggled());
            }
            return true;
        }
        if (isListening()) listening = null;
        if (!expanded || expandAnim.get() < 0.9f) return false;
        if (!Ui.inside(mx, my, x, y, w, height())) return false;
        for (Component c : children) {
            if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
        }
        return true;
    }

    void mouseReleased(double mx, double my, int button) {
        for (Component c : children) c.mouseReleased(mx, my, button);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        if (isListening()) {
            boolean clear = key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE;
            module.setKey(clear ? -1 : key);
            listening = null;
            return true;
        }
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.keyPressed(key, scanCode, modifiers)) return true;
        return false;
    }

    boolean charTyped(char chr, int modifiers) {
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.charTyped(chr, modifiers)) return true;
        return false;
    }

    boolean isTyping() {
        if (!expanded) return false;
        for (Component c : children) if (c.isVisible() && c.isTyping()) return true;
        return false;
    }
}
