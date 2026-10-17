package ru.client.modules;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.clickgui.ClickGuiScreen;

public abstract class HudModule extends Module {

    private final NumberSetting posX;
    private final NumberSetting posY;
    private final NumberSetting scale;

    protected float width = 60, height = 16;
    private float renderX, renderY;

    protected HudModule(String name, String description, Category category, float defaultX, float defaultY) {
        super(name, description, category);
        posX = register(new NumberSetting("X", defaultX, -1, 10000, 0.5));
        posY = register(new NumberSetting("Y", defaultY, -1, 10000, 0.5));
        scale = register(new NumberSetting("HUD Scale", 1.0, 0.5, 3.0, 0.05));
        posX.visibleWhen(() -> false);
        posY.visibleWhen(() -> false);
    }

    @Override
    public final void onRender2D(GuiGraphicsExtractor ctx, float tickDelta) {
        float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        if (posX.get() < 0 || posY.get() < 0) initPosition(sw, sh);
        float s = getScale();
        renderX = Mth.clamp(posX.getFloat(), 0, Math.max(0, sw - width * s));
        renderY = Mth.clamp(posY.getFloat(), 0, Math.max(0, sh - height * s));
        if (s == 1f) {
            renderHud(ctx, renderX, renderY, tickDelta);
            return;
        }
        ctx.pose().pushMatrix();
        ctx.pose().translate(renderX, renderY);
        ctx.pose().scale(s, s);
        renderHud(ctx, 0, 0, tickDelta);
        ctx.pose().popMatrix();
    }

    protected void initPosition(float screenW, float screenH) { setPosition(6, 6); }

    protected abstract void renderHud(GuiGraphicsExtractor ctx, float x, float y, float tickDelta);

    protected static boolean isEditing() { return mc.gui.screen() instanceof ClickGuiScreen; }

    protected boolean isRightSide() { return renderX + getWidth() / 2f > mc.getWindow().getGuiScaledWidth() / 2f; }

    public boolean isOver(double mx, double my) {
        return mx >= renderX && mx <= renderX + getWidth() && my >= renderY && my <= renderY + getHeight();
    }

    public void setPosition(float x, float y) {
        posX.set((double) x);
        posY.set((double) y);
    }

    public float getRenderX() { return renderX; }
    public float getRenderY() { return renderY; }
    public float getWidth() { return width * getScale(); }
    public float getHeight() { return height * getScale(); }

    public float getScale() { return (float) Mth.clamp(scale.get(), 0.5, 3.0); }

    public void setScale(float s) {
        s = Math.round(Mth.clamp(s, 0.5f, 3.0f) * 20f) / 20f;
        scale.set((double) s);
    }
}
