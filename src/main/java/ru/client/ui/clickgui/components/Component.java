package ru.client.ui.clickgui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.Fonts;

public abstract class Component {

    protected static final Minecraft mc = Minecraft.getInstance();

    protected float x, y, width;

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract float getHeight();

    public boolean isVisible() { return true; }

    public abstract void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta);

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public void mouseReleased(double mx, double my, int button) {}
    public boolean keyPressed(int key, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char chr, int modifiers) { return false; }

    public boolean isTyping() { return false; }

    private static boolean flat;

    public static void setFlat(boolean value) { flat = value; }

    protected static boolean flat() { return flat; }

    protected static int accent() {
        Theme t = Theme.get();
        return flat ? t.accentSolid() : t.accentA();
    }

    protected static int accent2() {
        Theme t = Theme.get();
        return flat ? t.accentSolid() : t.accentB();
    }

    protected static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    protected static Font font() { return mc.font; }

    protected static void text(GuiGraphicsExtractor ctx, String s, float x, float y, int color) {
        Fonts.draw(ctx, s, Math.round(x), Math.round(y), color, false);
    }

    protected static void textRight(GuiGraphicsExtractor ctx, String s, float rightX, float y, int color) {
        text(ctx, s, rightX - Fonts.width(s), y, color);
    }

    protected static void label(GuiGraphicsExtractor ctx, String s, float x, float rightLimit, float y, int color) {
        text(ctx, Fonts.trim(s, (int) Math.max(8, rightLimit - x - 4)), x, y, color);
    }
}
