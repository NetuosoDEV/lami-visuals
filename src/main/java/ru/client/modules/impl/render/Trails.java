package ru.client.modules.impl.render;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

public final class Trails extends Module {

    private final ModeSetting mode = register(new ModeSetting("Mode", "Ribbon", "Ribbon", "Line", "Particles"));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final NumberSetting length = register(new NumberSetting("Length", 1.0, 0.2, 3, 0.1));
    private final NumberSetting height = register(new NumberSetting("Height", 0.6, 0.05, 2.0, 0.05));
    private final NumberSetting spawnHeight = register(new NumberSetting("Spawn Height", 0.05, 0, 2.0, 0.05));
    private final NumberSetting width = register(new NumberSetting("Width", 0.08, 0.02, 0.3, 0.01));
    private final NumberSetting glow = register(new NumberSetting("Glow", 1.0, 0, 2, 0.1));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", 0.6, 0.1, 1, 0.05));
    private final ModeSetting texture = register(new ModeSetting("Texture", "Sparkle", ParticleTexture.modes()));
    private final NumberSetting density = register(new NumberSetting("Density", 8, 1, 30, 1));
    private final NumberSetting size = register(new NumberSetting("Size", 0.08, 0.03, 0.25, 0.01));
    private final BooleanSetting physics = register(new BooleanSetting("Physics", true));

    private record Point(Vec3 pos, long time) {}

    private final ArrayDeque<Point> points = new ArrayDeque<>();
    private final ParticleSystem particles = new ParticleSystem(1500);
    private final Render3D.TexBuf beamBuf = new Render3D.TexBuf(), whiteBuf = new Render3D.TexBuf();
    private Vec3 lastEmit;
    private double emitDebt;

    public Trails() {
        super("Trails", "След за игроком", Category.RENDER);
        customColor.visibleWhen(() -> color.is("Custom"));
        height.visibleWhen(() -> !mode.is("Line"));
        width.visibleWhen(() -> mode.is("Line"));
        glow.visibleWhen(() -> mode.is("Line"));
        alpha.visibleWhen(() -> !mode.is("Particles"));
        texture.visibleWhen(() -> mode.is("Particles"));
        density.visibleWhen(() -> mode.is("Particles"));
        size.visibleWhen(() -> mode.is("Particles"));
        physics.visibleWhen(() -> mode.is("Particles"));
    }

    @Override
    protected void onDisable() {
        points.clear();
        particles.clear();
        lastEmit = null;
    }

    private int colorAt(float t) {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(3000, (int) (t * 1500), 0.65f, 1f, 255);
            case "Custom" -> ColorUtils.lerp(customColor.getArgb(), ColorUtils.lerp(customColor.getArgb(), 0xFFFFFFFF, 0.4f), t);
            default -> ColorUtils.lerp(Theme.get().accentA(), Theme.get().accentB(), t);
        };
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) { points.clear(); lastEmit = null; return; }
        long now = System.currentTimeMillis();
        Vec3 pos = mc.player.getPosition(td);

        if (mode.is("Particles")) {
            points.clear();
            emit(pos);
            boolean ph = physics.isEnabled();
            particles.render(ms, camera, new ParticleSystem.Physics(ph, 6f, ph ? 0.6f : 0.5f, 0.35f), true);
            return;
        }
        particles.clear();
        lastEmit = null;

        long lifeMs = (long) (length.get() * 1000);
        Point last = points.peekLast();
        if (last == null || last.pos().distanceToSqr(pos) > 0.0025) points.addLast(new Point(pos, now));
        while (!points.isEmpty() && now - points.peekFirst().time() > lifeMs) points.pollFirst();
        if (points.size() < 2) return;

        List<Point> list = new ArrayList<>(points);
        int n = list.size();
        float a = alpha.getFloat(), h = height.getFloat(), base = spawnHeight.getFloat();
        float[] fade = new float[n];
        for (int i = 0; i < n; i++) fade[i] = 1f - (now - list.get(n - 1 - i).time()) / (float) lifeMs;

        beamBuf.clear();
        whiteBuf.clear();
        if (mode.is("Line")) {
            List<Vec3> line = new ArrayList<>(n);
            for (int i = 0; i < n; i++) line.add(list.get(n - 1 - i).pos().add(0, base, 0));
            float w = width.getFloat(), g = glow.getFloat();
            if (g > 0.01f) {
                Render3D.beam(ms, beamBuf, line, false, t -> w * (2.5f + 2.5f * g) * (1f - t * 0.7f),
                        t -> ColorUtils.multAlpha(colorAt(t), a * 0.35f * Math.min(1f, g) * fadeAt(fade, t)));
            }
            Render3D.beam(ms, beamBuf, line, false, t -> w * (1f - t * 0.8f),
                    t -> ColorUtils.multAlpha(ColorUtils.lerp(colorAt(t), 0xFFFFFFFF, 0.35f), a * fadeAt(fade, t)));
        } else {
            Vec3[] bottom = new Vec3[n], top = new Vec3[n];
            int[] cb = new int[n], ct = new int[n];
            List<Vec3> edge = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                Vec3 p = list.get(n - 1 - i).pos();
                float t = i / (float) (n - 1);
                bottom[i] = p.add(0, base, 0);
                top[i] = p.add(0, base + h, 0);
                cb[i] = ColorUtils.multAlpha(colorAt(t), a * fade[i]);
                ct[i] = ColorUtils.withAlpha(colorAt(t), 0);
                edge.add(bottom[i]);
            }
            Render3D.band(ms, whiteBuf, bottom, top, cb, ct, 0, 1);
            Render3D.beam(ms, beamBuf, edge, false, t -> 0.07f * (1f - t * 0.6f),
                    t -> ColorUtils.multAlpha(colorAt(t), a * fadeAt(fade, t)));
        }
        if (!whiteBuf.isEmpty()) Render3D.drawTextured(Render3D.FX_WHITE, whiteBuf);
        if (!beamBuf.isEmpty()) Render3D.drawTextured(Render3D.FX_BEAM, beamBuf);
    }

    private static float fadeAt(float[] fade, float t) {
        int i = Math.min(fade.length - 1, Math.max(0, Math.round(t * (fade.length - 1))));
        return Math.max(0f, fade[i]);
    }

    private void emit(Vec3 pos) {
        if (lastEmit == null || lastEmit.distanceToSqr(pos) > 64) { lastEmit = pos; emitDebt = 0; return; }
        double dist = lastEmit.distanceTo(pos);
        if (dist < 1e-3) return;
        emitDebt += dist * density.get();
        int n = (int) emitDebt;
        emitDebt -= n;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean ph = physics.isEnabled();
        float life = length.getFloat() * 1.5f;
        for (int i = 0; i < n; i++) {
            Vec3 p0 = lastEmit.lerp(pos, (i + r.nextDouble()) / n);
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = p0.x + r.nextDouble(-0.2, 0.2);
            p.y = p0.y + spawnHeight.get() + r.nextDouble(0, Math.max(0.01, height.get()));
            p.z = p0.z + r.nextDouble(-0.2, 0.2);
            p.vx = r.nextDouble(-0.4, 0.4);
            p.vy = ph ? r.nextDouble(0.8, 2.0) : r.nextDouble(0.05, 0.35);
            p.vz = r.nextDouble(-0.4, 0.4);
            if (!ph) {
                p.swayAmp = 0.15f;
                p.swayFreq = (float) r.nextDouble(1, 2.5);
                p.swayPhase = (float) r.nextDouble(Math.PI * 2);
            }
            p.size = (float) (size.get() * r.nextDouble(0.75, 1.2));
            p.rotation = (float) r.nextDouble(Math.PI * 2);
            p.rotationSpeed = (float) r.nextDouble(-4, 4);
            p.lifeMs = (int) (life * 1000 * r.nextDouble(0.8, 1.2));
            p.color = colorAt((float) r.nextDouble());
            p.texture = ParticleTexture.pick(texture.get());
            particles.add(p);
        }
        lastEmit = pos;
    }
}
