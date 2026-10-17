package ru.client.ui.clickgui.legacy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.ui.clickgui.components.Component;
import ru.client.ui.UiSounds;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

final class LegacyPanel {

    static final float WIDTH = 112, HEADER = 20, RADIUS = 6;

    private final java.util.function.Supplier<String> title;
    private final List<Component> components = new ArrayList<>();
    private final Animation openAnim = new Animation(12f, 1f);

    private float x, y, renderY;
    private boolean open = true, dragging;
    private float dragX, dragY;

    LegacyPanel(java.util.function.Supplier<String> title) { this.title = title; }

    void add(Component c) { components.add(c); }

    void setPosition(float x, float y) { this.x = x; this.y = y; }

    void scroll(float dy) { y += dy; }

    private float contentHeight() {
        float h = 0;
        for (Component c : components) if (c.isVisible()) h += c.getHeight();
        return h;
    }

    void render(GuiGraphicsExtractor ctx, int mx, int my, float delta, float slide) {
        if (dragging) {
            x = mx - dragX;
            y = my - dragY;
        }
        Theme t = Theme.get();
        float anim = openAnim.update();
        float py = y + slide;
        renderY = py;

        float content = contentHeight() * anim;
        float bottomPad = 3 * anim;
        float total = HEADER + content + bottomPad;
        int a1 = t.accentA(), a2 = t.accentB();

        RenderUtils.drawGlow(ctx, x, py, WIDTH, total, RADIUS, 5, ColorUtils.withAlpha(a1, 90));
        RenderUtils.drawRoundedRect(ctx, x, py, WIDTH, total, RADIUS, t.panel());
        RenderUtils.drawRoundedRect(ctx, x, py, WIDTH, HEADER, RADIUS, t.header());
        if (anim > 0.02f) {
            RenderUtils.drawRect(ctx, x, py + HEADER / 2f, WIDTH, HEADER / 2f, t.header());
            RenderUtils.drawHorizontalGradient(ctx, x, py + HEADER - 1, WIDTH, 1, a1, a2);
        }

        String title = this.title.get();
        Fonts.draw(ctx, title, Math.round(x + WIDTH / 2f - Fonts.width(title) / 2f), Math.round(py + 6), t.text());

        if (content < 0.5f) return;

        float top = py + HEADER;
        ctx.enableScissor((int) x, (int) top, (int) Math.ceil(x + WIDTH), (int) Math.ceil(top + content));

        boolean mouseIn = mx >= x && mx <= x + WIDTH && my >= top && my <= top + content;
        int cmx = mouseIn ? mx : -10000, cmy = mouseIn ? my : -10000;
        float cy = top;
        for (Component c : components) {
            if (!c.isVisible()) continue;
            c.setBounds(x, cy, WIDTH);
            c.render(ctx, cmx, cmy, delta);
            cy += c.getHeight();
        }

        ctx.disableScissor();
    }

    private boolean overHeader(double mx, double my) {
        return mx >= x && mx <= x + WIDTH && my >= renderY && my <= renderY + HEADER;
    }

    boolean isOver(double mx, double my) {
        float h = HEADER + (open ? contentHeight() + 3 : 0);
        return mx >= x && mx <= x + WIDTH && my >= renderY && my <= renderY + h;
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (overHeader(mx, my)) {
            if (button == 0) {
                dragging = true;
                dragX = (float) (mx - x);
                dragY = (float) (my - y);
            } else if (button == 1) {
                open = !open;
                UiSounds.dropdown(open);
                openAnim.setTarget(open ? 1f : 0f);
            }
            return true;
        }
        if (!open) return false;
        float top = renderY + HEADER;
        if (mx < x || mx > x + WIDTH || my < top || my > top + contentHeight()) return false;
        for (Component c : components) {
            if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
        }
        return true;
    }

    void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (Component c : components) c.mouseReleased(mx, my, button);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        for (Component c : components) if (c.isVisible() && c.keyPressed(key, scanCode, modifiers)) return true;
        return false;
    }

    boolean typing() {
        for (Component c : components) if (c.isVisible() && c.isTyping()) return true;
        return false;
    }

    boolean charTyped(char chr, int modifiers) {
        for (Component c : components) if (c.isVisible() && c.charTyped(chr, modifiers)) return true;
        return false;
    }
}
