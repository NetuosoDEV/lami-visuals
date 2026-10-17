package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public final class Potions extends HudModule {

    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private final BooleanSetting hideVanilla = register(new BooleanSetting("Hide Vanilla", true));

    public Potions() {
        super("Potions", "Эффекты и их длительность", Category.HUD, 6, 120);
    }

    public boolean hideVanilla() { return hideVanilla.isEnabled(); }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        if (nullCheck()) return;
        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        if (effects.isEmpty() && !isEditing()) { width = 90; height = 16; return; }
        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());

        Theme t = Theme.get();
        float rowH = 12;
        width = 110;
        for (MobEffectInstance e : effects) width = Math.max(width, Fonts.width(name(e)) + 60);
        height = 16 + Math.max(1, effects.size()) * rowH + 3;

        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());
        Fonts.draw(ctx, I18n.tr("Эффекты"), Math.round(x + 6), Math.round(y + 4), t.text(), false);
        RenderUtils.drawHorizontalGradient(ctx, x + 6, y + 14, width - 12, 1, t.accentA(), t.accentB());

        float ry = y + 18;
        if (effects.isEmpty()) {
            Fonts.draw(ctx, I18n.tr("Нет эффектов"), Math.round(x + 6), Math.round(ry), t.textDim(), false);
            return;
        }
        float tickRate = mc.level != null ? mc.level.tickRateManager().tickrate() : 20f;
        for (MobEffectInstance e : effects) {
            int color = e.getEffect().value().getColor() | 0xFF000000;
            RenderUtils.drawCircle(ctx, x + 9, ry + 3.5f, 2.5f, color);
            Fonts.draw(ctx, name(e), Math.round(x + 15), Math.round(ry), t.text(), false);
            String dur = MobEffectUtil.formatDuration(e, 1f, tickRate).getString();
            boolean ending = !e.isInfiniteDuration() && e.getDuration() < 200;
            Fonts.draw(ctx, dur, Math.round(x + width - 6 - Fonts.width(dur)), Math.round(ry),
                    ending ? ColorUtils.lerp(t.textDim(), 0xFFFF5555, (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5)) : t.textDim(), false);
            ry += rowH;
        }
    }

    private static String name(MobEffectInstance e) {
        int amp = e.getAmplifier();
        return e.getEffect().value().getDisplayName().getString() + (amp >= 0 && amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
    }
}
