package ru.client.modules.impl.combat;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class HitCounter extends HudModule {

    private final NumberSetting window = register(new NumberSetting("Window", 1.5, 0.5, 5, 0.1));
    private final BooleanSetting playersOnly = register(new BooleanSetting("Players Only", true));

    private int combo;
    private int lastTargetId = -1;
    private long lastHit;
    private final Animation pulse = new Animation(10f, 0f);
    private final Animation appear = new Animation(10f, 0f);

    public HitCounter() {
        super("HitCounter", "Счётчик серии ударов", Category.COMBAT, -1, -1);
        width = 80;
        height = 24;
    }

    @Override
    public void onAttack(Entity target) {
        if (playersOnly.isEnabled() && !(target instanceof Player)) return;
        long now = System.currentTimeMillis();
        if (target.getId() == lastTargetId && now - lastHit <= window.get() * 1000) combo++;
        else combo = 1;
        lastTargetId = target.getId();
        lastHit = now;
        pulse.snap(1f);
        pulse.setTarget(0f);
    }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(sw / 2f - width / 2f, sh / 2f - 60); }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        long now = System.currentTimeMillis();
        float windowMs = (float) (window.get() * 1000);
        float left = 1f - (now - lastHit) / windowMs;
        if (left <= 0) combo = 0;
        boolean show = combo >= 2 || isEditing();
        appear.setTarget(show ? 1f : 0f);
        float a = appear.update();
        if (a < 0.02f) return;

        Theme t = Theme.get();
        float p = pulse.update();
        int shown = Math.max(combo, isEditing() ? 5 : 0);
        String text = I18n.tr("x%d КОМБО", shown);

        var m = ctx.pose();
        m.pushMatrix();
        float cx = x + width / 2f, cy = y + height / 2f;
        m.translate(cx, cy);
        float s = (0.9f + 0.1f * a) * (1f + 0.15f * p);
        m.scale(s, s);
        m.translate(-cx, -cy);

        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, ColorUtils.multAlpha(t.panel(), a));
        int tw = Fonts.width(text);
        Fonts.draw(ctx, text, Math.round(cx - tw / 2f), Math.round(y + 5),
                ColorUtils.multAlpha(ColorUtils.lerp(t.text(), t.accentA(), p), Math.max(a, 0.05f)), false);
        float bw = (width - 12) * Math.max(0, isEditing() && combo < 2 ? 0.6f : left);
        RenderUtils.drawRoundedRect(ctx, x + 6, y + height - 6, width - 12, 2.5f, 1.25f, 0xFF26262E);
        RenderUtils.drawRoundedHorizontalGradient(ctx, x + 6, y + height - 6, bw, 2.5f, 1.25f, t.accentA(), t.accentB());
        m.popMatrix();
    }
}
