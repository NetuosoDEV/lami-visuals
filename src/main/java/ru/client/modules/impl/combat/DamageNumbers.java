package ru.client.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DamageNumbers extends Module {

    private final ModeSetting targets = register(new ModeSetting("Targets", "All", "All", "Players", "Attacked"));
    private final BooleanSetting self = register(new BooleanSetting("Self", false));
    private final BooleanSetting heal = register(new BooleanSetting("Heal", true));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1.2, 0.5, 3, 0.1));
    private final NumberSetting life = register(new NumberSetting("Lifetime", 1.2, 0.4, 3, 0.1));
    private final NumberSetting range = register(new NumberSetting("Range", 24, 4, 64, 1));

    private static final int NORMAL = 0xFFFFC94D, CRIT = 0xFFFF4545, HEAL = 0xFF55FF77;

    private record Popup(double x, double y, double z, String text, int color, long born, float drift) {}

    private final List<Popup> popups = new ArrayList<>();
    private Int2FloatOpenHashMap lastHealth = new Int2FloatOpenHashMap();

    public DamageNumbers() {
        super("DamageNumbers", "3D-числа урона над целью", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        popups.clear();
        lastHealth.clear();
    }

    @Override
    public void onTick() {
        if (nullCheck()) { lastHealth.clear(); return; }
        Int2FloatOpenHashMap next = new Int2FloatOpenHashMap(lastHealth.size() + 8);
        double r2 = range.get() * range.get();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity e) || entity.distanceToSqr(mc.player) > r2) continue;
            if (e == mc.player && !self.isEnabled()) continue;
            if (targets.is("Players") && !(e instanceof Player)) continue;
            if (targets.is("Attacked") && !CombatTracker.attackedRecently(e, 3000)) continue;

            float hp = e.getHealth() + e.getAbsorptionAmount();
            next.put(e.getId(), hp);
            if (!lastHealth.containsKey(e.getId())) continue;
            float delta = lastHealth.get(e.getId()) - hp;
            if (delta > 0.05f) {
                boolean crit = CombatTracker.isRecentCrit(e, 600);
                spawn(e, String.format(Locale.ROOT, crit ? "%.1f!" : "%.1f", delta), crit ? CRIT : NORMAL);
            } else if (delta < -0.05f && heal.isEnabled() && e.isAlive()) {
                spawn(e, String.format(Locale.ROOT, "+%.1f", -delta), HEAL);
            }
        }
        lastHealth = next;
    }

    private void spawn(LivingEntity e, String text, int color) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double ox = r.nextDouble(-0.35, 0.35), oz = r.nextDouble(-0.35, 0.35);
        popups.add(new Popup(e.getX() + ox, e.getY() + e.getBbHeight() + 0.25, e.getZ() + oz, text, color,
                System.currentTimeMillis(), (float) r.nextDouble(-0.2, 0.2)));
        if (popups.size() > 80) popups.remove(0);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (popups.isEmpty()) return;
        long now = System.currentTimeMillis();
        long lifeMs = (long) (life.get() * 1000);
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            float t = (now - p.born()) / (float) lifeMs;
            if (t >= 1f) { it.remove(); continue; }
            float rise = 1f - (1f - t) * (1f - t);
            float pop = t < 0.12f ? 1.6f - t / 0.12f * 0.6f : 1f;
            float alpha = t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
            int color = ColorUtils.multAlpha(p.color(), alpha);
            Render3D.text(ms, camera, p.text(), p.x() + p.drift() * rise, p.y() + rise * 0.8, p.z(),
                    scale.getFloat() * pop, color);
        }
    }
}
