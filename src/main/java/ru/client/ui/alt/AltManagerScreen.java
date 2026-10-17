package ru.client.ui.alt;

import ru.client.core.I18n;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.client.core.ClientMod;
import ru.client.ui.UiSounds;
import ru.client.ui.clickgui.ClickGuiScreen;
import ru.client.ui.clickgui.Icons;
import ru.client.ui.clickgui.Ui;
import ru.client.ui.menu.LamiMainMenu;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

public final class AltManagerScreen extends Screen {

    private final Screen parent;
    private final AltsPage page = new AltsPage();
    private final Ui.Hits hits = new Ui.Hits();
    private final Animation open = new Animation(12f, 0f);

    public AltManagerScreen(Screen parent) {
        super(Component.literal("AltManager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        open.snap(0f);
        open.setTarget(1f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) return;
        if (parent instanceof LamiMainMenu menu) {
            if (menu.width != width || menu.height != height) menu.resize(width, height);
            menu.renderBackdrop(ctx, mouseX, mouseY);
        } else {
            extractPanorama(ctx, delta);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float a = Mth.clamp(open.update(), 0f, 1f);
        if (page.typing()) minecraft.textInputManager().startTextInput(this);
        else minecraft.textInputManager().stopTextInput(this);
        if (minecraft.options.getMenuBackgroundBlurriness() >= 1f) ctx.blurBeforeThisStratum();
        RenderUtils.drawRect(ctx, 0, 0, width, height, ColorUtils.withAlpha(0xFF000000, (int) (120 * a)));

        float w = Math.min(400, width - 32), h = Math.min(320, height - 32);
        float x = (width - w) / 2f, y = (height - h) / 2f + (1f - a) * 8f;
        hits.clear();

        RenderUtils.drawGlow(ctx, x, y, w, h, 8, 12, ColorUtils.withAlpha(0x000000, (int) (120 * a)));
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 8, Ui.bg());
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, 8, 1, Ui.LINE_HI);

        Fonts.drawScaled(ctx, I18n.tr("Аккаунты"), x + 16, y + 15, 1.4f, Ui.text(), Fonts.BOLD);
        float tw = Fonts.width(I18n.tr("Аккаунты"), Fonts.BOLD) * 1.4f;
        Fonts.draw(ctx, "AltManager", x + 16 + tw + 8, y + 18, Ui.faint());
        float cs = 20;
        Ui.iconButton(ctx, hits, "alts.close", Icons.Glyph.CLOSE, x + w - 12 - cs, y + 12, cs, false, Ui.text(), mx, my, this::onClose);
        RenderUtils.drawRect(ctx, x + 1, y + 44, w - 2, 1, Ui.LINE);

        page.render(ctx, x + 16, y + 56, w - 32, h - 56 - 12, mx, my);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
        int b = ClickGuiScreen.lamiButton(e.button());
        if (b == 0 && hits.click(e.x(), e.y())) return true;
        page.mouseClicked(e.x(), e.y(), b);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        page.scroll(vertical);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        if (page.keyPressed(e.key(), minecraft.hasControlDown())) return true;
        if (e.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent e) {
        if (e.codepoint() > 0xFFFF) return false;
        return page.charTyped((char) e.codepoint()) || super.charTyped(e);
    }

    @Override
    public void onClose() {
        ClientMod.getInstance().getAltManager().save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void added() {
        super.added();
        UiSounds.play(UiSounds.Sfx.MENU_OPEN);
    }

    @Override
    public void removed() {
        minecraft.textInputManager().stopTextInput(this);
        UiSounds.play(UiSounds.Sfx.MENU_CLOSE);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
