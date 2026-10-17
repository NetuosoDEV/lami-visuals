package ru.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import ru.client.utils.animation.Animation;

public abstract class Page {

    protected final Ui.Hits hits = new Ui.Hits();
    private final Animation scrollAnim = new Animation(16f, 0f);
    private float scrollTarget;

    public abstract void render(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int mx, int my);

    public boolean mouseClicked(double mx, double my, int button) {
        return button == 0 && hits.click(mx, my);
    }

    public boolean keyPressed(int key, boolean ctrl) { return false; }

    public boolean charTyped(char c) { return false; }

    public boolean typing() { return false; }

    public void scroll(double vertical) { scrollTarget -= (float) vertical * 22f; }

    protected float scroll(float content, float view) {
        scrollTarget = Mth.clamp(scrollTarget, 0, Math.max(0, content - view));
        scrollAnim.setTarget(scrollTarget);
        return scrollAnim.update();
    }

    protected void resetScroll() {
        scrollTarget = 0;
        scrollAnim.snap(0);
    }
}
