package ru.client.ui.clickgui;

import ru.client.core.I18n;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.Module;
import ru.client.modules.impl.utility.ClickGui;
import ru.client.ui.UiSounds;
import ru.client.ui.clickgui.legacy.LegacyGui;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ClickGuiScreen extends Screen {

    private static final float SNAP = 4, HANDLE = 6;

    private final ModernGui modern;
    private final LegacyGui legacy;
    private final Animation openAnim = new Animation(9f, 0f);
    private final Animation windowAnim = new Animation(10f, 1f);

    private boolean hudMode;
    private HudModule draggingHud, resizingHud;
    private float resizeStartScale, resizeStartW, resizeStartH;
    private float hudDragX, hudDragY;
    private Float guideX, guideY;

    private Screen parent;

    public ClickGuiScreen() {
        super(Component.literal("LamiVisuals"));
        modern = new ModernGui(this);
        legacy = new LegacyGui();
    }

    public void setParent(Screen parent) { this.parent = parent; }

    public static void setTooltip(String text) {}

    public void openPage(Category page) { modern.openPage(page); }

    void enterHudMode() { hudMode = true; }

    private boolean inWorld() { return minecraft != null && minecraft.level != null; }

    private static boolean modernStyle() {
        ClickGui g = ClickGui.get();
        return g == null || g.modern();
    }

    private static float uiScale() {
        ClickGui g = ClickGui.get();
        return g == null || !g.modern() ? 1f : Mth.clamp(g.scale.getFloat(), 0.5f, 2f);
    }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.setTarget(1f);
        hudMode = false;
        windowAnim.snap(1f);
        float s = uiScale();
        modern.init(width / s, height / s);
        legacy.init(width, height);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        float a = openAnim.update();
        windowAnim.setTarget(hudMode ? 0f : 1f);
        float wa = windowAnim.update();
        Theme t = Theme.get();
        ClickGui g = ClickGui.get();

        if (g != null && g.blur.isEnabled() && !hudMode && minecraft.options.getMenuBackgroundBlurriness() >= 1f) {
            ctx.blurBeforeThisStratum();
        }

        float dim = g == null ? 0.45f : g.dim.getFloat() / 100f;
        float k = inWorld() ? a * (0.35f + 0.65f * wa) : a * 0.7f;
        RenderUtils.drawRect(ctx, 0, 0, width, height, ColorUtils.rgba(0, 0, 0, (int) (255 * dim * k)));

        renderHudEditor(ctx, mouseX, mouseY, t);
        syncTextInput(!hudMode && (modernStyle() ? modern.typing() : legacy.typing()));

        if (modernStyle()) {
            if (wa > 0.5f) {
                float s = uiScale();
                ctx.pose().pushMatrix();
                ctx.pose().scale(s, s);
                modern.render(ctx, (int) (mouseX / s), (int) (mouseY / s), delta, ease(a), width / s, height / s);
                ctx.pose().popMatrix();
            }
        } else {
            legacy.render(ctx, mouseX, mouseY, delta, a, width, height);
        }

        if (hudMode) {
            String s = I18n.tr("Редактор HUD  •  уголок или колёсико — размер  •  ПКМ/Esc — назад");
            float bw = Fonts.width(s) + 20, bx = width / 2f - bw / 2f, by = 8;
            RenderUtils.drawRoundedRect(ctx, bx, by, bw, 18, Ui.RADIUS, ColorUtils.withAlpha(Ui.bg(), 240));
            RenderUtils.drawRoundedOutline(ctx, bx, by, bw, 18, Ui.RADIUS, 1, Ui.LINE_HI);
            Fonts.draw(ctx, s, bx + 10, by + 5, t.text());
        }
    }

    private void renderHudEditor(GuiGraphicsExtractor ctx, int mx, int my, Theme t) {
        if (!inWorld()) return;
        guideX = guideY = null;
        if (draggingHud != null) dragHud(mx, my);
        if (resizingHud != null) {
            float bw = resizeStartW / resizeStartScale, bh = resizeStartH / resizeStartScale;
            float k = Math.max((mx - resizingHud.getRenderX()) / Math.max(1f, bw), (my - resizingHud.getRenderY()) / Math.max(1f, bh));
            resizingHud.setScale(k);
        }
        if (guideX != null) RenderUtils.drawRect(ctx, guideX - 0.5f, 0, 1, height, ColorUtils.withAlpha(t.accentB(), 170));
        if (guideY != null) RenderUtils.drawRect(ctx, 0, guideY - 0.5f, width, 1, ColorUtils.withAlpha(t.accentB(), 170));
        boolean overUi = !hudMode && overUi(mx, my);
        for (Module m : ClientMod.getInstance().getModuleManager().getModules()) {
            if (!(m instanceof HudModule hud) || !m.isToggled()) continue;
            boolean hover = hud == draggingHud || (!overUi && hud.isOver(mx, my));
            RenderUtils.drawRoundedOutline(ctx, hud.getRenderX() - 2, hud.getRenderY() - 2, hud.getWidth() + 4, hud.getHeight() + 4, 4, 1,
                    ColorUtils.withAlpha(t.accentA(), hover ? 230 : (hudMode ? 140 : 70)));
            if (hover || hud == resizingHud) {
                float hx = hud.getRenderX() + hud.getWidth() - HANDLE / 2f + 2, hy = hud.getRenderY() + hud.getHeight() - HANDLE / 2f + 2;
                boolean onHandle = hud == resizingHud || overHandle(hud, mx, my);
                RenderUtils.drawRoundedRect(ctx, hx - HANDLE / 2f, hy - HANDLE / 2f, HANDLE, HANDLE, 1.5f,
                        onHandle ? t.accentB() : ColorUtils.withAlpha(t.accentA(), 220));
                String s = hud.getDisplayName() + "  " + Math.round(hud.getScale() * 100) + "%";
                Fonts.draw(ctx, s, hud.getRenderX(), Math.max(0, hud.getRenderY() - 12), t.text(), true);
            }
        }
    }

    private void dragHud(int mx, int my) {
        HudModule h = draggingHud;
        float nx = mx - hudDragX, ny = my - hudDragY, w = h.getWidth(), hh = h.getHeight();
        boolean free = minecraft.hasShiftDown();
        if (!free) {
            float[] xs = {width / 2f, 4, width - 4};
            float[] ys = {height / 2f, 4, height - 4};
            if (Math.abs(nx + w / 2f - xs[0]) < SNAP) { nx = xs[0] - w / 2f; guideX = xs[0]; }
            else if (Math.abs(nx - xs[1]) < SNAP) { nx = xs[1]; guideX = xs[1]; }
            else if (Math.abs(nx + w - xs[2]) < SNAP) { nx = xs[2] - w; guideX = xs[2]; }
            if (Math.abs(ny + hh / 2f - ys[0]) < SNAP) { ny = ys[0] - hh / 2f; guideY = ys[0]; }
            else if (Math.abs(ny - ys[1]) < SNAP) { ny = ys[1]; guideY = ys[1]; }
            else if (Math.abs(ny + hh - ys[2]) < SNAP) { ny = ys[2] - hh; guideY = ys[2]; }
            for (Module m : ClientMod.getInstance().getModuleManager().getModules()) {
                if (!(m instanceof HudModule o) || o == h || !o.isToggled()) continue;
                if (guideX == null) {
                    if (Math.abs(nx - o.getRenderX()) < SNAP) { nx = o.getRenderX(); guideX = nx; }
                    else if (Math.abs(nx + w - (o.getRenderX() + o.getWidth())) < SNAP) { nx = o.getRenderX() + o.getWidth() - w; guideX = nx + w; }
                }
                if (guideY == null) {
                    if (Math.abs(ny - (o.getRenderY() + o.getHeight() + 3)) < SNAP) { ny = o.getRenderY() + o.getHeight() + 3; guideY = ny; }
                    else if (Math.abs(ny - o.getRenderY()) < SNAP) { ny = o.getRenderY(); guideY = ny; }
                }
            }
        }
        h.setPosition(Mth.clamp(nx, 0, Math.max(0, width - w)), Mth.clamp(ny, 0, Math.max(0, height - hh)));
    }

    private static boolean overHandle(HudModule hud, double mx, double my) {
        float hx = hud.getRenderX() + hud.getWidth() + 2, hy = hud.getRenderY() + hud.getHeight() + 2;
        return Math.abs(mx - hx) <= HANDLE && Math.abs(my - hy) <= HANDLE;
    }

    private HudModule hudAt(double mx, double my) {
        if (!inWorld()) return null;
        List<Module> all = ClientMod.getInstance().getModuleManager().getModules();
        for (int i = all.size() - 1; i >= 0; i--)
            if (all.get(i) instanceof HudModule hud && hud.isToggled() && hud.isOver(mx, my)) return hud;
        return null;
    }

    private boolean overUi(double mx, double my) {
        if (modernStyle()) {
            float s = uiScale();
            return modern.overWindow(mx / s, my / s);
        }
        return legacy.isOver(mx, my);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (inWorld()) return;
        if (parent instanceof ru.client.ui.menu.LamiMainMenu menu) {
            if (menu.width != width || menu.height != height) menu.resize(width, height);
            menu.renderBackdrop(ctx, mouseX, mouseY);
        } else {
            extractPanorama(ctx, delta);
        }
    }

    private void syncTextInput(boolean typing) {
        if (minecraft == null) return;
        if (typing) minecraft.textInputManager().startTextInput(this);
        else minecraft.textInputManager().stopTextInput(this);
    }

    @Override
    public void added() {
        super.added();
        UiSounds.play(UiSounds.Sfx.MENU_OPEN);
    }

    @Override
    public void removed() {
        syncTextInput(false);
        UiSounds.sliderRelease();
        UiSounds.play(UiSounds.Sfx.MENU_CLOSE);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public boolean onMouseClicked(double mx, double my, int button) {
        if (hudMode) {
            if (button == 1) { hudMode = false; return true; }
            return tryGrabHud(mx, my, button);
        }
        if (modernStyle()) {
            float s = uiScale();
            if (modern.mouseClicked(mx / s, my / s, button)) return true;
        } else if (legacy.mouseClicked(mx, my, button)) {
            return true;
        }
        return tryGrabHud(mx, my, button);
    }

    private boolean tryGrabHud(double mx, double my, int button) {
        if (button != 0 || !inWorld()) return false;
        List<Module> all = ClientMod.getInstance().getModuleManager().getModules();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i) instanceof HudModule hud && hud.isToggled() && overHandle(hud, mx, my)) {
                resizingHud = hud;
                resizeStartScale = hud.getScale();
                resizeStartW = hud.getWidth();
                resizeStartH = hud.getHeight();
                return true;
            }
        }
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i) instanceof HudModule hud && hud.isToggled() && hud.isOver(mx, my)) {
                draggingHud = hud;
                hudDragX = (float) (mx - hud.getRenderX());
                hudDragY = (float) (my - hud.getRenderY());
                return true;
            }
        }
        return false;
    }

    public boolean onMouseReleased(double mx, double my, int button) {
        float s = uiScale();
        modern.mouseReleased(mx / s, my / s, button);
        legacy.mouseReleased(mx, my, button);
        draggingHud = null;
        resizingHud = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        boolean onUi = !hudMode && overUi(mx, my);
        HudModule hud = onUi ? null : hudAt(mx, my);
        if (hud != null) {
            hud.setScale(hud.getScale() + (float) Math.signum(vertical) * 0.05f);
            return true;
        }
        if (hudMode) return true;
        if (modernStyle()) {
            float s = uiScale();
            modern.scroll(mx / s, my / s, vertical);
        } else {
            legacy.scroll(vertical);
        }
        return true;
    }

    public boolean onKeyPressed(int key, int scanCode, int modifiers) {
        if (hudMode && key == InputConstants.KEY_ESCAPE) { hudMode = false; return true; }
        if (!hudMode) {
            boolean consumed = modernStyle()
                    ? modern.keyPressed(key, scanCode, modifiers, minecraft.hasControlDown())
                    : legacy.keyPressed(key, scanCode, modifiers);
            if (consumed) return true;
        }
        ClickGui module = ClickGui.get();
        if (key == InputConstants.KEY_ESCAPE || (module != null && key == module.getKey())) {
            onClose();
            return true;
        }
        return (lami$lastKey != null && super.keyPressed(lami$lastKey));
    }

    public boolean onCharTyped(char chr, int modifiers) {
        if (!hudMode) {
            boolean consumed = modernStyle() ? modern.charTyped(chr, modifiers) : legacy.charTyped(chr, modifiers);
            if (consumed) return true;
        }
        return (lami$lastChar != null && super.charTyped(lami$lastChar));
    }

    @Override
    public void onClose() {
        ClientMod.getInstance().getConfigManager().save();
        draggingHud = null;
        resizingHud = null;
        modern.onClose();
        if (minecraft != null && parent != null) {
            Screen p = parent;
            parent = null;
            minecraft.gui.setScreen(p);
        } else {
            super.onClose();
        }
    }

    private net.minecraft.client.input.KeyEvent lami$lastKey;
    private net.minecraft.client.input.CharacterEvent lami$lastChar;

    public static int lamiButton(int sdl) {
        return switch (sdl) { case 1 -> 0; case 3 -> 1; case 2 -> 2; default -> Math.max(0, sdl - 1); };
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
        return onMouseClicked(e.x(), e.y(), lamiButton(e.button()));
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e) {
        return onMouseReleased(e.x(), e.y(), lamiButton(e.button()));
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e, double dx, double dy) {
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        lami$lastKey = e;
        return onKeyPressed(e.key(), e.keycode(), e.modifiers());
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
        lami$lastChar = e;
        if (e.codepoint() > 0xFFFF) return false;
        return onCharTyped((char) e.codepoint(), 0);
    }
}
