package ru.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.client.modules.ModuleManager;

public final class CombatTracker {

    private static final Minecraft mc = Minecraft.getInstance();

    private static LivingEntity target;
    private static long lastAttackMs;
    private static int critEntityId = -1;
    private static long lastCritMs;
    private static boolean killHandled;

    private CombatTracker() {}

    public static void onAttack(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        if (living != target) killHandled = false;
        target = living;
        lastAttackMs = System.currentTimeMillis();
        if (isCriticalNow()) {
            critEntityId = living.getId();
            lastCritMs = lastAttackMs;
        }
    }

    private static boolean isCriticalNow() {
        LocalPlayer p = mc.player;
        if (p == null) return false;
        return p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater()
                && !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger() && !p.isSprinting()
                && p.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public static void tick(ModuleManager manager) {
        if (target == null) return;
        if (mc.level == null || target.level() != mc.level) { reset(); return; }
        if (!killHandled && (target.isDeadOrDying() || target.getHealth() <= 0)
                && System.currentTimeMillis() - lastAttackMs < 3000) {
            killHandled = true;
            manager.onKill(target);
        }
        if (target.isRemoved() && System.currentTimeMillis() - lastAttackMs > 3000) target = null;
    }

    public static LivingEntity getTarget(long timeoutMs) {
        if (target == null || target.isRemoved() || target.isDeadOrDying() || target.level() != mc.level) return null;
        return System.currentTimeMillis() - lastAttackMs <= timeoutMs ? target : null;
    }

    public static boolean isRecentCrit(Entity e, long withinMs) {
        return e.getId() == critEntityId && System.currentTimeMillis() - lastCritMs <= withinMs;
    }

    public static boolean attackedRecently(Entity e, long withinMs) {
        return target == e && System.currentTimeMillis() - lastAttackMs <= withinMs;
    }

    public static void reset() {
        target = null;
        critEntityId = -1;
        killHandled = false;
    }
}
