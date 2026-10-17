package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.food.FoodData;

public final class FoodHud extends HudModule {

    private final Animation food = new Animation(8f, 20f);
    private final Animation sat = new Animation(8f, 5f);

    public FoodHud() {
        super("FoodHUD", "Голод и скрытое насыщение", Category.HUD, -1, -1);
        width = 104;
        height = 30;
    }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(sw / 2f + 96, sh - 70); }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        if (nullCheck()) return;
        Theme t = Theme.get();
        FoodData hm = mc.player.getFoodData();
        food.setTarget(hm.getFoodLevel());
        sat.setTarget(hm.getSaturationLevel());
        float f = food.update(), s = sat.update();

        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());
        float bx = x + 6, bw = width - 12;
        Fonts.draw(ctx, I18n.tr("Еда"), bx, y + 4, t.textDim());
        String fv = hm.getFoodLevel() + "/20";
        Fonts.draw(ctx, fv, x + width - 6 - Fonts.width(fv), y + 4, t.text());
        RenderUtils.drawRoundedRect(ctx, bx, y + 14, bw, 3, 1.5f, 0x30FFFFFF);
        RenderUtils.drawRoundedHorizontalGradient(ctx, bx, y + 14, bw * f / 20f, 3, 1.5f, 0xFFE07A2E, 0xFFFFB14D);

        String sv = I18n.tr("насыщ. %.1f", hm.getSaturationLevel());
        RenderUtils.drawRoundedRect(ctx, bx, y + 21, bw, 3, 1.5f, 0x30FFFFFF);
        RenderUtils.drawRoundedHorizontalGradient(ctx, bx, y + 21, bw * Math.min(1f, s / 20f), 3, 1.5f, 0xFFFFD84D, 0xFFFFF1A8);
        var m = ctx.pose();
        m.pushMatrix();
        m.translate(x + width - 6, y + 25.5f);
        m.scale(0.6f, 0.6f);
        Fonts.draw(ctx, sv, -Fonts.width(sv), 0, ColorUtils.multAlpha(t.textDim(), 0.9f));
        m.popMatrix();
    }
}
