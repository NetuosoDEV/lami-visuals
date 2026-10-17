package ru.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.client.modules.Modules;
import ru.client.modules.impl.hud.SessionStats;

import java.util.Locale;

public final class SessionTracker {
    private SessionTracker() {}

    private static final Minecraft mc = Minecraft.getInstance();
    private static long sessionStart;
    private static int session;
    private static Object worldRef;
    private static int kills, deaths, hits, combo, bestCombo, lastTarget = -1;
    private static long lastHit;
    private static boolean wasDead;

    private static boolean playersOnly() {
        SessionStats s = Modules.get(SessionStats.class);
        return s == null || s.playersOnly();
    }

    public static void tick() {
        if (mc.level == null) {
            if (worldRef != null) { kills = deaths = hits = combo = bestCombo = 0; sessionStart = 0; }
            worldRef = null;
            return;
        }
        if (worldRef == null) {
            sessionStart = System.currentTimeMillis();
            session++;
        }
        worldRef = mc.level;
        boolean dead = mc.player != null && mc.player.isDeadOrDying();
        if (dead && !wasDead) deaths++;
        wasDead = dead;
    }

    public static void onAttack(Entity target) {
        if (playersOnly() && !(target instanceof Player)) return;
        long now = System.currentTimeMillis();
        hits++;
        combo = (target.getId() == lastTarget && now - lastHit < 1500) ? combo + 1 : 1;
        bestCombo = Math.max(bestCombo, combo);
        lastTarget = target.getId();
        lastHit = now;
    }

    public static void onKill(LivingEntity target) {
        if (playersOnly() && !(target instanceof Player)) return;
        kills++;
    }

    public static int session() { return session; }

    public static int kills() { return kills; }
    public static int deaths() { return deaths; }
    public static int hits() { return hits; }
    public static int bestCombo() { return bestCombo; }

    public static String kd() {
        return String.format(Locale.ROOT, "%.2f", deaths == 0 ? kills : kills / (float) deaths);
    }

    public static String time() {
        long sec = sessionStart == 0 ? 0 : (System.currentTimeMillis() - sessionStart) / 1000;
        return sec >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60)
                : String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60);
    }
}
