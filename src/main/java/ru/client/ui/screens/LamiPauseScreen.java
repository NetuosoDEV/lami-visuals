package ru.client.ui.screens;

import ru.client.core.I18n;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import ru.client.core.SessionTracker;
import ru.client.modules.impl.utility.ClickGui;
import ru.client.ui.UiSounds;
import ru.client.ui.clickgui.ClickGuiScreen;
import ru.client.ui.clickgui.Icons;
import ru.client.ui.clickgui.Ui;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

public final class LamiPauseScreen extends PauseScreen {

    private final Ui.Hits hits = new Ui.Hits();
    private final Animation open = new Animation(12f, 0f);
    private boolean disconnecting;

    public LamiPauseScreen() {
        super(true);
    }

    @Override
    protected void init() {
        disconnecting = false;
        open.snap(0f);
        open.setTarget(1f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        if (minecraft.options.getMenuBackgroundBlurriness() >= 1f && minecraft.gui.screen() == this) ctx.blurBeforeThisStratum();
        RenderUtils.drawRect(ctx, 0, 0, width, height, ColorUtils.withAlpha(0xFF000000, (int) (110 * Mth.clamp(open.get(), 0f, 1f))));
    }

    private String place() {
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local != null) return local.getWorldData().getLevelName();
        ServerData server = minecraft.getCurrentServer();
        return server != null ? server.ip : I18n.tr("Сетевая игра");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float a = Mth.clamp(open.update(), 0f, 1f);
        hits.clear();
        float w = Math.min(300, width - 32);
        float h = 264;
        float x = (width - w) / 2f, y = Math.max(8, (height - h) / 2f) + (1f - a) * 10f;

        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 8, Ui.bg());
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, 8, 1, Ui.LINE_HI);

        Fonts.drawScaled(ctx, I18n.tr("Пауза"), x + 16, y + 14, 1.4f, Ui.text(), Fonts.BOLD);
        Fonts.draw(ctx, Fonts.trim(place(), (int) (w - 120)), x + 16, y + 30, Ui.faint());
        float cs = 20;
        Ui.iconButton(ctx, hits, "pause.lami", Icons.Glyph.GEAR, x + w - 12 - cs, y + 12, cs, false, Ui.text(), mx, my,
                () -> ClickGui.open(this));
        RenderUtils.drawRect(ctx, x + 1, y + 46, w - 2, 1, Ui.LINE);

        float bx = x + 16, bw = w - 32, by = y + 58;
        Ui.button(ctx, hits, "pause.back", I18n.tr("Вернуться в игру"), null, bx, by, bw, 24, Ui.Kind.PRIMARY, mx, my, this::backToGame);
        by += 32;
        float half = (bw - 6) / 2f;
        Ui.button(ctx, hits, "pause.adv", I18n.tr("Достижения"), null, bx, by, half, 22, Ui.Kind.GHOST, mx, my,
                () -> minecraft.gui.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this)));
        Ui.button(ctx, hits, "pause.stats", I18n.tr("Статистика"), null, bx + half + 6, by, half, 22, Ui.Kind.GHOST, mx, my,
                () -> minecraft.gui.setScreen(new StatsScreen(this, minecraft.player.getStats())));
        by += 28;
        Ui.button(ctx, hits, "pause.options", I18n.tr("Настройки"), Icons.Glyph.GEAR, bx, by, half, 22, Ui.Kind.GHOST, mx, my,
                () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options)));
        if (minecraft.level != null) {
            Ui.button(ctx, hits, "pause.world", I18n.tr("Настройки мира"), null, bx + half + 6, by, half, 22, Ui.Kind.GHOST, mx, my,
                    () -> minecraft.gui.setScreen(new WorldOptionsScreen(this, minecraft.level)));
        }
        by += 28;
        Ui.button(ctx, hits, "pause.social", I18n.tr("Игроки"), Icons.Glyph.USER, bx, by, half, 22, Ui.Kind.GHOST, mx, my,
                () -> minecraft.gui.setScreen(new SocialInteractionsScreen(this)));
        Ui.button(ctx, hits, "pause.lamivisuals", "LamiVisuals", null, bx + half + 6, by, half, 22, Ui.Kind.GHOST, mx, my,
                () -> ClickGui.open(this));
        by += 34;

        String[][] stats = {{SessionTracker.time(), I18n.tr("время")}, {String.valueOf(SessionTracker.kills()), I18n.tr("убийства")},
                {String.valueOf(SessionTracker.deaths()), I18n.tr("смерти")}, {SessionTracker.kd(), "K/D"}};
        float sw = (bw - 6 * 3) / 4f;
        for (int i = 0; i < stats.length; i++) {
            float sx = bx + i * (sw + 6);
            RenderUtils.drawRoundedRect(ctx, sx, by, sw, 32, 5, Ui.inset());
            Fonts.drawCentered(ctx, stats[i][0], sx + sw / 2f, by + 6, Ui.text(), Fonts.BOLD);
            Fonts.drawCentered(ctx, stats[i][1], sx + sw / 2f, by + 18, Ui.faint(), Fonts.REGULAR);
        }
        by += 42;

        Ui.button(ctx, hits, "pause.vanilla", I18n.tr("Ванильное меню"), null, bx, by, half, 22, Ui.Kind.GHOST, mx, my, () -> {
            LamiScreens.allowVanillaOnce();
            minecraft.gui.setScreen(new PauseScreen(true));
        });
        String exit = CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()).getString();
        Ui.button(ctx, hits, "pause.exit", exit, null, bx + half + 6, by, half, 22, Ui.Kind.DANGER, mx, my, this::disconnect);
    }

    private void backToGame() {
        minecraft.gui.setScreen(null);
        minecraft.mouseHandler.grabMouse();
    }

    private void disconnect() {
        if (disconnecting) return;
        disconnecting = true;
        minecraft.getReportingContext().draftReportHandled(minecraft, this,
                () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
        if (ClickGuiScreen.lamiButton(e.button()) == 0 && hits.click(e.x(), e.y())) return true;
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public void added() {
        super.added();
        UiSounds.play(UiSounds.Sfx.MENU_OPEN);
    }
}
