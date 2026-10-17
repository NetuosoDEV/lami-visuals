package ru.client.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;
import ru.client.core.ClientMod;
import ru.client.core.SessionTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.RenderUtils;
import ru.client.utils.render.WorldToScreen;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KillStreak extends Module {

    private final NumberSetting window = register(new NumberSetting("Window", 10, 3, 30, 1));
    private final NumberSetting minStreak = register(new NumberSetting("Min Streak", 2, 1, 5, 1));
    private final BooleanSetting playersOnly = register(new BooleanSetting("Players Only", false));
    private final BooleanSetting firstBlood = register(new BooleanSetting("First Blood", true));
    private final BooleanSetting bloodOnce = register(new BooleanSetting("Once Per Session", false));
    private final ModeSetting color = register(new ModeSetting("Color", "Tier", "Tier", "Theme", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    private final NumberSetting duration = register(new NumberSetting("Duration", 2.2, 1.0, 5.0, 0.1));
    private final BooleanSetting shockwave = register(new BooleanSetting("Shockwave", true));
    private final BooleanSetting sparks = register(new BooleanSetting("Sparks", true));
    private final BooleanSetting sound = register(new BooleanSetting("Sound", true));
    private static final boolean DOTA = KillStreak.class.getResource("/assets/lamivisuals_dota/sounds.json") != null;
    private final ModeSetting soundPack = register(DOTA
            ? new ModeSetting("Sound Pack", "LamiVisuals", "LamiVisuals", "Dota 2")
            : new ModeSetting("Sound Pack", "LamiVisuals", "LamiVisuals"));
    private final NumberSetting volume = register(new NumberSetting("Volume", 0.7, 0.1, 1.0, 0.05));

    private static final SoundEvent STREAK = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "fx.streak"));
    private static final SoundEvent DOTA_FIRST_BLOOD = dota("first_blood"), DOTA_DOUBLE = dota("double_kill"),
            DOTA_TRIPLE = dota("triple_kill"), DOTA_ULTRA = dota("ultra_kill"), DOTA_RAMPAGE = dota("rampage"),
            DOTA_UNSTOPPABLE = dota("unstoppable"), DOTA_WICKED_SICK = dota("wicked_sick"), DOTA_MONSTER = dota("monster_kill"),
            DOTA_GODLIKE = dota("godlike");
    private static final Tier FIRST_BLOOD = new Tier("FIRST BLOOD", "", 0xFFFF3B3B, false);
    private static final ParticleSystem.Physics SPARK = new ParticleSystem.Physics(true, 5f, 0.5f, 0.3f);
    private static final ParticleSystem.Physics FLOAT = new ParticleSystem.Physics(false, 0f, 0.6f, 0f);

    private record Tier(String title, String sub, int color, boolean rainbow) {}

    private static final class Popup {
        Vec3 ground, text;
        long born;
        int count;
        Tier tier;
        float[] tilt;
        boolean burst, dissolved;
    }

    private final List<Popup> popups = new ArrayList<>();
    private final ParticleSystem bursts = new ParticleSystem(500);
    private final ParticleSystem motes = new ParticleSystem(300);
    private final Render3D.TexBuf beamBuf = new Render3D.TexBuf();
    private int streak;
    private long lastKillAt;
    private int bloodSession = -1;

    public KillStreak() {
        super("KillStreak", "Надписи серии убийств: x2, x3, RAMPAGE, GODLIKE…", Category.COMBAT);
        customColor.visibleWhen(() -> color.is("Custom"));
        volume.visibleWhen(sound::isEnabled);
        soundPack.visibleWhen(() -> DOTA && sound.isEnabled());
        bloodOnce.visibleWhen(firstBlood::isEnabled);
    }

    private static SoundEvent dota(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID + "_dota", "streak." + name));
    }

    @Override
    protected void onDisable() {
        popups.clear();
        bursts.clear();
        motes.clear();
        streak = 0;
    }

    private static Tier tier(int n, boolean dota) {
        if (dota) return dotaTier(n);
        return switch (n) {
            case 1 -> new Tier("KILL", "", 0xFFFFFFFF, false);
            case 2 -> new Tier("x2", "DOUBLE KILL", 0xFF4FD8FF, false);
            case 3 -> new Tier("x3", "TRIPLE KILL", 0xFF7CFF6B, false);
            case 4 -> new Tier("x4", "QUADRA KILL", 0xFFFFD84A, false);
            case 5 -> new Tier("RAMPAGE", "5 KILLS", 0xFFFF8A3D, false);
            case 6 -> new Tier("UNSTOPPABLE", "6 KILLS", 0xFFFF4A6A, false);
            case 7 -> new Tier("GODLIKE", "7 KILLS", 0xFFC86BFF, false);
            default -> new Tier("LEGENDARY", n + " KILLS", 0xFFFFE9A8, true);
        };
    }

    private static Tier dotaTier(int n) {
        return switch (n) {
            case 1 -> new Tier("KILL", "", 0xFFFFFFFF, false);
            case 2 -> new Tier("x2", "DOUBLE KILL", 0xFF4FD8FF, false);
            case 3 -> new Tier("x3", "TRIPLE KILL", 0xFF7CFF6B, false);
            case 4 -> new Tier("x4", "ULTRA KILL", 0xFFFFD84A, false);
            case 5 -> new Tier("RAMPAGE", "5 KILLS", 0xFFFF8A3D, false);
            case 6 -> new Tier("UNSTOPPABLE", "6 KILLS", 0xFFFF4A6A, false);
            case 7 -> new Tier("WICKED SICK", "7 KILLS", 0xFFC86BFF, false);
            case 8 -> new Tier("MONSTER KILL", "8 KILLS", 0xFFFF5CD8, false);
            default -> new Tier("GODLIKE", n + " KILLS", 0xFFFFE9A8, true);
        };
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.player.isDeadOrDying()) streak = 0;
    }

    @Override
    public void onKill(LivingEntity target) {
        if (playersOnly.isEnabled() && !(target instanceof Player)) return;
        long now = System.currentTimeMillis();
        streak = now - lastKillAt <= window.get() * 1000 ? streak + 1 : 1;
        lastKillAt = now;
        boolean newSession = bloodSession != SessionTracker.session();
        bloodSession = SessionTracker.session();
        boolean blood = streak == 1 && firstBlood.isEnabled() && (!bloodOnce.isEnabled() || newSession);
        boolean dota = DOTA && soundPack.is("Dota 2");
        if (streak < minStreak.getInt() && !blood) return;
        Popup p = new Popup();
        p.ground = target.position();
        p.text = p.ground.add(0, target.getBbHeight() + 0.5, 0);
        p.born = now;
        p.count = streak;
        p.tier = blood ? FIRST_BLOOD : tier(streak, dota);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        p.tilt = new float[p.tier.title().length()];
        for (int i = 0; i < p.tilt.length; i++) p.tilt[i] = (float) r.nextDouble(-0.5, 0.5);
        popups.add(p);
        if (popups.size() > 4) popups.remove(0);
        if (sound.isEnabled()) {
            if (dota) {
                SoundEvent voice = blood ? DOTA_FIRST_BLOOD : dotaVoice(streak);
                if (voice != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(voice, 1f, volume.getFloat()));
            } else {
                float pitch = Math.min(1.5f, 0.85f + 0.08f * Math.max(0, streak - 2));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(STREAK, pitch, volume.getFloat()));
            }
        }
    }

    private static SoundEvent dotaVoice(int n) {
        return switch (n) {
            case 1 -> null;
            case 2 -> DOTA_DOUBLE;
            case 3 -> DOTA_TRIPLE;
            case 4 -> DOTA_ULTRA;
            case 5 -> DOTA_RAMPAGE;
            case 6 -> DOTA_UNSTOPPABLE;
            case 7 -> DOTA_WICKED_SICK;
            case 8 -> DOTA_MONSTER;
            default -> DOTA_GODLIKE;
        };
    }

    private int baseColor(Popup p) {
        return switch (color.get()) {
            case "Theme" -> Theme.get().accentA();
            case "Custom" -> customColor.getArgb();
            default -> p.tier.color();
        };
    }

    private int letterColor(Popup p, int i, int n, float age) {
        float k = n <= 1 ? 0 : i / (float) (n - 1);
        if (color.is("Theme")) return ColorUtils.lerp(Theme.get().accentA(), Theme.get().accentB(), k);
        if (p.tier.rainbow() && color.is("Tier")) return ColorUtils.rainbow(2000, (int) (k * 1400 + age * 300), 0.45f, 1f, 255);
        int c = baseColor(p);
        return ColorUtils.lerp(c, ColorUtils.lerp(c, 0xFFFFFFFF, 0.55f), k);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        long now = System.currentTimeMillis();
        float life = duration.getFloat();
        beamBuf.clear();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Popup p : popups) {
            float age = (now - p.born) / 1000f;
            int c = baseColor(p);
            if (!p.burst) {
                p.burst = true;
                if (sparks.isEnabled()) {
                    int n = 18 + Math.min(p.count, 8) * 4;
                    for (int i = 0; i < n; i++) {
                        ParticleSystem.Particle s = new ParticleSystem.Particle();
                        s.x = p.text.x; s.y = p.text.y; s.z = p.text.z;
                        double th = r.nextDouble(Math.PI * 2), up = r.nextDouble(0.3, 1.2), v = r.nextDouble(2.5, 5.5);
                        s.vx = Math.cos(th) * v * 0.7; s.vy = up * v * 0.8; s.vz = Math.sin(th) * v * 0.7;
                        s.size = (float) r.nextDouble(0.05, 0.1);
                        s.lifeMs = (int) r.nextDouble(700, 1300);
                        s.color = ColorUtils.lerp(c, 0xFFFFFFFF, (float) r.nextDouble(0, 0.5));
                        s.texture = r.nextBoolean() ? ParticleTexture.STAR : ParticleTexture.SPARKLE;
                        s.rotationSpeed = (float) r.nextDouble(-8, 8);
                        s.tailTime = 0.05f;
                        bursts.add(s);
                    }
                }
            }
            if (!p.dissolved && age > life - 0.45f) {
                p.dissolved = true;
                if (sparks.isEnabled()) {
                    for (int i = 0; i < 16; i++) {
                        ParticleSystem.Particle s = new ParticleSystem.Particle();
                        s.x = p.text.x + r.nextDouble(-0.6, 0.6); s.y = p.text.y + age * 0.25 + r.nextDouble(-0.1, 0.2); s.z = p.text.z + r.nextDouble(-0.6, 0.6);
                        s.vy = r.nextDouble(0.6, 1.4);
                        s.swayAmp = 0.3f; s.swayFreq = (float) r.nextDouble(1.5, 3); s.swayPhase = (float) r.nextDouble(6.28);
                        s.size = (float) r.nextDouble(0.03, 0.06);
                        s.lifeMs = (int) r.nextDouble(600, 1000);
                        s.color = c;
                        s.texture = ParticleTexture.SPARKLE;
                        motes.add(s);
                    }
                }
            }
            if (shockwave.isEnabled()) {
                for (int w = 0; w < 2; w++) {
                    float k = (age - w * 0.12f) / 0.7f;
                    if (k <= 0 || k >= 1) continue;
                    float e = 1f - (1f - k) * (1f - k) * (1f - k);
                    float radius = 0.3f + e * (2.4f + 0.15f * Math.min(p.count, 8));
                    float a = (1f - k) * (w == 0 ? 1f : 0.6f);
                    List<Vec3> loop = new ArrayList<>(57);
                    for (int i = 0; i <= 56; i++) {
                        double ang = Math.PI * 2 * i / 56;
                        loop.add(new Vec3(p.ground.x + Math.cos(ang) * radius, p.ground.y + 0.05, p.ground.z + Math.sin(ang) * radius));
                    }
                    Render3D.beam(ms, beamBuf, loop, true, Render3D.FloatFn.of(0.05f + 0.16f * (1f - k)), t -> ColorUtils.multAlpha(c, a));
                }
            }
            if (p.count >= 5 && age < 1.3f) {
                float k = age / 1.3f;
                float a = k < 0.15f ? k / 0.15f : 1f - (k - 0.15f) / 0.85f;
                List<Vec3> pillar = List.of(p.ground, p.ground.add(0, 9, 0));
                Render3D.beam(ms, beamBuf, pillar, false, t -> (0.9f - 0.6f * t) * (1f - k * 0.5f),
                        t -> ColorUtils.multAlpha(c, a * (1f - t) * 0.8f));
            }
        }
        if (!beamBuf.isEmpty()) Render3D.drawTextured(Render3D.FX_BEAM, beamBuf);
        bursts.render(ms, camera, SPARK, true);
        motes.render(ms, camera, FLOAT, false);
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static float easeOutCubic(float t) { t = 1 - t; return 1 - t * t * t; }

    private static float easeOutBack(float t) {
        float c1 = 1.9f, c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (popups.isEmpty()) return;
        long now = System.currentTimeMillis();
        float life = duration.getFloat();
        popups.removeIf(p -> now - p.born > life * 1000);
        for (Popup p : popups) drawPopup(ctx, p, (now - p.born) / 1000f, life);
    }

    private void drawPopup(GuiGraphicsExtractor ctx, Popup p, float age, float life) {
        Vector3f sc = WorldToScreen.project(p.text.add(0, age * 0.25, 0));
        if (sc == null) return;
        float k = Mth.clamp(7f / Math.max(1f, sc.z()), 0.45f, 1.4f) * scale.getFloat();
        String s = p.tier.title();
        int n = s.length();
        float size = 26f * k * (n > 6 ? 0.8f : 1f);
        float fs = size / Fonts.logoSize();
        float totalW = Fonts.width(s, Fonts.LOGO) * fs;
        float x0 = sc.x() - totalW / 2f, yc = sc.y();
        float exit = clamp01((age - (life - 0.45f)) / 0.45f);
        float appear = clamp01(age / 0.3f);
        int base = baseColor(p);

        RenderUtils.drawGlow(ctx, x0 - 6 * k, yc - size * 0.32f, totalW + 12 * k, size * 0.64f, size * 0.32f, 12 * k,
                ColorUtils.withAlpha(base, (int) (110 * appear * (1 - exit))));

        Matrix3x2fStack ms = ctx.pose();
        float sweep = (age * 0.9f) % 1.6f - 0.3f;
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(s.charAt(i));
            float prefix = Fonts.width(s.substring(0, i), Fonts.LOGO) * fs;
            float cwUnits = Fonts.width(ch, Fonts.LOGO);
            float cw = cwUnits * fs;
            float lt = age - i * 0.035f;
            if (lt <= 0) continue;
            float in = easeOutBack(clamp01(lt / 0.3f));
            float drop = (1 - easeOutCubic(clamp01(lt / 0.25f))) * -20 * k;
            float rot = p.tilt[i] * (1 - easeOutCubic(clamp01(lt / 0.4f)));
            float wave = (float) Math.sin(age * 4 + i * 0.55f) * 1.3f * k;
            float ex = clamp01((age - (life - 0.45f) - i * 0.02f) / 0.4f);
            float exE = ex * ex * ex;
            float dir = n <= 1 ? 0 : (i - (n - 1) / 2f) / ((n - 1) / 2f);
            float exX = dir * 30 * k * exE, exY = -28 * k * exE - (i % 2 == 0 ? 6 : 2) * k * ex;
            float alpha = clamp01(lt / 0.12f) * (1 - ex);
            if (alpha <= 0.02f) continue;
            int c = letterColor(p, i, n, age);
            float hl = Math.max(0, 1 - Math.abs((n <= 1 ? 0.5f : i / (float) (n - 1)) - sweep) * 4);
            c = ColorUtils.lerp(c, 0xFFFFFFFF, hl * 0.55f);
            float sc2 = fs * in * (1 + 0.35f * ex);

            ms.pushMatrix();
            ms.translate(x0 + prefix + cw / 2f + exX, yc + drop + wave + exY);
            ms.rotate(rot + dir * 0.8f * ex);
            ms.scale(sc2, sc2);
            Fonts.draw(ctx, ch, -cwUnits / 2f + 1.5f, -Fonts.logoSize() / 2f + 1.5f, ColorUtils.withAlpha(0xFF000000, (int) (alpha * 130)), false, Fonts.LOGO);
            Fonts.draw(ctx, ch, -cwUnits / 2f, -Fonts.logoSize() / 2f, ColorUtils.multAlpha(c, alpha), false, Fonts.LOGO);
            ms.popMatrix();
        }

        String sub = p.tier.sub();
        if (sub.isEmpty()) return;
        float subA = clamp01((age - 0.18f) / 0.25f) * (1 - exit);
        if (subA <= 0.02f) return;
        float track = (1.6f + 5f * (1 - easeOutCubic(clamp01((age - 0.18f) / 0.4f)))) * k;
        float ks = Math.max(0.6f, k);
        float w = 0;
        for (int i = 0; i < sub.length(); i++) w += Fonts.width(String.valueOf(sub.charAt(i)), Fonts.BOLD) * ks + track;
        w -= track;
        float sx = sc.x() - w / 2f, sy = yc + size * 0.42f + 3 * k;
        int sc3 = ColorUtils.withAlpha(0xFFFFFFFF, (int) (230 * subA));
        for (int i = 0; i < sub.length(); i++) {
            String ch = String.valueOf(sub.charAt(i));
            ms.pushMatrix();
            ms.translate(sx, sy);
            ms.scale(ks, ks);
            Fonts.draw(ctx, ch, 0, 0, sc3, true, Fonts.BOLD);
            ms.popMatrix();
            sx += Fonts.width(ch, Fonts.BOLD) * ks + track;
        }
    }
}
