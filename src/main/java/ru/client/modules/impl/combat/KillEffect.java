package ru.client.modules.impl.combat;

import ru.client.core.CustomSound;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KillEffect extends Module {

    private static final String[] EFFECTS = {"Lightning", "Burst", "Soul", "Disintegrate", "Confetti", "Glitch", "Tombstone", "Firework"};

    private final ModeSetting mode = register(new ModeSetting("Mode", "Lightning", modes()));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final ModeSetting sound = register(new ModeSetting("Sound", "Effect", CustomSound.withTitles("Effect", "Off")));
    private final NumberSetting volume = register(new NumberSetting("Volume", 0.7, 0.1, 1.0, 0.05));

    private record Bolt(List<Vec3> points, long born, int color) {}
    private record Soul(Vec3 pos, long born, int cA, int cB) {}
    private record Glitch(Vec3 pos, float width, float height, long born, long seed) {}
    private record Tomb(Vec3 pos, long born, int cA, int cB) {}
    private record Flash(Vec3 pos, long born, int color) {}

    private final List<Bolt> bolts = new ArrayList<>();
    private final List<Soul> souls = new ArrayList<>();
    private final List<Glitch> glitches = new ArrayList<>();
    private final List<Tomb> tombs = new ArrayList<>();
    private final List<Flash> flashes = new ArrayList<>();

    private static final ParticleSystem.Physics BURST = new ParticleSystem.Physics(true, 6f, 0.55f, 0.45f);
    private static final ParticleSystem.Physics DUST = new ParticleSystem.Physics(false, 0f, 0.7f, 0f);
    private static final ParticleSystem.Physics PAPER = new ParticleSystem.Physics(true, 4.5f, 0.3f, 0.1f);
    private static final ParticleSystem.Physics SHARDS = new ParticleSystem.Physics(true, 3.5f, 0.45f, 0.3f);
    private final ParticleSystem burst = new ParticleSystem(300);
    private final ParticleSystem dust = new ParticleSystem(900);
    private final ParticleSystem confetti = new ParticleSystem(500);
    private final ParticleSystem shards = new ParticleSystem(400);

    private final Map<Identifier, Render3D.TexBuf> bufs = new LinkedHashMap<>();

    public KillEffect() {
        super("KillEffect", "Эффект при убийстве", Category.COMBAT);
        customColor.visibleWhen(() -> color.is("Custom"));
        volume.visibleWhen(() -> !sound.is("Off"));
    }

    private static String[] modes() {
        String[] out = new String[EFFECTS.length + 1];
        System.arraycopy(EFFECTS, 0, out, 0, EFFECTS.length);
        out[EFFECTS.length] = "Random";
        return out;
    }

    @Override
    protected void onDisable() {
        bolts.clear();
        souls.clear();
        glitches.clear();
        tombs.clear();
        flashes.clear();
        burst.clear();
        dust.clear();
        confetti.clear();
        shards.clear();
    }

    private int colorA() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(3000, 0, 0.65f, 1f, 255);
            case "Custom" -> customColor.getArgb();
            default -> Theme.get().accentA();
        };
    }

    private int colorB() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(3000, 1100, 0.65f, 1f, 255);
            case "Custom" -> ColorUtils.lerp(customColor.getArgb(), 0xFFFFFFFF, 0.35f);
            default -> Theme.get().accentB();
        };
    }

    @Override
    public void onKill(LivingEntity target) {
        String m = mode.get();
        if (m.equals("Random")) m = EFFECTS[ThreadLocalRandom.current().nextInt(EFFECTS.length)];
        Vec3 pos = target.position();
        long now = System.currentTimeMillis();
        int cA = colorA(), cB = colorB();
        switch (m) {
            case "Burst" -> burst(target, cA, cB);
            case "Soul" -> souls.add(new Soul(pos, now, cA, cB));
            case "Disintegrate" -> disintegrate(target, cA, cB);
            case "Confetti" -> confetti(target, cA, cB);
            case "Glitch" -> glitches.add(new Glitch(pos, target.getBbWidth(), target.getBbHeight(), now, ThreadLocalRandom.current().nextLong()));
            case "Tombstone" -> tomb(pos, now, cA, cB);
            case "Firework" -> firework(target, now, cA, cB);
            default -> bolts.add(new Bolt(bolt(pos), now, cA));
        }
        playSound(m);
    }

    private void playSound(String effect) {
        if (sound.is("Off")) return;
        float vol = volume.getFloat();
        CustomSound custom = CustomSound.byTitle(sound.get());
        if (custom != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(custom.event, 1f, vol));
            return;
        }
        SoundEvent s;
        float pitch = 1f, base = 0.35f;
        switch (effect) {
            case "Lightning" -> { s = SoundEvents.LIGHTNING_BOLT_THUNDER; pitch = 1.2f; }
            case "Disintegrate" -> { s = SoundEvents.ILLUSIONER_MIRROR_MOVE; pitch = 0.8f; base = 0.5f; }
            case "Confetti" -> { s = SoundEvents.FIREWORK_ROCKET_TWINKLE; pitch = 1.2f; base = 0.45f; }
            case "Glitch" -> { s = SoundEvents.ENDERMAN_TELEPORT; pitch = 1.7f; base = 0.4f; }
            case "Tombstone" -> { s = SoundEvents.BELL_BLOCK; pitch = 0.6f; base = 0.3f; }
            case "Firework" -> { s = SoundEvents.FIREWORK_ROCKET_LARGE_BLAST; base = 0.5f; }
            default -> { s = SoundEvents.EXPERIENCE_ORB_PICKUP; pitch = 1.2f; }
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, Math.min(1f, base * vol / 0.7f)));
    }

    private static List<Vec3> bolt(Vec3 base) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<Vec3> pts = new ArrayList<>();
        double x = base.x, z = base.z;
        for (int i = 0; i <= 16; i++) {
            double y = base.y + 14 - i * (14.0 / 16);
            pts.add(new Vec3(x, y, z));
            x += r.nextDouble(-0.6, 0.6);
            z += r.nextDouble(-0.6, 0.6);
            x += (base.x - x) * 0.2;
            z += (base.z - z) * 0.2;
        }
        pts.set(pts.size() - 1, base);
        return pts;
    }

    private void burst(LivingEntity target, int cA, int cB) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 40; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = target.getX();
            p.y = target.getY() + target.getBbHeight() * 0.5;
            p.z = target.getZ();
            double theta = r.nextDouble(Math.PI * 2), phi = r.nextDouble(-0.2, 1.4), v = r.nextDouble(2, 6);
            p.vx = Math.cos(theta) * Math.cos(phi) * v;
            p.vy = Math.sin(phi) * v;
            p.vz = Math.sin(theta) * Math.cos(phi) * v;
            p.size = (float) r.nextDouble(0.08, 0.18);
            p.rotationSpeed = (float) r.nextDouble(-8, 8);
            p.lifeMs = (int) r.nextDouble(900, 1600);
            p.color = ColorUtils.lerp(cA, cB, (float) r.nextDouble());
            p.texture = r.nextBoolean() ? ParticleTexture.STAR : ParticleTexture.SPARKLE;
            burst.add(p);
        }
    }

    private void disintegrate(LivingEntity target, int cA, int cB) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        AABB box = target.getBoundingBox();
        Vec3 away = mc.player != null ? target.position().subtract(mc.player.position()) : new Vec3(1, 0, 0);
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
        double h = Math.max(0.3, box.getYsize());
        double inX = box.getXsize() * 0.2, inZ = box.getZsize() * 0.2;
        for (int i = 0; i < 160; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = r.nextDouble(box.minX + inX, box.maxX - inX + 1e-3);
            p.y = r.nextDouble(box.minY, box.maxY);
            p.z = r.nextDouble(box.minZ + inZ, box.maxZ - inZ + 1e-3);
            float k = (float) ((p.y - box.minY) / h);
            p.delayMs = (int) ((1 - k) * 450 + r.nextDouble(0, 90));
            double wind = r.nextDouble(0.4, 1.3);
            p.vx = away.x * wind + r.nextDouble(-0.2, 0.2);
            p.vy = r.nextDouble(0.3, 1.0);
            p.vz = away.z * wind + r.nextDouble(-0.2, 0.2);
            p.swayAmp = 0.25f;
            p.swayFreq = (float) r.nextDouble(1.5, 3.5);
            p.swayPhase = (float) r.nextDouble(Math.PI * 2);
            p.size = (float) r.nextDouble(0.025, 0.05);
            p.lifeMs = (int) r.nextDouble(900, 1500);
            p.color = ColorUtils.lerp(cA, cB, k);
            p.color2 = cB;
            p.rotationSpeed = (float) r.nextDouble(-4, 4);
            p.texture = r.nextFloat() < 0.7f ? ParticleTexture.ORB : ParticleTexture.DIAMOND;
            dust.add(p);
        }
    }

    private void confetti(LivingEntity target, int cA, int cB) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int[] palette = switch (color.get()) {
            case "Rainbow" -> null;
            default -> new int[]{cA, cB, ColorUtils.lerp(cA, 0xFFFFFFFF, 0.5f), ColorUtils.lerp(cB, 0xFFFFFFFF, 0.5f), 0xFFFFD166};
        };
        for (int i = 0; i < 80; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = target.getX() + r.nextDouble(-0.2, 0.2);
            p.y = target.getY() + target.getBbHeight() * 0.9;
            p.z = target.getZ() + r.nextDouble(-0.2, 0.2);
            p.vx = r.nextDouble(-2.5, 2.5);
            p.vy = r.nextDouble(4, 7);
            p.vz = r.nextDouble(-2.5, 2.5);
            p.size = (float) r.nextDouble(0.06, 0.09);
            p.textureId = Render3D.FX_CONFETTI;
            p.flipSpeed = (float) r.nextDouble(6, 14);
            p.rotation = (float) r.nextDouble(Math.PI * 2);
            p.rotationSpeed = (float) r.nextDouble(-5, 5);
            p.swayAmp = 0.6f;
            p.swayFreq = (float) r.nextDouble(2, 4);
            p.swayPhase = (float) r.nextDouble(Math.PI * 2);
            p.lifeMs = (int) r.nextDouble(2600, 3600);
            p.color = palette == null
                    ? ColorUtils.rainbow(1000, r.nextInt(1000), 0.75f, 1f, 255)
                    : palette[r.nextInt(palette.length)];
            confetti.add(p);
        }
    }

    private void tomb(Vec3 pos, long now, int cA, int cB) {
        tombs.add(new Tomb(pos, now, cA, cB));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = pos.x + r.nextDouble(-0.4, 0.4);
            p.y = pos.y + r.nextDouble(0, 0.3);
            p.z = pos.z + r.nextDouble(-0.4, 0.4);
            p.vy = r.nextDouble(0.4, 0.8);
            p.swayAmp = 0.2f;
            p.swayFreq = (float) r.nextDouble(1, 2);
            p.swayPhase = (float) r.nextDouble(Math.PI * 2);
            p.delayMs = 300 + r.nextInt(900);
            p.fadeInMs = 300;
            p.size = (float) r.nextDouble(0.04, 0.07);
            p.lifeMs = (int) r.nextDouble(1400, 2000);
            p.color = cB;
            p.texture = ParticleTexture.ORB;
            dust.add(p);
        }
    }

    private void firework(LivingEntity target, long now, int cA, int cB) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.6, 0);
        flashes.add(new Flash(c, now, cA));
        for (int i = 0; i < 48; i++) {
            double u = r.nextDouble(-1, 1), th = r.nextDouble(Math.PI * 2), s = Math.sqrt(1 - u * u);
            double v = r.nextDouble(5, 8);
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = c.x; p.y = c.y; p.z = c.z;
            p.vx = s * Math.cos(th) * v;
            p.vy = u * v + 1.5;
            p.vz = s * Math.sin(th) * v;
            p.size = (float) r.nextDouble(0.05, 0.075);
            p.tailTime = 0.09f;
            p.lifeMs = (int) r.nextDouble(1100, 1600);
            int col = ColorUtils.lerp(cA, cB, (float) r.nextDouble());
            p.color = ColorUtils.lerp(col, 0xFFFFFFFF, 0.6f);
            p.color2 = col;
            p.texture = ParticleTexture.SPARKLE;
            p.rotationSpeed = (float) r.nextDouble(-6, 6);
            shards.add(p);
        }
    }

    private Render3D.TexBuf buf(Identifier texture) {
        return bufs.computeIfAbsent(texture, k -> new Render3D.TexBuf());
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        long now = System.currentTimeMillis();
        for (Render3D.TexBuf b : bufs.values()) b.clear();
        Render3D.Billboard bb = new Render3D.Billboard(ms, camera);

        bolts.removeIf(b -> now - b.born() > 700);
        for (Bolt b : bolts) {
            float a = 1f - (now - b.born()) / 700f;
            Render3D.beam(ms, buf(Render3D.FX_BEAM), b.points(), false, Render3D.FloatFn.of(0.55f), t -> ColorUtils.multAlpha(b.color(), a * 0.8f));
            Render3D.beam(ms, buf(Render3D.FX_BEAM), b.points(), false, Render3D.FloatFn.of(0.16f), t -> ColorUtils.multAlpha(0xFFFFFFFF, a));
        }

        souls.removeIf(s -> now - s.born() > 1500);
        for (Soul s : souls) {
            float k = (now - s.born()) / 1500f;
            float a = 1f - k;
            double y = s.pos().y + k * 2.5;
            float radius = 0.4f + k * 0.8f;
            List<Vec3> loop = new ArrayList<>(65);
            for (int i = 0; i <= 64; i++) {
                double ang = Math.PI * 2 * i / 64;
                loop.add(new Vec3(s.pos().x + Math.cos(ang) * radius, y, s.pos().z + Math.sin(ang) * radius));
            }
            Render3D.beam(ms, buf(Render3D.FX_BEAM), loop, true, Render3D.FloatFn.of(0.14f),
                    t -> ColorUtils.multAlpha(ColorUtils.lerp(s.cA(), s.cB(), t), a));
            Render3D.flatTexturedQuad(ms, buf(ParticleTexture.ORB.texture), s.pos().x, y, s.pos().z,
                    radius * 1.6f, 0, ColorUtils.multAlpha(s.cB(), a * 0.45f));
        }

        glitches.removeIf(g -> now - g.born() > 900);
        for (Glitch g : glitches) glitch(bb, g, now);

        tombs.removeIf(t -> now - t.born() > 2600);
        for (Tomb t : tombs) {
            long age = now - t.born();
            float rise = Math.min(1f, age / 450f);
            rise = 1f - (1f - rise) * (1f - rise) * (1f - rise);
            float a = age < 2000 ? 1f : 1f - (age - 2000) / 600f;
            double cy = t.pos().y + 0.47 - 1.2 * (1 - rise);
            bb.uprightQuad(buf(Render3D.FX_TOMBSTONE), t.pos().x, cy, t.pos().z, 0.65f, 0.65f, ColorUtils.multAlpha(t.cA(), a));
            Render3D.flatTexturedQuad(ms, buf(ParticleTexture.ORB.texture), t.pos().x, t.pos().y + 0.02, t.pos().z,
                    1.1f, 0, ColorUtils.multAlpha(t.cB(), a * 0.35f * rise));
        }

        flashes.removeIf(f -> now - f.born() > 300);
        for (Flash f : flashes) {
            float k = (now - f.born()) / 300f;
            bb.texturedQuad(buf(ParticleTexture.ORB.texture), f.pos().x, f.pos().y, f.pos().z, 0.6f + k * 1.6f, 0,
                    ColorUtils.multAlpha(ColorUtils.lerp(0xFFFFFFFF, f.color(), k), 1f - k));
        }

        for (Map.Entry<Identifier, Render3D.TexBuf> e : bufs.entrySet()) {
            if (!e.getValue().isEmpty()) Render3D.drawTextured(e.getKey(), e.getValue());
        }
        burst.render(ms, camera, BURST, true);
        dust.render(ms, camera, DUST, false);
        confetti.render(ms, camera, PAPER, false);
        shards.render(ms, camera, SHARDS, true);
    }

    private void glitch(Render3D.Billboard bb, Glitch g, long now) {
        long age = now - g.born();
        Random r = new Random(g.seed() ^ (age / 45) * 0x9E3779B97F4A7C15L);
        if (r.nextFloat() < 0.15f) return;
        float fade = 1f - age / 900f;
        float hx = bb.right.x, hz = bb.right.z;
        float len = (float) Math.sqrt(hx * hx + hz * hz);
        if (len < 1e-4f) { hx = 1; hz = 0; } else { hx /= len; hz /= len; }
        Render3D.TexBuf w = buf(Render3D.FX_WHITE);
        float width = g.width() * 1.3f, height = g.height() * 1.05f;
        float d = 0.06f;
        for (int j = 0; j < 7; j++) {
            double y = g.pos().y + height * r.nextFloat();
            float halfH = height * (0.02f + 0.06f * r.nextFloat());
            float halfW = width * 0.5f * (0.5f + 0.6f * r.nextFloat());
            float off = (r.nextFloat() - 0.5f) * width * 0.6f;
            double x = g.pos().x + hx * off, z = g.pos().z + hz * off;
            bb.uprightQuad(w, x - hx * d, y, z - hz * d, halfW, halfH, ColorUtils.multAlpha(0xFF00FFFF, fade * 0.55f));
            bb.uprightQuad(w, x + hx * d, y, z + hz * d, halfW, halfH, ColorUtils.multAlpha(0xFFFF00FF, fade * 0.55f));
            bb.uprightQuad(w, x, y, z, halfW, halfH * 0.6f, ColorUtils.multAlpha(0xFFFFFFFF, fade * 0.25f));
        }
        for (int j = 0; j < 10; j++) {
            float off = (r.nextFloat() - 0.5f) * width * 1.6f;
            double y = g.pos().y + height * r.nextFloat() * 1.1f;
            float s = 0.025f + 0.04f * r.nextFloat();
            int c = r.nextBoolean() ? 0xFF00FFFF : 0xFFFF00FF;
            bb.uprightQuad(w, g.pos().x + hx * off, y, g.pos().z + hz * off, s, s, ColorUtils.multAlpha(c, fade * 0.8f));
        }
    }
}
