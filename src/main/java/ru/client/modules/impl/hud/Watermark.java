package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.Logo;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class Watermark extends HudModule {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ModeSetting style = register(new ModeSetting("Style", "Island", "Island", "Classic"));
    private final BooleanSetting showVersion = register(new BooleanSetting("Version", true));
    private final BooleanSetting showFps = register(new BooleanSetting("FPS", true));
    private final BooleanSetting showPing = register(new BooleanSetting("Ping", true));
    private final BooleanSetting showServer = register(new BooleanSetting("Server", false));
    private final BooleanSetting showTime = register(new BooleanSetting("Time", true));
    private final NumberSetting radius = register(new NumberSetting("Radius", 5, 0, 10, 0.5));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));

    public Watermark() {
        super("Watermark", "Логотип клиента с FPS / пингом / временем", Category.HUD, 6, 6);
        radius.visibleWhen(() -> style.is("Classic"));
        setToggled(true);
    }

    private List<String> parts() {
        List<String> p = new ArrayList<>();
        if (showFps.isEnabled()) p.add(I18n.tr("%d fps", mc.getFps()));
        if (showPing.isEnabled() && mc.player != null && mc.getConnection() != null) {
            PlayerInfo e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            p.add(I18n.tr("%d мс", e == null ? 0 : e.getLatency()));
        }
        if (showServer.isEnabled()) {
            var info = mc.getCurrentServer();
            p.add(info != null ? info.ip : I18n.tr("одиночная игра"));
        }
        if (showTime.isEnabled()) p.add(LocalTime.now().format(TIME));
        return p;
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float tickDelta) {
        Theme t = Theme.get();
        int c1 = t.accentA(), c2 = t.accentB();
        String ver = showVersion.isEnabled() ? " " + ClientMod.VERSION : "";
        List<String> parts = parts();
        String sep = "  •  ";

        String name = ClientMod.NAME;
        float markH = 11, markW = Logo.markWidth(markH);
        float nameW = markW + 4 + Fonts.width(name, Fonts.BOLD);
        float verW = Fonts.width(ver);
        float partsW = 0;
        for (String s : parts) partsW += Fonts.width(sep) + Fonts.width(s);

        boolean island = style.is("Island");
        float padL = island ? 9 : 7;
        width = padL + nameW + verW + partsW + 8;
        height = island ? 18 : 17;
        float r = island ? height / 2f : radius.getFloat();

        if (glow.isEnabled()) RenderUtils.drawGlow(ctx, x, y, width, height, r, 6, ColorUtils.withAlpha(c1, 130));
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, r, island ? 0xF0080809 : t.panel());
        if (!island) RenderUtils.drawRoundedHorizontalGradient(ctx, x + 3, y + height - 1.5f, width - 6, 1.5f, 0.75f, c1, c2);

        float tx = x + padL, ty = y + (height - 8) / 2f;
        Logo.mark(ctx, tx, y + (height - markH) / 2f, markH, t.accentSolid());
        Fonts.drawGradient(ctx, name, tx + markW + 4, ty, c1, c2, Fonts.BOLD);
        tx += nameW;
        if (!ver.isEmpty()) {
            Fonts.draw(ctx, ver, tx, ty, t.textDim());
            tx += verW;
        }
        for (String s : parts) {
            Fonts.draw(ctx, sep, tx, ty, ColorUtils.multAlpha(t.textDim(), 0.6f));
            tx += Fonts.width(sep);
            Fonts.draw(ctx, s, tx, ty, t.text());
            tx += Fonts.width(s);
        }
    }
}
