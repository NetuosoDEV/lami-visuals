package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.core.SessionTracker;

import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SessionStats extends HudModule {

    private final BooleanSetting playersOnly = register(new BooleanSetting("Players Only", true));

    public SessionStats() {
        super("SessionStats", "Время игры, убийства, K/D за сессию", Category.HUD, -1, -1);
        width = 110;
    }

    public boolean playersOnly() { return playersOnly.isEnabled(); }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(6, 30); }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        Theme t = Theme.get();
        String[][] rows = {
                {I18n.tr("Время"), SessionTracker.time()}, {I18n.tr("Убийства"), String.valueOf(SessionTracker.kills())},
                {I18n.tr("Смерти"), String.valueOf(SessionTracker.deaths())}, {"K/D", SessionTracker.kd()},
                {I18n.tr("Удары"), String.valueOf(SessionTracker.hits())}, {I18n.tr("Лучшая серия"), String.valueOf(SessionTracker.bestCombo())},
        };
        height = 20 + rows.length * 11 + 3;
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());
        Fonts.drawBold(ctx, I18n.tr("Сессия"), x + 6, y + 5, t.text());
        RenderUtils.drawRoundedHorizontalGradient(ctx, x + 6, y + 16, width - 12, 1, 0.5f, t.accentA(), ColorUtils.withAlpha(t.accentB(), 0));
        float ry = y + 20;
        for (String[] r : rows) {
            Fonts.draw(ctx, r[0], x + 6, ry, t.textDim());
            Fonts.draw(ctx, r[1], x + width - 6 - Fonts.width(r[1]), ry, t.text());
            ry += 11;
        }
    }
}
