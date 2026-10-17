package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;

import java.util.concurrent.ThreadLocalRandom;

public final class ParticleRain extends Module {

    private static final String[] KINDS = {"Rain", "Fireflies", "Embers", "Meteors", "Vortex"};

    private final ModeSetting mode = register(new ModeSetting("Mode", "Rain", KINDS));
    private final ModeSetting texture = register(new ModeSetting("Texture", "Snowflake", ParticleTexture.modes()));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "White", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF9AD8FF));
    private final NumberSetting density = register(new NumberSetting("Density", 60, 5, 400, 5));
    private final NumberSetting radius = register(new NumberSetting("Radius", 14, 4, 40, 1));
    private final NumberSetting height = register(new NumberSetting("Height", 12, 4, 30, 1));
    private final NumberSetting size = register(new NumberSetting("Size", 0.14, 0.04, 0.5, 0.01));
    private final NumberSetting fallSpeed = register(new NumberSetting("Fall Speed", 2.5, 0.3, 12, 0.1));
    private final NumberSetting flowSpeed = register(new NumberSetting("Flow Speed", 1.0, 0.2, 3, 0.1));
    private final NumberSetting sway = register(new NumberSetting("Sway", 0.6, 0, 3, 0.1));
    private final NumberSetting spin = register(new NumberSetting("Spin", 1.5, 0, 8, 0.1));
    private final NumberSetting life = register(new NumberSetting("Lifetime", 6, 1, 15, 0.5));
    private final BooleanSetting physics = register(new BooleanSetting("Physics", true));
    private final NumberSetting gravity = register(new NumberSetting("Gravity", 3, 0.5, 15, 0.5));
    private final NumberSetting bounce = register(new NumberSetting("Bounce", 0.3, 0, 0.9, 0.05));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final BooleanSetting skyOnly = register(new BooleanSetting("Sky Only", true));
    private final ModeSetting layer2 = register(new ModeSetting("Layer 2", "Off", "Off", "Rain", "Fireflies", "Embers", "Meteors", "Vortex"));
    private final ModeSetting texture2 = register(new ModeSetting("Layer 2 Texture", "Orb", ParticleTexture.modes()));
    private final NumberSetting density2 = register(new NumberSetting("Layer 2 Density", 30, 5, 400, 5));

    private final Layer main = new Layer(), second = new Layer();

    private final class Layer {
        final ParticleSystem system = new ParticleSystem(2500);
        long lastNs = System.nanoTime();
        double debt;

        void reset() { lastNs = System.nanoTime(); debt = 0; }

        void tick(PoseStack ms, Camera camera, String kind, String tex, double perSecond) {
            long now = System.nanoTime();
            double dt = Math.min((now - lastNs) / 1e9, 0.1);
            lastNs = now;
            debt += perSecond * rate(kind) * dt;
            int n = (int) debt;
            debt -= n;
            if (n > 0) spawn(system, kind, tex, n);
            boolean ph = physics.isEnabled() && kind.equals("Rain");
            ParticleSystem.Physics phys = new ParticleSystem.Physics(ph, gravity.getFloat(), ph ? 0.6f : 1f, bounce.getFloat());
            system.render(ms, camera, phys, glow.isEnabled(), mover(kind));
        }
    }

    public ParticleRain() {
        super("ParticleRain", "Частицы вокруг тебя: дождь, светлячки, искры, метеоры, вихрь", Category.RENDER);
        customColor.visibleWhen(() -> color.is("Custom"));
        fallSpeed.visibleWhen(() -> uses("Rain"));
        flowSpeed.visibleWhen(() -> !mode.is("Rain") || !layer2.is("Off") && !layer2.is("Rain"));
        sway.visibleWhen(() -> uses("Rain") || uses("Embers"));
        physics.visibleWhen(() -> uses("Rain"));
        gravity.visibleWhen(() -> uses("Rain") && physics.isEnabled());
        bounce.visibleWhen(() -> uses("Rain") && physics.isEnabled());
        skyOnly.visibleWhen(() -> uses("Rain") || uses("Meteors"));
        texture2.visibleWhen(() -> !layer2.is("Off"));
        density2.visibleWhen(() -> !layer2.is("Off"));
    }

    private boolean uses(String kind) { return mode.is(kind) || layer2.is(kind); }

    private static double rate(String kind) {
        return switch (kind) {
            case "Fireflies" -> 0.35;
            case "Embers" -> 0.6;
            case "Meteors" -> 0.04;
            case "Vortex" -> 0.5;
            default -> 1.0;
        };
    }

    @Override
    protected void onEnable() {
        main.reset();
        second.reset();
    }

    @Override
    protected void onDisable() {
        main.system.clear();
        second.system.clear();
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) return;
        main.tick(ms, camera, mode.get(), texture.get(), density.get());
        if (layer2.is("Off")) second.system.clear();
        else second.tick(ms, camera, layer2.get(), texture2.get(), density2.get());
    }

    private void spawn(ParticleSystem system, String kind, String tex, int n) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vec3 base = mc.player.position();
        double rad = radius.get();
        float flow = flowSpeed.getFloat();
        for (int i = 0; i < n; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            double a = r.nextDouble(Math.PI * 2), d = Math.sqrt(r.nextDouble()) * rad;
            double x = base.x + Math.cos(a) * d, z = base.z + Math.sin(a) * d;
            p.x = x;
            p.z = z;
            p.size = (float) (size.get() * r.nextDouble(0.7, 1.25));
            p.rotation = (float) r.nextDouble(Math.PI * 2);
            p.rotationSpeed = (float) (spin.get() * r.nextDouble(-1, 1));
            p.lifeMs = (int) (life.get() * 1000 * r.nextDouble(0.85, 1.1));
            p.color = color(r);
            p.texture = ParticleTexture.pick(tex);
            switch (kind) {
                case "Fireflies" -> {
                    p.y = base.y + r.nextDouble(-0.5, 3.5);
                    p.vx = r.nextDouble(-0.4, 0.4) * flow;
                    p.vy = r.nextDouble(-0.15, 0.15) * flow;
                    p.vz = r.nextDouble(-0.4, 0.4) * flow;
                    p.blinkFreq = (float) r.nextDouble(0.4, 1.2);
                    p.swayPhase = (float) r.nextDouble(Math.PI * 2);
                    p.fadeInMs = 900;
                    p.size *= 0.7f;
                }
                case "Embers" -> {
                    p.y = base.y + r.nextDouble(-1.0, 0.5);
                    p.vx = r.nextDouble(-0.2, 0.2);
                    p.vy = r.nextDouble(0.8, 1.8) * flow;
                    p.vz = r.nextDouble(-0.2, 0.2);
                    p.swayAmp = (float) (sway.get() * 0.6 * r.nextDouble(0.5, 1.0));
                    p.swayFreq = (float) r.nextDouble(1.0, 2.5);
                    p.swayPhase = (float) r.nextDouble(Math.PI * 2);
                    p.color2 = ColorUtils.lerp(p.color, 0xFF000000, 0.75f);
                    p.lifeMs = (int) (p.lifeMs * 0.6f);
                    p.size *= 0.75f;
                }
                case "Meteors" -> {
                    double ang = r.nextDouble(Math.PI * 2), dist = rad * r.nextDouble(0.6, 1.5);
                    p.x = base.x + Math.cos(ang) * dist;
                    p.z = base.z + Math.sin(ang) * dist;
                    p.y = base.y + height.get() + r.nextDouble(4, 14);
                    double dir = r.nextDouble(Math.PI * 2), v = r.nextDouble(18, 28) * flow;
                    double hx = Math.cos(dir), hz = Math.sin(dir), len = Math.sqrt(1 + 0.55 * 0.55);
                    p.vx = hx / len * v;
                    p.vy = -0.55 / len * v;
                    p.vz = hz / len * v;
                    p.tailTime = 0.13f;
                    p.lifeMs = (int) r.nextDouble(1000, 1800);
                    p.size *= 1.2f;
                    p.rotationSpeed *= 2;
                }
                case "Vortex" -> {
                    double vr = Math.max(2, Math.min(8, rad * 0.35));
                    double rr = vr * r.nextDouble(0.35, 1.0);
                    p.x = base.x + Math.cos(a) * rr;
                    p.z = base.z + Math.sin(a) * rr;
                    p.y = base.y + r.nextDouble(0, Math.max(2, height.get() * 0.4));
                    p.param1 = (float) rr;
                    p.param2 = (float) r.nextDouble(0.7, 1.3);
                    p.fadeInMs = 400;
                }
                default -> {
                    p.y = base.y + height.get() + r.nextDouble(-2, 2);
                    double v = fallSpeed.get() * r.nextDouble(0.75, 1.15);
                    p.vx = r.nextDouble(-0.15, 0.15);
                    p.vy = -v;
                    p.vz = r.nextDouble(-0.15, 0.15);
                    p.swayAmp = (float) (sway.get() * r.nextDouble(0.5, 1.0));
                    p.swayFreq = (float) r.nextDouble(0.8, 2.0);
                    p.swayPhase = (float) r.nextDouble(Math.PI * 2);
                }
            }
            if (skyOnly.isEnabled() && kind.equals("Rain")) {
                int top = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(p.x), (int) Math.floor(p.z));
                if (top > p.y) continue;
            }
            system.add(p);
        }
    }

    private int color(ThreadLocalRandom r) {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(4000, r.nextInt(4000), 0.6f, 1f, 255);
            case "White" -> 0xFFFFFFFF;
            case "Custom" -> customColor.getArgb();
            default -> ColorUtils.lerp(Theme.get().accentA(), Theme.get().accentB(), (float) r.nextDouble());
        };
    }

    private ParticleSystem.Mover mover(String kind) {
        return switch (kind) {
            case "Fireflies" -> this::wander;
            case "Vortex" -> this::swirl;
            default -> null;
        };
    }

    private void wander(ParticleSystem.Particle p, float dt, float time) {
        float flow = flowSpeed.getFloat();
        double t = time * 0.7 + p.swayPhase * 3;
        double tx = Math.sin(t * 1.3 + p.swayPhase) * 0.5 * flow;
        double ty = Math.sin(t * 0.9 + p.swayPhase * 2) * 0.18 * flow;
        double tz = Math.cos(t * 1.1 + p.swayPhase * 1.5) * 0.5 * flow;
        if (mc.player != null) {
            Vec3 c = mc.player.position();
            double dx = c.x - p.x, dz = c.z - p.z, dist = Math.sqrt(dx * dx + dz * dz), lim = radius.get();
            if (dist > lim) { tx += dx / dist * 0.6 * flow; tz += dz / dist * 0.6 * flow; }
            double dy = c.y + 1.2 - p.y;
            if (Math.abs(dy) > 3) ty += Math.signum(dy) * 0.3 * flow;
        }
        double k = Math.min(1, dt * 1.5);
        p.vx += (tx - p.vx) * k;
        p.vy += (ty - p.vy) * k;
        p.vz += (tz - p.vz) * k;
    }

    private void swirl(ParticleSystem.Particle p, float dt, float time) {
        if (mc.player == null) return;
        Vec3 c = mc.player.position();
        double dx = p.x - c.x, dz = p.z - c.z;
        double dist = Math.max(0.2, Math.sqrt(dx * dx + dz * dz));
        double want = p.param1 * (1 + 0.15 * Math.sin(time * 1.3 + p.swayPhase + p.param1));
        double omega = flowSpeed.get() * 1.6 * p.param2 / (0.6 + p.param1 * 0.18);
        double tang = omega * dist;
        double vx = -dz / dist * tang + dx / dist * (want - dist) * 2.0;
        double vz = dx / dist * tang + dz / dist * (want - dist) * 2.0;
        double vy = 0.35 * flowSpeed.get() + Math.sin(time * 2 + p.param1 * 3) * 0.2;
        double k = Math.min(1, dt * 6);
        p.vx += (vx - p.vx) * k;
        p.vy += (vy - p.vy) * k;
        p.vz += (vz - p.vz) * k;
    }
}
