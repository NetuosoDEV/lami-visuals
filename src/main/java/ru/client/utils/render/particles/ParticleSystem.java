package ru.client.utils.render.particles;

import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ParticleSystem {

    public static final class Particle {
        public double x, y, z, vx, vy, vz;
        public float size, rotation, rotationSpeed;
        public float swayAmp, swayFreq, swayPhase;
        public int color, lifeMs;
        public int color2;
        public ParticleTexture texture = ParticleTexture.STAR;
        public Identifier textureId;
        public float extent = 1f / ParticleTexture.ICON_FRACTION;
        public float tailTime;
        public float blinkFreq;
        public int fadeInMs;
        public int delayMs;
        public float flipSpeed;
        public float param1, param2;
        final long born = System.currentTimeMillis();

        Identifier tex() { return textureId != null ? textureId : texture.texture; }
    }

    public record Physics(boolean enabled, float gravity, float drag, float bounce) {}

    @FunctionalInterface
    public interface Mover {
        void move(Particle p, float dt, float timeSec);
    }

    private static final Minecraft mc = Minecraft.getInstance();
    private final List<Particle> particles = new ArrayList<>();
    private final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    private final Map<Identifier, Render3D.TexBuf> buffers = new LinkedHashMap<>();
    private final List<Vec3> tail = new ArrayList<>(2);
    private final int max;
    private long lastNs = System.nanoTime();

    public ParticleSystem(int max) { this.max = max; }

    public void add(Particle p) {
        if (particles.size() >= max) particles.remove(0);
        particles.add(p);
    }

    public int size() { return particles.size(); }

    public void clear() { particles.clear(); }

    public void render(PoseStack ms, Camera camera, Physics phys, boolean glow) {
        render(ms, camera, phys, glow, null);
    }

    public void render(PoseStack ms, Camera camera, Physics phys, boolean glow, Mover mover) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNs) / 1_000_000_000f, 0.05f);
        lastNs = now;
        if (particles.isEmpty() || mc.level == null) return;

        long ms0 = System.currentTimeMillis();
        float drag = (float) Math.pow(phys.drag(), dt);
        float time = (ms0 % 1_000_000L) / 1000f;
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            long age = ms0 - p.born - p.delayMs;
            if (age > p.lifeMs) { it.remove(); continue; }
            if (age < 0) continue;
            if (mover != null) mover.move(p, dt, time);
            if (phys.enabled()) p.vy -= phys.gravity() * dt;
            p.vx *= drag; p.vy *= drag; p.vz *= drag;
            double sx = 0, sz = 0;
            if (p.swayAmp > 0) {
                float a = time * p.swayFreq + p.swayPhase;
                sx = Math.cos(a) * p.swayAmp;
                sz = Math.sin(a * 0.8f) * p.swayAmp;
            }
            double nx = p.x + (p.vx + sx) * dt, ny = p.y + p.vy * dt, nz = p.z + (p.vz + sz) * dt;
            if (phys.enabled()) {
                if (solid(p.x, ny, p.z)) {
                    p.vy = -p.vy * phys.bounce();
                    ny = p.y;
                    p.vx *= 0.8; p.vz *= 0.8;
                    if (Math.abs(p.vy) < 0.15) { p.vy = 0; p.swayAmp = 0; p.rotationSpeed *= 0.8f; p.flipSpeed *= 0.8f; }
                }
                if (solid(nx, ny, p.z)) { p.vx = -p.vx * 0.5; nx = p.x; }
                if (solid(nx, ny, nz)) { p.vz = -p.vz * 0.5; nz = p.z; }
            }
            p.x = nx; p.y = ny; p.z = nz;
            p.rotation += p.rotationSpeed * dt;
        }

        Render3D.Billboard bb = new Render3D.Billboard(ms, camera);
        for (Render3D.TexBuf b : buffers.values()) b.clear();
        for (Particle p : particles) {
            long age = ms0 - p.born - p.delayMs;
            if (age < 0) continue;
            float a = alpha(p, age, time), s = Math.min(1f, age / 150f);
            int col = p.color2 != 0 ? ColorUtils.lerp(p.color, p.color2, Math.min(1f, age / (float) p.lifeMs)) : p.color;
            float half = p.size * p.extent * s;
            if (glow) {
                bb.texturedQuad(buf(ParticleTexture.ORB.texture), p.x, p.y, p.z, half * 2.2f, 0, ColorUtils.multAlpha(col, a * 0.3f));
            }
            if (p.tailTime > 0) tail(ms, p, col, a);
            float halfW = p.flipSpeed != 0 ? half * Math.max(0.15f, Math.abs((float) Math.cos(time * p.flipSpeed + p.swayPhase))) : half;
            bb.texturedQuad(buf(p.tex()), p.x, p.y, p.z, halfW, half, p.rotation, ColorUtils.multAlpha(col, a));
        }
        for (Map.Entry<Identifier, Render3D.TexBuf> e : buffers.entrySet()) {
            if (!e.getValue().isEmpty()) Render3D.drawTextured(e.getKey(), e.getValue());
        }
    }

    private void tail(PoseStack ms, Particle p, int col, float a) {
        double len = p.tailTime;
        if (p.vx * p.vx + p.vy * p.vy + p.vz * p.vz < 0.04) return;
        tail.clear();
        tail.add(new Vec3(p.x, p.y, p.z));
        tail.add(new Vec3(p.x - p.vx * len, p.y - p.vy * len, p.z - p.vz * len));
        float w = p.size * 1.6f;
        Render3D.beam(ms, buf(Render3D.FX_BEAM), tail, false, t -> w * (1f - t * 0.85f),
                t -> ColorUtils.multAlpha(col, a * (1f - t) * 0.9f));
    }

    private Render3D.TexBuf buf(Identifier t) {
        return buffers.computeIfAbsent(t, k -> new Render3D.TexBuf());
    }

    private static float alpha(Particle p, long age, float time) {
        float t = age / (float) p.lifeMs;
        float a = t < 0.6f ? 1f : Math.max(0f, 1f - (t - 0.6f) / 0.4f);
        if (p.fadeInMs > 0) a *= Math.min(1f, age / (float) p.fadeInMs);
        if (p.blinkFreq > 0) a *= 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(time * p.blinkFreq * Math.PI * 2 + p.swayPhase));
        return a;
    }

    private boolean solid(double x, double y, double z) {
        mutable.set(x, y, z);
        BlockState state = mc.level.getBlockState(mutable);
        return !state.isAir() && !state.getCollisionShape(mc.level, mutable).isEmpty();
    }
}
