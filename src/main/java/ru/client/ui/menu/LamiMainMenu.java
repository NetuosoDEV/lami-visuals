package ru.client.ui.menu;

import ru.client.core.I18n;
import ru.client.ui.Logo;
import ru.client.ui.UiSounds;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import ru.client.core.ClientMod;
import ru.client.core.alt.AltManager;
import ru.client.ui.alt.AltManagerScreen;
import ru.client.ui.clickgui.Icons;
import ru.client.ui.clickgui.Ui;
import ru.client.modules.Modules;
import ru.client.modules.impl.utility.ClickGui;
import ru.client.modules.impl.utility.MainMenu;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class LamiMainMenu extends Screen {

    private static final String[] CHANGELOG = {
            "FreeLook: обзор без поворота персонажа",
            "TotemPop: счётчик тотемов и своя анимация",
            "Shaders: Motion Blur, Bloom, цветокоррекция",
            "ChunkAnimator: чанки выезжают при загрузке",
            "Гладкий заголовок меню",
            "Язык: English, Русский, Español",
    };

    private static boolean firstOpen = true;

    private final List<MenuButton> buttons = new ArrayList<>();
    private BackgroundPopup popup;
    private final Animation fade;
    private final long openedAt = System.currentTimeMillis();
    private long lastFrame = System.nanoTime();

    public LamiMainMenu() {
        super(Component.literal("LamiVisuals"));
        fade = new Animation(4f, firstOpen ? 0f : 0.6f);
        firstOpen = false;
    }

    private static MainMenu settings() { return Modules.get(MainMenu.class); }

    @Override
    protected void init() {
        buttons.clear();
        float bw = 210, bh = 24, gap = 6;
        float cx = width / 2f, y = Math.max(height / 2f - 18, 110);
        buttons.add(new MenuButton(I18n.tr("Одиночная игра"), cx - bw / 2, y, bw, bh, false,
                () -> minecraft.gui.setScreen(new SelectWorldScreen(this))));
        y += bh + gap;
        MenuButton mp = new MenuButton(I18n.tr("Сетевая игра"), cx - bw / 2, y, bw, bh, false, () -> minecraft.gui.setScreen(
                minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        mp.active = minecraft.allowsMultiplayer();
        buttons.add(mp);
        y += bh + gap;
        buttons.add(new MenuButton("LamiVisuals", cx - bw / 2, y, bw, bh, true, () -> ClickGui.open(this)));
        y += bh + gap;
        float half = (bw - gap) / 2f;
        buttons.add(new MenuButton(I18n.tr("Настройки"), cx - bw / 2, y, half, bh, false,
                () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options))));
        buttons.add(new MenuButton(I18n.tr("Выход"), cx - bw / 2 + half + gap, y, half, bh, false, minecraft::stop));

        String vanilla = I18n.tr("Ванильное меню");
        float vw = Fonts.width(vanilla) + 16;
        buttons.add(new MenuButton(vanilla, width - vw - 6, height - 22, vw, 16, false, () -> {
            MainMenu m = settings();
            if (m != null) m.setToggled(false);
            ClientMod.getInstance().getConfigManager().save();
            minecraft.gui.setScreen(new TitleScreen());
        }).small());

        String bgLabel = I18n.tr("Фон");
        float bgw = Fonts.width(bgLabel) + 16;
        buttons.add(new MenuButton(bgLabel, width - vw - 6 - bgw - 4, height - 22, bgw, 16, false,
                () -> popup = new BackgroundPopup(this)).small());

        if (popup != null) popup.layout();
        fade.setTarget(1f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float time = (System.currentTimeMillis() - openedAt) / 1000f;
        Theme t = Theme.get();
        int a1 = t.accentA(), a2 = t.accentB();
        MainMenu cfg = settings();

        MenuBackground.get().render(ctx, width, height, mouseX, mouseY, () -> extractPanorama(ctx, delta));

        RenderUtils.drawVerticalGradient(ctx, 0, height * 0.5f, width, height * 0.5f, 0x00000000, 0x70000000);

        renderAccount(ctx, t, mouseX, mouseY);

        float logoSize = Mth.clamp(height / 7f, 26, 48);
        String logo = ClientMod.NAME;
        float lw = Fonts.logoWidth(logo, logoSize);
        float markH = logoSize * 1.25f, markW = Logo.markWidth(markH), gap = logoSize * 0.3f;
        float ly = Math.max(16, buttons.isEmpty() ? 40 : buttons.get(0).y - markH - 40) + Mth.sin(time * 1.4f) * 2f;
        float lx = (width - (markW + gap + lw)) / 2f;
        Logo.mark(ctx, lx, ly, markH, t.accentSolid());
        int shift = (int) (time * 400);
        float ty = ly + markH / 2f - Fonts.logoCapCenter(logoSize);
        Fonts.drawLogo(ctx, logo, lx + markW + gap, ty, logoSize, t.accent(shift), t.accent(shift + 1500));
        String sub = I18n.tr("визуал для minecraft 26.3");
        Fonts.drawCentered(ctx, sub, width / 2f, ly + markH + 6, ColorUtils.withAlpha(t.textDim(), 220), Fonts.REGULAR);
        String ver = "v" + ClientMod.VERSION;
        float vw = Fonts.width(ver) + 12;
        RenderUtils.drawRoundedRect(ctx, width / 2f - vw / 2f, ly + markH + 19, vw, 13, 6.5f, ColorUtils.withAlpha(a1, 50));
        Fonts.drawCentered(ctx, ver, width / 2f, ly + markH + 21.5f, t.text(), Fonts.BOLD);

        for (MenuButton b : buttons) b.render(ctx, mouseX, mouseY, t);

        if ((cfg == null || cfg.changelog.isEnabled()) && width >= 560) renderChangelog(ctx, t);

        Fonts.draw(ctx, "Minecraft 26.3  •  Fabric", 6, height - 17, ColorUtils.withAlpha(t.textDim(), 200));

        if (popup != null) {
            if (popup.render(ctx, mouseX, mouseY, delta)) { popup = null; ClientMod.getInstance().getConfigManager().save(); }
        }

        float f = fade.update();
        if (f < 0.995f) RenderUtils.drawRect(ctx, 0, 0, width, height, ColorUtils.withAlpha(0xFF000000, (int) (255 * (1 - f))));
    }

    public void renderBackdrop(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        MenuBackground.get().render(ctx, width, height, mouseX, mouseY,
                () -> extractPanorama(ctx, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
    }

    private float accX, accY, accW, accH;

    private void renderAccount(GuiGraphicsExtractor ctx, Theme t, int mx, int my) {
        String name = minecraft.getUser().getName();
        AltManager am = AltManager.get();
        boolean alt = am != null && am.altActive();
        String sub = I18n.tr(alt ? "оффлайн-альт" : "сменить аккаунт");
        float w = Math.max(Fonts.width(name), Ui.labelWidth(sub)) + 52, h = 28, x = 6, y = 6;
        accX = x; accY = y; accW = w; accH = h;
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        float a = Ui.anim("menu.account", hover);
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, Ui.RADIUS, ColorUtils.lerp(0xC0101014, 0xE01A1A1F, a));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, Ui.RADIUS, 1, ColorUtils.lerp(Ui.LINE, Ui.LINE_HI, a));
        if (alt || am == null) {
            Ui.letterAvatar(ctx, name, x + 5, y + 5, 18);
        } else {
            Ui.face(ctx, Minecraft.getInstance().getGameProfile(), x + 5, y + 5, 18);
        }
        Fonts.draw(ctx, name, x + 29, y + 5, t.text());
        Ui.label(ctx, sub, x + 29, y + 16, Ui.faint());
        Icons.draw(ctx, Icons.Glyph.SWAP, x + w - 12, y + h / 2f, ColorUtils.lerp(Ui.faint(), t.text(), a));
    }

    private void renderChangelog(GuiGraphicsExtractor ctx, Theme t) {
        float w = 170, lineH = 12;
        float h = 30 + CHANGELOG.length * lineH;
        float x = width - w - 10, y = 10;
        RenderUtils.drawGlow(ctx, x, y, w, h, 8, 6, ColorUtils.withAlpha(t.accentA(), 50));
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 8, 0xC80E0E16);
        RenderUtils.drawRoundedHorizontalGradient(ctx, x + 10, y + 21, w - 20, 1, 0.5f, t.accentA(), t.accentB());
        Fonts.drawBold(ctx, I18n.tr("Что нового"), x + 10, y + 8, t.text());
        String v = "v" + ClientMod.VERSION;
        Fonts.draw(ctx, v, x + w - 10 - Fonts.width(v), y + 8, t.accentA());
        float ly = y + 27;
        for (String s : CHANGELOG) {
            RenderUtils.drawCircle(ctx, x + 13, ly + 4, 1.4f, t.accentB());
            Fonts.draw(ctx, Fonts.trim(I18n.tr(s), (int) w - 30), x + 19, ly, ColorUtils.withAlpha(t.text(), 210));
            ly += lineH;
        }
    }

        public boolean onMouseClicked(double mx, double my, int button) {
        if (popup != null) {
            if (popup.mouseClicked(mx, my, button)) return true;
            popup.close();
            return true;
        }
        if (button == 0 && mx >= accX && mx <= accX + accW && my >= accY && my <= accY + accH) {
            UiSounds.click();
            minecraft.gui.setScreen(new AltManagerScreen(this));
            return true;
        }
        if (button == 0) {
            for (MenuButton b : buttons) {
                if (b.active && b.isOver(mx, my)) {
                    UiSounds.click();
                    b.action.run();
                    return true;
                }
            }
        }
        return (lami$lastMouse != null && super.mouseClicked(lami$lastMouse, false));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

        public boolean onMouseReleased(double mx, double my, int button) {
        if (popup != null) popup.mouseReleased(mx, my, button);
        return (lami$lastMouse != null && super.mouseReleased(lami$lastMouse));
    }

        public boolean onKeyPressed(int key, int scanCode, int modifiers) {
        if (popup != null) {
            if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) { popup.close(); return true; }
            if (popup.keyPressed(key, scanCode, modifiers)) return true;
        }
        return (lami$lastKey != null && super.keyPressed(lami$lastKey));
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final class MenuButton {
        final String label;
        final float x, y, w, h;
        final boolean accent;
        final Runnable action;
        boolean active = true, small;
        private final Animation hover = new Animation(12f, 0f);

        MenuButton(String label, float x, float y, float w, float h, boolean accent, Runnable action) {
            this.label = label; this.x = x; this.y = y; this.w = w; this.h = h; this.accent = accent; this.action = action;
        }

        MenuButton small() { small = true; return this; }

        boolean isOver(double mx, double my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

        void render(GuiGraphicsExtractor ctx, int mx, int my, Theme t) {
            hover.setTarget(active && isOver(mx, my) ? 1f : 0f);
            float a = hover.update();
            int a1 = t.accentA(), a2 = t.accentB();
            float r = h / 2f;
            if (small) {
                RenderUtils.drawRoundedRect(ctx, x, y, w, h, r, ColorUtils.withAlpha(0xFF101018, (int) (140 + 80 * a)));
                Fonts.drawCentered(ctx, label, x + w / 2f, y + h / 2f - 4, ColorUtils.lerp(t.textDim(), t.text(), a), Fonts.REGULAR);
                return;
            }
            if (a > 0.01f || accent) RenderUtils.drawGlow(ctx, x, y, w, h, r, 6 + 4 * a, ColorUtils.withAlpha(a1, (int) ((accent ? 90 : 0) + 90 * a)));
            if (accent) {
                RenderUtils.drawRoundedHorizontalGradient(ctx, x, y, w, h, r,
                        ColorUtils.lerp(a1, 0xFFFFFFFF, 0.12f * a), ColorUtils.lerp(a2, 0xFFFFFFFF, 0.12f * a));
            } else {
                RenderUtils.drawRoundedRect(ctx, x, y, w, h, r, ColorUtils.lerp(0xD0121219, 0xE81C1C28, a));
                RenderUtils.drawRoundedOutline(ctx, x, y, w, h, r, 1, ColorUtils.withAlpha(ColorUtils.lerp(0xFF3A3A4A, a1, a), 200));
            }
            int tc = !active ? 0xFF5A5A66 : accent ? 0xFFFFFFFF : ColorUtils.lerp(0xFFD8D8E4, 0xFFFFFFFF, a);
            Fonts.draw(ctx, label, x + w / 2f - Fonts.width(label, Fonts.BOLD) / 2f + 2 * a, y + h / 2f - 4, tc, false, Fonts.BOLD);
        }
    }

    private net.minecraft.client.input.MouseButtonEvent lami$lastMouse;
    private net.minecraft.client.input.KeyEvent lami$lastKey;
    private net.minecraft.client.input.CharacterEvent lami$lastChar;

    public static int lamiButton(int sdl) {
        return switch (sdl) { case 1 -> 0; case 3 -> 1; case 2 -> 2; default -> Math.max(0, sdl - 1); };
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
        lami$lastMouse = e;
        return onMouseClicked(e.x(), e.y(), lamiButton(e.button()));
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e) {
        lami$lastMouse = e;
        return onMouseReleased(e.x(), e.y(), lamiButton(e.button()));
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        lami$lastKey = e;
        return onKeyPressed(e.key(), e.keycode(), e.modifiers());
    }

}
