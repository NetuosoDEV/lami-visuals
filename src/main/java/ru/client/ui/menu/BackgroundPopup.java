package ru.client.ui.menu;

import ru.client.core.I18n;
import ru.client.ui.UiSounds;

import ru.client.modules.Modules;
import ru.client.modules.impl.utility.MainMenu;
import ru.client.modules.settings.Setting;
import ru.client.ui.clickgui.components.Component;
import ru.client.ui.clickgui.components.SettingComponents;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

final class BackgroundPopup {

    private static final float W = 260, CHIP_H = 18, GAP = 5, PAD = 12;

    private final LamiMainMenu menu;
    private final List<Component> comps = new ArrayList<>();
    private final Animation anim = new Animation(10f, 0f);
    private final Animation[] chipHover;
    private boolean closing;
    private float x, y, h;

    BackgroundPopup(LamiMainMenu menu) {
        this.menu = menu;
        MainMenu m = Modules.get(MainMenu.class);
        if (m != null) {
            for (Setting<?> s : m.getSettings()) {
                if (s == m.background) continue;
                comps.add(SettingComponents.create(s));
            }
        }
        chipHover = new Animation[MainMenu.BACKGROUNDS.length];
        for (int i = 0; i < chipHover.length; i++) chipHover[i] = new Animation(12f, 0f);
        anim.setTarget(1f);
        layout();
    }

    void layout() {
        x = menu.width / 2f - W / 2f;
        h = computeHeight();
        y = Math.max(8, menu.height / 2f - h / 2f);
    }

    private int cols() { return 3; }

    private float chipsHeight() {
        int rows = (MainMenu.BACKGROUNDS.length + cols() - 1) / cols();
        return rows * CHIP_H + (rows - 1) * GAP;
    }

    private float compsHeight() {
        float ch = 0;
        for (Component c : comps) if (c.isVisible()) ch += c.getHeight();
        return ch;
    }

    private float computeHeight() { return 30 + chipsHeight() + 14 + compsHeight() + 10 + 18 + PAD; }

    void close() {
        closing = true;
        anim.setTarget(0f);
    }

    boolean render(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float a = anim.update();
        if (closing && a < 0.02f) return true;
        Theme t = Theme.get();
        MainMenu m = Modules.get(MainMenu.class);
        float target = computeHeight();
        h += (target - h) * 0.3f;
        y = Math.max(8, menu.height / 2f - h / 2f);

        RenderUtils.drawRect(ctx, 0, 0, menu.width, menu.height, ColorUtils.withAlpha(0xFF000000, (int) (120 * a)));
        ctx.pose().pushMatrix();
        float s = 0.92f + 0.08f * a;
        ctx.pose().translate(menu.width / 2f, menu.height / 2f);
        ctx.pose().scale(s, s);
        ctx.pose().translate(-menu.width / 2f, -menu.height / 2f);

        RenderUtils.drawGlow(ctx, x, y, W, h, 10, 8, ColorUtils.withAlpha(t.accentA(), (int) (70 * a)));
        RenderUtils.drawRoundedRect(ctx, x, y, W, h, 10, ColorUtils.withAlpha(0xFF0F0F17, (int) (245 * a)));
        RenderUtils.drawRoundedOutline(ctx, x, y, W, h, 10, 1, ColorUtils.withAlpha(t.accentA(), (int) (80 * a)));
        Fonts.drawBold(ctx, I18n.tr("Фон главного меню"), x + PAD, y + 10, ColorUtils.multAlpha(t.text(), a));
        RenderUtils.drawRoundedHorizontalGradient(ctx, x + PAD, y + 23, W - PAD * 2, 1, 0.5f,
                ColorUtils.multAlpha(t.accentA(), a), ColorUtils.multAlpha(t.accentB(), a));

        float cw = (W - PAD * 2 - GAP * (cols() - 1)) / cols();
        float cy0 = y + 30;
        for (int i = 0; i < MainMenu.BACKGROUNDS.length; i++) {
            String name = MainMenu.BACKGROUNDS[i];
            float cx = x + PAD + (i % cols()) * (cw + GAP), cy = cy0 + (i / cols()) * (CHIP_H + GAP);
            boolean sel = m != null && m.background.is(name);
            boolean hov = !closing && mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CHIP_H;
            chipHover[i].setTarget(hov ? 1f : 0f);
            float ha = chipHover[i].update();
            if (sel) {
                RenderUtils.drawRoundedHorizontalGradient(ctx, cx, cy, cw, CHIP_H, CHIP_H / 2f,
                        ColorUtils.multAlpha(t.accentA(), a), ColorUtils.multAlpha(t.accentB(), a));
            } else {
                RenderUtils.drawRoundedRect(ctx, cx, cy, cw, CHIP_H, CHIP_H / 2f,
                        ColorUtils.multAlpha(ColorUtils.lerp(0xFF1A1A24, 0xFF252533, ha), a));
            }
            int tc = sel ? 0xFFFFFFFF : ColorUtils.lerp(t.textDim(), t.text(), ha);
            Fonts.drawCentered(ctx, name, cx + cw / 2f, cy + CHIP_H / 2f - 4, ColorUtils.multAlpha(tc, a), sel ? Fonts.BOLD : Fonts.REGULAR);
        }

        float sy = cy0 + chipsHeight() + 10;
        RenderUtils.drawRect(ctx, x + PAD, sy, W - PAD * 2, 1, ColorUtils.withAlpha(0xFFFFFFFF, (int) (24 * a)));
        sy += 4;
        int cmx = closing ? -10000 : mx, cmy = closing ? -10000 : my;
        ctx.enableScissor((int) x, (int) sy, (int) (x + W), (int) (y + h - 30));
        for (Component c : comps) {
            if (!c.isVisible()) continue;
            c.setBounds(x + PAD - 5, sy, W - PAD * 2 + 10);
            c.render(ctx, cmx, cmy, delta);
            sy += c.getHeight();
        }
        ctx.disableScissor();

        float bw = 80, bh = 18, bx = x + W / 2f - bw / 2f, by = y + h - PAD - bh;
        boolean bhov = !closing && mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        RenderUtils.drawRoundedHorizontalGradient(ctx, bx, by, bw, bh, bh / 2f,
                ColorUtils.multAlpha(ColorUtils.lerp(t.accentA(), 0xFFFFFFFF, bhov ? 0.15f : 0f), a),
                ColorUtils.multAlpha(ColorUtils.lerp(t.accentB(), 0xFFFFFFFF, bhov ? 0.15f : 0f), a));
        Fonts.drawCentered(ctx, I18n.tr("Готово"), bx + bw / 2f, by + bh / 2f - 4, ColorUtils.multAlpha(0xFFFFFFFF, a), Fonts.BOLD);

        ctx.pose().popMatrix();
        return false;
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        if (mx < x || mx > x + W || my < y || my > y + h) return false;
        MainMenu m = Modules.get(MainMenu.class);
        float cw = (W - PAD * 2 - GAP * (cols() - 1)) / cols();
        float cy0 = y + 30;
        for (int i = 0; i < MainMenu.BACKGROUNDS.length; i++) {
            float cx = x + PAD + (i % cols()) * (cw + GAP), cy = cy0 + (i / cols()) * (CHIP_H + GAP);
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CHIP_H) {
                if (m != null) m.background.set(MainMenu.BACKGROUNDS[i]);
                click();
                return true;
            }
        }
        float bw = 80, bh = 18, bx = x + W / 2f - bw / 2f, by = y + h - PAD - bh;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            click();
            close();
            return true;
        }
        for (Component c : comps) if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
        return true;
    }

    void mouseReleased(double mx, double my, int button) {
        for (Component c : comps) c.mouseReleased(mx, my, button);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        for (Component c : comps) if (c.isVisible() && c.keyPressed(key, scanCode, modifiers)) return true;
        return false;
    }

    private static void click() {
        UiSounds.click();
    }
}
