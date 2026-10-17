package ru.client.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.particles.JumpTexture;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class TargetEsp extends Module {

    private final ModeSetting mode = register(new ModeSetting("Mode", "Circle", "Circle", "Orbit", "Ribbons", "Comets", "Seal"));
    private final ModeSetting targets = register(new ModeSetting("Targets", "Players", "Players", "All"));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final NumberSetting speed = register(new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting ribbons = register(new NumberSetting("Ribbons", 3, 1, 4, 1));
    private final ModeSetting sealTexture = register(new ModeSetting("Seal Texture", "Mandala", JumpTexture.titles()));
    private final BooleanSetting hitFlash = register(new BooleanSetting("Hit Flash", true));

    private final Animation appear = new Animation(8f, 0f);
    private LivingEntity displayed;

    private final Map<Identifier, Render3D.TexBuf> bufs = new LinkedHashMap<>();
    private final ParticleSystem sparks = new ParticleSystem(240);
    private static final ParticleSystem.Physics FLOAT = new ParticleSystem.Physics(false, 0, 0.35f, 0);
    private long lastSparkAt;

    public TargetEsp() {
        super("TargetESP", "Анимированная подсветка цели", Category.COMBAT);
        customColor.visibleWhen(() -> color.is("Custom"));
        ribbons.visibleWhen(() -> mode.is("Ribbons"));
        sealTexture.visibleWhen(() -> mode.is("Seal"));
    }

    @Override
    protected void onDisable() {
        sparks.clear();
        displayed = null;
        appear.snap(0f);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) return;
        LivingEntity t = CombatTracker.getTarget(4000);
        if (t != null && targets.is("Players") && !(t instanceof Player)) t = null;
        if (t != null) { displayed = t; appear.setTarget(1f); } else appear.setTarget(0f);
        float a = appear.update();
        for (Render3D.TexBuf b : bufs.values()) b.clear();

        if (a >= 0.02f && displayed != null && !displayed.isRemoved()) {
            LivingEntity e = displayed;
            Vec3 pos = e.getPosition(td);
            float flash = hitFlash.isEnabled() ? e.hurtTime / 10f : 0f;
            int cA = ColorUtils.lerp(colorA(), 0xFFFF3030, flash);
            int cB = ColorUtils.lerp(colorB(), 0xFFFF3030, flash);
            double time = System.currentTimeMillis() / 1000.0 * speed.get() * (1 + flash);
            Render3D.Billboard bb = new Render3D.Billboard(ms, camera);
            switch (mode.get()) {
                case "Orbit" -> orbit(bb, e, pos, time, cA, cB, a);
                case "Ribbons" -> ribbons(ms, bb, e, pos, time, cA, cB, a);
                case "Comets" -> comets(ms, bb, e, pos, time, cA, cB, a);
                case "Seal" -> seal(ms, e, pos, time, cA, cB, a, flash);
                default -> circle(ms, e, pos, time, cA, cB, a);
            }
        }
        sparks.render(ms, camera, FLOAT, false);
        for (Map.Entry<Identifier, Render3D.TexBuf> en : bufs.entrySet()) {
            if (!en.getValue().isEmpty()) Render3D.drawTextured(en.getKey(), en.getValue());
        }
    }

    private Render3D.TexBuf buf(Identifier texture) {
        return bufs.computeIfAbsent(texture, k -> new Render3D.TexBuf());
    }

    private int colorA() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(4000, 0, 0.65f, 1f, 255);
            case "Custom" -> customColor.getArgb();
            default -> Theme.get().accentA();
        };
    }

    private int colorB() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(4000, 1400, 0.65f, 1f, 255);
            case "Custom" -> ColorUtils.lerp(customColor.getArgb(), 0xFFFFFFFF, 0.35f);
            default -> Theme.get().accentB();
        };
    }

    private static Vec3 ringPoint(double cx, double cy, double cz, double r, double theta, double tilt, double yaw) {
        double x = Math.cos(theta) * r, z = Math.sin(theta) * r;
        double y = z * Math.sin(tilt);
        z = z * Math.cos(tilt);
        double rx = x * Math.cos(yaw) - z * Math.sin(yaw), rz = x * Math.sin(yaw) + z * Math.cos(yaw);
        return new Vec3(cx + rx, cy + y, cz + rz);
    }

    private void circle(PoseStack ms, LivingEntity e, Vec3 pos, double time, int cA, int cB, float a) {
        float radius = e.getBbWidth() * 0.8f + 0.15f;
        double wave = Math.sin(time * 2.2) * 0.5 + 0.5;
        double y = pos.y + wave * e.getBbHeight();
        double tail = -Math.cos(time * 2.2) * 0.35;
        int seg = 72;
        Vec3[] ring = new Vec3[seg + 1], top = new Vec3[seg + 1];
        int[] ringCol = new int[seg + 1], topCol = new int[seg + 1];
        List<Vec3> loop = new ArrayList<>(seg + 1);
        for (int i = 0; i <= seg; i++) {
            float t = i / (float) seg;
            double ang = Math.PI * 2 * t;
            double x = pos.x + Math.cos(ang) * radius, z = pos.z + Math.sin(ang) * radius;
            ring[i] = new Vec3(x, y, z);
            top[i] = new Vec3(x, y + tail, z);
            int c = ColorUtils.lerp(cA, cB, (float) (Math.sin(t * Math.PI * 2 + time) * 0.5 + 0.5));
            ringCol[i] = ColorUtils.multAlpha(c, a * 0.45f);
            topCol[i] = ColorUtils.withAlpha(c, 0);
            loop.add(ring[i]);
        }
        Render3D.band(ms, buf(Render3D.FX_WHITE), ring, top, ringCol, topCol, 0, 1);
        Render3D.beam(ms, buf(Render3D.FX_BEAM), loop, true, Render3D.FloatFn.of(0.12f),
                t -> ColorUtils.multAlpha(ColorUtils.lerp(cA, cB, (float) (Math.sin(t * Math.PI * 2 + time) * 0.5 + 0.5)), a));
    }

    private void orbit(Render3D.Billboard bb, LivingEntity e, Vec3 pos, double time, int cA, int cB, float a) {
        Render3D.TexBuf orb = buf(ParticleTexture.ORB.texture);
        double cy = pos.y + e.getBbHeight() * 0.55;
        float radius = e.getBbWidth() * 0.9f + 0.2f;
        int orbs = 3, trail = 12;
        for (int o = 0; o < orbs; o++) {
            double tilt = Math.toRadians(o * 60 - 60);
            int c = ColorUtils.lerp(cA, cB, o / (float) (orbs - 1));
            for (int k = 0; k < trail; k++) {
                double ang = time * 3 + o * (Math.PI * 2 / orbs) - k * 0.09;
                Vec3 p = ringPoint(pos.x, cy, pos.z, radius, ang, tilt, 0);
                float fade = 1f - k / (float) trail;
                float size = 0.11f * (0.4f + 0.6f * fade);
                bb.texturedQuad(orb, p.x, p.y, p.z, size * 2.6f, 0, ColorUtils.multAlpha(c, a * fade * 0.6f));
                bb.texturedQuad(orb, p.x, p.y, p.z, size * 0.9f, 0, ColorUtils.multAlpha(ColorUtils.lerp(c, 0xFFFFFFFF, 0.7f), a * fade));
            }
        }
    }

    private void ribbons(PoseStack ms, Render3D.Billboard bb, LivingEntity e, Vec3 pos, double time, int cA, int cB, float a) {
        int n = ribbons.getInt();
        double r = e.getBbWidth() * 0.85 + 0.3;
        int pts = 44;
        double arc = Math.PI * 1.05;
        Render3D.TexBuf band = buf(Render3D.FX_BEAM), orb = buf(ParticleTexture.ORB.texture);
        for (int k = 0; k < n; k++) {
            double dir = k % 2 == 0 ? 1 : -1;
            double tilt = Math.toRadians((k % 2 == 0 ? 1 : -1) * (22 + 26 * (k / 2)));
            double yaw = k * Math.PI / Math.max(1, n) + time * 0.35;
            double cy = pos.y + e.getBbHeight() * (0.5 + 0.18 * Math.sin(time * 1.3 + k * 1.7));
            double head = time * (2.3 + 0.35 * k) * dir;
            int c = n == 1 ? cA : ColorUtils.lerp(cA, cB, k / (float) (n - 1));
            Vec3[] edgeA = new Vec3[pts], edgeB = new Vec3[pts];
            int[] colA = new int[pts], colB = new int[pts];
            Vec3 normal = ringPoint(0, 0, 0, 1, Math.PI / 2, tilt + Math.PI / 2, yaw);
            for (int j = 0; j < pts; j++) {
                float t = j / (float) (pts - 1);
                double theta = head - arc * (1 - t) * dir;
                Vec3 p = ringPoint(pos.x, cy, pos.z, r, theta, tilt, yaw);
                double w = 0.17 * (0.3 + 0.7 * t) * 0.5;
                edgeA[j] = p.add(normal.scale(w));
                edgeB[j] = p.subtract(normal.scale(w));
                int cc = ColorUtils.multAlpha(ColorUtils.lerp(cB, c, t), a * t * (float) Math.sqrt(t));
                colA[j] = cc;
                colB[j] = cc;
            }
            Render3D.band(ms, band, edgeA, edgeB, colA, colB, 0, 1);
            Vec3 h = ringPoint(pos.x, cy, pos.z, r, head, tilt, yaw);
            bb.texturedQuad(orb, h.x, h.y, h.z, 0.2f, 0, ColorUtils.multAlpha(c, a * 0.8f));
        }
    }

    private void comets(PoseStack ms, Render3D.Billboard bb, LivingEntity e, Vec3 pos, double time, int cA, int cB, float a) {
        double cy = pos.y + e.getBbHeight() * 0.55;
        double r = e.getBbWidth() * 0.9 + 0.35;
        int count = 3, pts = 34;
        double tailArc = 2.3;
        Render3D.TexBuf beam = buf(Render3D.FX_BEAM), orb = buf(ParticleTexture.ORB.texture), spark = buf(ParticleTexture.SPARKLE.texture);
        long now = System.currentTimeMillis();
        boolean emit = now - lastSparkAt > 45;
        if (emit) lastSparkAt = now;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int o = 0; o < count; o++) {
            double tilt = Math.toRadians(-50 + o * 50);
            double yaw = o * 0.9;
            double head = time * 2.6 + o * (Math.PI * 2 / count);
            int c = ColorUtils.lerp(cA, cB, o / (float) (count - 1));
            List<Vec3> path = new ArrayList<>(pts);
            for (int j = 0; j < pts; j++) {
                double theta = head - tailArc * (1 - j / (double) (pts - 1));
                path.add(ringPoint(pos.x, cy, pos.z, r, theta, tilt, yaw));
            }
            Render3D.beam(ms, beam, path, false, t -> 0.2f * t * t,
                    t -> ColorUtils.multAlpha(ColorUtils.lerp(cB, c, t), a * t));
            Vec3 h = path.get(pts - 1);
            bb.texturedQuad(orb, h.x, h.y, h.z, 0.32f, 0, ColorUtils.multAlpha(c, a * 0.7f));
            bb.texturedQuad(spark, h.x, h.y, h.z, 0.17f, (float) (time * 3 + o), ColorUtils.multAlpha(0xFFFFFFFF, a));
            if (emit && a > 0.5f) {
                ParticleSystem.Particle p = new ParticleSystem.Particle();
                p.x = h.x; p.y = h.y; p.z = h.z;
                p.vx = rnd.nextDouble(-0.3, 0.3); p.vy = rnd.nextDouble(-0.4, 0.1); p.vz = rnd.nextDouble(-0.3, 0.3);
                p.size = (float) rnd.nextDouble(0.025, 0.045);
                p.lifeMs = (int) rnd.nextDouble(350, 650);
                p.color = c;
                p.texture = ParticleTexture.SPARKLE;
                p.rotationSpeed = (float) rnd.nextDouble(-6, 6);
                sparks.add(p);
            }
        }
    }

    private void seal(PoseStack ms, LivingEntity e, Vec3 pos, double time, int cA, int cB, float a, float flash) {
        JumpTexture tex = JumpTexture.pick(sealTexture.get());
        if (tex == null) tex = JumpTexture.MANDALA;
        float radius = (e.getBbWidth() * 0.9f + 0.55f) * (0.7f + 0.3f * a) * (1f + flash * 0.15f);
        float pulse = 1f + 0.04f * (float) Math.sin(time * 3.0);
        double y = pos.y + 0.03;
        Render3D.TexBuf s = buf(tex.texture);
        Render3D.flatTexturedQuad(ms, s, pos.x, y, pos.z, radius * pulse / JumpTexture.RADIUS_FRACTION, (float) (time * 1.1), ColorUtils.multAlpha(cA, a));
        Render3D.flatTexturedQuad(ms, s, pos.x, y + 0.01, pos.z, radius * 0.62f / JumpTexture.RADIUS_FRACTION, (float) (-time * 1.6), ColorUtils.multAlpha(cB, a * 0.6f));
        int seg = 48;
        float h = e.getBbHeight() * 0.45f;
        Vec3[] bottom = new Vec3[seg + 1], top = new Vec3[seg + 1];
        int[] cb = new int[seg + 1], ct = new int[seg + 1];
        for (int i = 0; i <= seg; i++) {
            double ang = Math.PI * 2 * i / seg;
            double x = pos.x + Math.cos(ang) * radius * 0.82, z = pos.z + Math.sin(ang) * radius * 0.82;
            bottom[i] = new Vec3(x, y, z);
            top[i] = new Vec3(x, y + h, z);
            cb[i] = ColorUtils.multAlpha(ColorUtils.lerp(cA, cB, i / (float) seg), a * 0.22f);
            ct[i] = ColorUtils.withAlpha(cB, 0);
        }
        Render3D.band(ms, buf(Render3D.FX_WHITE), bottom, top, cb, ct, 0, 1);
    }
}
