package ru.client.ui.screens;

import ru.client.core.I18n;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.client.core.SessionTracker;
import ru.client.ui.clickgui.ClickGuiScreen;
import ru.client.ui.clickgui.Ui;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LamiDeathScreen extends DeathScreen {

    private static final int RED = 0xFFFF4A5A, RED2 = 0xFFFF9A6A;

    private final Component cause;
    private final boolean hardcore;
    private final LocalPlayer player;
    private final String coords;
    private final Ui.Hits hits = new Ui.Hits();
    private final long openedAt = System.currentTimeMillis();
    private int ticks;
    private boolean respawning;

    public LamiDeathScreen(Component cause, boolean hardcore, LocalPlayer player) {
        super(cause, hardcore, player);
        this.cause = cause;
        this.hardcore = hardcore;
        this.player = player;
        this.coords = String.format(Locale.ROOT, "%d, %d, %d", Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()));
    }

    @Override
    protected void init() {
        respawning = false;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
    }

    private boolean ready() { return ticks >= 20; }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        float t = (System.currentTimeMillis() - openedAt) / 1000f;
        float in = Mth.clamp(t / 0.6f, 0f, 1f);
        RenderUtils.drawRect(ctx, 0, 0, width, height, ColorUtils.withAlpha(0xFF0A0306, (int) (170 * in)));
        float pulse = 0.75f + 0.25f * (float) Math.sin(t * 2.2);
        int edge = ColorUtils.withAlpha(0xFFB0101E, (int) (150 * in * pulse)), clear = ColorUtils.withAlpha(0xFFB0101E, 0);
        float d = Math.min(width, height) * 0.22f;
        RenderUtils.drawVerticalGradient(ctx, 0, 0, width, d, edge, clear);
        RenderUtils.drawVerticalGradient(ctx, 0, height - d, width, d, clear, edge);
        RenderUtils.drawHorizontalGradient(ctx, 0, 0, d, height, edge, clear);
        RenderUtils.drawHorizontalGradient(ctx, width - d, 0, d, height, clear, edge);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float t = (System.currentTimeMillis() - openedAt) / 1000f;
        float a = Mth.clamp(t / 0.45f, 0f, 1f);
        hits.clear();

        String title = I18n.tr(hardcore ? "Игра окончена" : "Ты погиб");
        float size = Mth.clamp(width / 14f, 26f, 42f);
        float drop = (1f - a) * (1f - a) * -24f;
        float shake = t < 0.6f ? (float) Math.sin(t * 60) * (0.6f - t) * 4f : 0f;
        float tw = Fonts.logoWidth(title, size);
        float ty = Math.max(12, height / 2f - 150) + drop;
        RenderUtils.drawSoftCircle(ctx, width / 2f, ty + size * 0.5f, tw * 0.6f, ColorUtils.withAlpha(RED, (int) (55 * a)));
        Fonts.drawLogo(ctx, title, width / 2f - tw / 2f + shake, ty, size, RED, RED2);

        float w = Math.min(300, width - 32);
        float x = (width - w) / 2f, y = ty + size + 18;
        List<String> causeLines = wrap(cause != null ? cause.getString() : "", (int) (w - 32));
        float h = 16 + causeLines.size() * 11 + 10 + 30 + 10 + 32 + 14 + 24 + 6 + 22 + 16;
        y += (1f - a) * 10f;
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 8, ColorUtils.withAlpha(Ui.bg(), (int) (245 * a)));
        RenderUtils.drawRoundedOutline(ctx, x, y, w, h, 8, 1, ColorUtils.withAlpha(RED, (int) (70 * a)));

        float cy = y + 16;
        for (String line : causeLines) {
            Fonts.drawCentered(ctx, line, x + w / 2f, cy, Ui.text(), Fonts.REGULAR);
            cy += 11;
        }
        cy += 10;

        float half = (w - 32 - 6) / 2f;
        chip(ctx, x + 16, cy, half, I18n.tr("счёт"), String.valueOf(player.getScore()));
        chip(ctx, x + 16 + half + 6, cy, half, I18n.tr("место смерти"), coords);
        cy += 40;

        String[][] stats = {{SessionTracker.time(), I18n.tr("время")}, {String.valueOf(SessionTracker.kills()), I18n.tr("убийства")},
                {String.valueOf(SessionTracker.deaths()), I18n.tr("смерти")}, {SessionTracker.kd(), "K/D"}};
        float bw = w - 32, sw = (bw - 18) / 4f;
        for (int i = 0; i < stats.length; i++) {
            float sx = x + 16 + i * (sw + 6);
            RenderUtils.drawRoundedRect(ctx, sx, cy, sw, 32, 5, Ui.inset());
            Fonts.drawCentered(ctx, stats[i][0], sx + sw / 2f, cy + 6, Ui.text(), Fonts.BOLD);
            Fonts.drawCentered(ctx, stats[i][1], sx + sw / 2f, cy + 18, Ui.faint(), Fonts.REGULAR);
        }
        cy += 46;

        String respawn = I18n.tr(hardcore ? "Наблюдать" : "Возродиться");
        float charge = Mth.clamp((ticks + delta) / 20f, 0f, 1f);
        if (ready() && !respawning) {
            Ui.button(ctx, hits, "death.respawn", respawn, null, x + 16, cy, bw, 24, Ui.Kind.PRIMARY, mx, my, this::respawn);
        } else {
            RenderUtils.drawRoundedRect(ctx, x + 16, cy, bw, 24, Ui.RADIUS, Ui.surface());
            RenderUtils.drawRoundedRect(ctx, x + 16, cy, bw * charge, 24, Ui.RADIUS, ColorUtils.withAlpha(Ui.accent(), 110));
            Fonts.drawCentered(ctx, respawning ? I18n.tr("Возрождение…") : respawn, x + 16 + bw / 2f, cy + 8, Ui.dim(), Fonts.REGULAR);
        }
        cy += 30;
        if (ready()) {
            Ui.button(ctx, hits, "death.title", I18n.tr("В главное меню"), null, x + 16, cy, bw, 22, Ui.Kind.DANGER, mx, my, this::exitToTitle);
        } else {
            RenderUtils.drawRoundedRect(ctx, x + 16, cy, bw, 22, Ui.RADIUS, Ui.surface());
            Fonts.drawCentered(ctx, I18n.tr("В главное меню"), x + 16 + bw / 2f, cy + 7, Ui.faint(), Fonts.REGULAR);
        }
    }

    private void chip(GuiGraphicsExtractor ctx, float x, float y, float w, String label, String value) {
        RenderUtils.drawRoundedRect(ctx, x, y, w, 30, 5, Ui.inset());
        Fonts.draw(ctx, label, x + 8, y + 5, Ui.faint());
        Fonts.drawBold(ctx, Fonts.trim(value, (int) (w - 16)), x + 8, y + 16, Ui.text());
    }

    private static List<String> wrap(String s, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String probe = line.isEmpty() ? word : line + " " + word;
            if (Fonts.width(probe) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(probe);
            }
            if (out.size() == 2) break;
        }
        if (out.size() < 3 && !line.isEmpty()) out.add(Fonts.trim(line.toString(), maxW));
        return out;
    }

    private void respawn() {
        if (respawning) return;
        respawning = true;
        player.respawn();
    }

    private void exitToTitle() {
        minecraft.getReportingContext().draftReportHandled(minecraft, this, this::handleExitToTitleScreen, true);
    }

    private void handleExitToTitleScreen() {
        if (hardcore) {
            exitToTitleScreen();
            return;
        }
        ConfirmScreen confirm = new DeathScreen.TitleConfirmScreen(result -> {
            if (result) {
                exitToTitleScreen();
            } else {
                player.respawn();
                minecraft.gui.setScreen(null);
            }
        }, Component.translatable("deathScreen.quit.confirm"), CommonComponents.EMPTY,
                Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.respawn"));
        minecraft.gui.setScreen(confirm);
        confirm.setDelay(20);
    }

    private void exitToTitleScreen() {
        if (minecraft.level != null) minecraft.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
        minecraft.disconnectWithSavingScreen();
        minecraft.gui.setScreen(new TitleScreen());
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
        if (ClickGuiScreen.lamiButton(e.button()) == 0 && hits.click(e.x(), e.y())) return true;
        return false;
    }
}
