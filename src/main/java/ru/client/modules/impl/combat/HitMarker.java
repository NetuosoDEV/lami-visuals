package ru.client.modules.impl.combat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class HitMarker extends Module {

    private final NumberSetting size = register(new NumberSetting("Size", 6, 3, 12, 0.5));
    private final NumberSetting duration = register(new NumberSetting("Duration", 0.35, 0.1, 1.0, 0.05));
    private final BooleanSetting killMarker = register(new BooleanSetting("Kill Marker", true));

    private long hitAt, killAt;
    private boolean crit;

    public HitMarker() {
        super("HitMarker", "Крестик у прицела при попадании", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        hitAt = System.currentTimeMillis();
        crit = CombatTracker.isRecentCrit(target, 50);
    }

    @Override
    public void onKill(LivingEntity target) {
        if (killMarker.isEnabled()) killAt = System.currentTimeMillis();
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (nullCheck() || !mc.options.getCameraType().isFirstPerson()) return;
        long now = System.currentTimeMillis();
        float dur = (float) (duration.get() * 1000);
        float kill = killAt == 0 ? 0 : 1f - (now - killAt) / (dur * 1.8f);
        float hit = hitAt == 0 ? 0 : 1f - (now - hitAt) / dur;
        float k = Math.max(hit, kill);
        if (k <= 0) return;

        boolean isKill = kill > 0;
        int color = isKill ? 0xFFFF3040 : crit ? Theme.get().accentA() : 0xFFFFFFFF;
        float s = size.getFloat() * (isKill ? 1.5f : 1f) * (0.85f + 0.3f * k);
        float gap = 2.5f + (1f - k) * 2f;
        float cx = mc.getWindow().getGuiScaledWidth() / 2f, cy = mc.getWindow().getGuiScaledHeight() / 2f;
        int c = ColorUtils.multAlpha(color, Math.min(1f, k * 1.5f));
        int shadow = ColorUtils.multAlpha(0xFF000000, Math.min(1f, k * 1.5f) * 0.5f);
        for (int i = 0; i < 4; i++) {
            float dx = (i == 0 || i == 3) ? -1 : 1, dy = i < 2 ? -1 : 1;
            float x1 = cx + dx * gap, y1 = cy + dy * gap, x2 = cx + dx * (gap + s), y2 = cy + dy * (gap + s);
            RenderUtils.drawLine(ctx, x1, y1 + 0.5f, x2, y2 + 0.5f, 2.2f, shadow);
            RenderUtils.drawLine(ctx, x1, y1, x2, y2, isKill ? 1.8f : 1.4f, c);
        }
    }
}
