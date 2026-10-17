package ru.client.utils.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ru.client.core.ClientMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;

public final class Render3D {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final PoseStack IDENTITY = new PoseStack();
    private static SubmitNodeCollector collector;
    private static Camera camera;

    private Render3D() {}

    public static void beginFrame(SubmitNodeCollector c, Camera cam) {
        collector = c;
        camera = cam;
        LateRenderer.beginFrame();
    }

    public static void endFrame() { collector = null; }

    public static Vec3 camPos() { return (camera != null ? camera : mc.gameRenderer.mainCamera()).position(); }

    public static void begin() {}
    public static void additive() {}
    public static void normalBlend() {}
    public static void end() {}

    public enum Mode { QUADS, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN }

    public static final class Buf {
        private final Mode mode;
        private float[] p = new float[96];
        private int[] c = new int[32];
        private int n;
        private final Vector3f tmp = new Vector3f();

        Buf(Mode mode) { this.mode = mode; }

        public Buf addVertex(Matrix4f m, float x, float y, float z) {
            if (n == c.length) { p = Arrays.copyOf(p, p.length * 2); c = Arrays.copyOf(c, c.length * 2); }
            m.transformPosition(x, y, z, tmp);
            p[n * 3] = tmp.x; p[n * 3 + 1] = tmp.y; p[n * 3 + 2] = tmp.z;
            c[n] = 0xFFFFFFFF;
            n++;
            return this;
        }

        public Buf setColor(int color) {
            if (n > 0) c[n - 1] = color;
            return this;
        }

        int[] quads() {
            int[] out;
            int k = 0;
            switch (mode) {
                case QUADS -> { out = new int[n - n % 4]; for (int i = 0; i < out.length; i++) out[i] = i; return out; }
                case TRIANGLES -> {
                    int t = n / 3; out = new int[t * 4];
                    for (int i = 0; i < t; i++) { out[k++] = i * 3; out[k++] = i * 3 + 1; out[k++] = i * 3 + 2; out[k++] = i * 3 + 2; }
                    return out;
                }
                case TRIANGLE_STRIP -> {
                    int t = Math.max(0, (n - 2) / 2); out = new int[t * 4];
                    for (int i = 0; i < t; i++) { int a = i * 2; out[k++] = a; out[k++] = a + 1; out[k++] = a + 3; out[k++] = a + 2; }
                    return out;
                }
                default -> {
                    int t = Math.max(0, n - 2); out = new int[t * 4];
                    for (int i = 0; i < t; i++) { out[k++] = 0; out[k++] = i + 1; out[k++] = i + 2; out[k++] = i + 2; }
                    return out;
                }
            }
        }
    }

    public static Buf buffer(Mode mode) { return new Buf(mode); }

    public static void draw(Buf b) {
        if (collector == null || b.n < 3) return;
        final float[] pos = Arrays.copyOf(b.p, b.n * 3);
        final int[] col = Arrays.copyOf(b.c, b.n);
        final int[] idx = b.quads();
        if (idx.length == 0) return;
        collector.submitCustomGeometry(IDENTITY, RenderTypes.debugQuads(), (pose, vc) -> {
            for (int i : idx) vc.addVertex(pose, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]).setColor(col[i]);
        });
    }

    private static void drawLines(float[] seg, int[] col, int count, float width) {
        if (collector == null || count == 0) return;
        final float[] s = Arrays.copyOf(seg, count * 6);
        final int[] cc = Arrays.copyOf(col, count * 2);
        final int cnt = count;
        collector.submitCustomGeometry(IDENTITY, RenderTypes.linesTranslucent(), (pose, vc) -> {
            for (int i = 0; i < cnt; i++) {
                float x1 = s[i * 6], y1 = s[i * 6 + 1], z1 = s[i * 6 + 2], x2 = s[i * 6 + 3], y2 = s[i * 6 + 4], z2 = s[i * 6 + 5];
                float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-6f) continue;
                dx /= len; dy /= len; dz /= len;
                vc.addVertex(pose, x1, y1, z1).setColor(cc[i * 2]).setNormal(pose, dx, dy, dz).setLineWidth(width);
                vc.addVertex(pose, x2, y2, z2).setColor(cc[i * 2 + 1]).setNormal(pose, dx, dy, dz).setLineWidth(width);
            }
        });
    }

    public static final RenderPipeline ADDITIVE_TEXTURED = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "pipeline/additive_textured"))
                    .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false)
                    .build());

    public static void init() {}

    public static final class TexBuf {
        private float[] p = new float[4 * 3 * 64];
        private float[] uv = new float[4 * 2 * 64];
        private int[] c = new int[4 * 64];
        private int n;

        public void clear() { n = 0; }

        public boolean isEmpty() { return n == 0; }

        int count() { return n; }

        void copyTo(float[] dp, float[] duv, int[] dc, int offset) {
            System.arraycopy(p, 0, dp, offset * 3, n * 3);
            System.arraycopy(uv, 0, duv, offset * 2, n * 2);
            System.arraycopy(c, 0, dc, offset, n);
        }

        void vertex(float x, float y, float z, float u, float v, int color) {
            if (n == c.length) {
                p = Arrays.copyOf(p, p.length * 2);
                uv = Arrays.copyOf(uv, uv.length * 2);
                c = Arrays.copyOf(c, c.length * 2);
            }
            p[n * 3] = x; p[n * 3 + 1] = y; p[n * 3 + 2] = z;
            uv[n * 2] = u; uv[n * 2 + 1] = v;
            c[n] = color;
            n++;
        }
    }

    public static void drawTextured(Identifier texture, TexBuf b) {
        if (collector == null) return;
        LateRenderer.add(texture, b);
    }

    public static final Identifier FX_WHITE = fx("white");
    public static final Identifier FX_BEAM = fx("beam");
    public static final Identifier FX_CONFETTI = fx("confetti");
    public static final Identifier FX_TOMBSTONE = fx("tombstone");

    private static Identifier fx(String name) {
        return Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "textures/fx/" + name + ".png");
    }

    public static int premul(int argb) {
        float a = ColorUtils.alpha(argb) / 255f;
        return 0xFF000000 | ((int) (ColorUtils.red(argb) * a) << 16) | ((int) (ColorUtils.green(argb) * a) << 8) | (int) (ColorUtils.blue(argb) * a);
    }

    @FunctionalInterface
    public interface FloatFn {
        float get(float t);

        static FloatFn of(float v) { return t -> v; }
    }

    public static void beam(PoseStack ms, TexBuf b, List<Vec3> pts, boolean closed, FloatFn widthAt, FloatColor colorAt) {
        int n = pts.size();
        if (n < 2) return;
        Vec3 c = camPos();
        float[] px = new float[n], py = new float[n], pz = new float[n];
        for (int i = 0; i < n; i++) {
            Vec3 p = pts.get(i);
            px[i] = (float) (p.x - c.x); py[i] = (float) (p.y - c.y); pz[i] = (float) (p.z - c.z);
        }
        float[] sx = new float[n], sy = new float[n], sz = new float[n];
        float lastX = 0, lastY = 1, lastZ = 0;
        for (int i = 0; i < n; i++) {
            int prev, next;
            if (closed) {
                int count = n - 1, r = i % count;
                prev = (r - 1 + count) % count;
                next = (r + 1) % count;
            } else {
                prev = Math.max(0, i - 1);
                next = Math.min(n - 1, i + 1);
            }
            float tx = px[next] - px[prev], ty = py[next] - py[prev], tz = pz[next] - pz[prev];
            float vx = -px[i], vy = -py[i], vz = -pz[i];
            float cx = ty * vz - tz * vy, cy = tz * vx - tx * vz, cz = tx * vy - ty * vx;
            float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (len < 1e-6f) { cx = lastX; cy = lastY; cz = lastZ; }
            else { cx /= len; cy /= len; cz /= len; lastX = cx; lastY = cy; lastZ = cz; }
            float w = widthAt.get(i / (float) (n - 1)) * 0.5f;
            sx[i] = cx * w; sy[i] = cy * w; sz[i] = cz * w;
        }
        Matrix4f m = ms.last().pose();
        Vector3f v = new Vector3f();
        for (int i = 0; i < n - 1; i++) {
            int j = i + 1;
            float ti = i / (float) (n - 1), tj = j / (float) (n - 1);
            int ci = premul(colorAt.get(ti)), cj = premul(colorAt.get(tj));
            if (ci == 0xFF000000 && cj == 0xFF000000) continue;
            m.transformPosition(px[i] - sx[i], py[i] - sy[i], pz[i] - sz[i], v); b.vertex(v.x, v.y, v.z, ti, 1, ci);
            m.transformPosition(px[i] + sx[i], py[i] + sy[i], pz[i] + sz[i], v); b.vertex(v.x, v.y, v.z, ti, 0, ci);
            m.transformPosition(px[j] + sx[j], py[j] + sy[j], pz[j] + sz[j], v); b.vertex(v.x, v.y, v.z, tj, 0, cj);
            m.transformPosition(px[j] - sx[j], py[j] - sy[j], pz[j] - sz[j], v); b.vertex(v.x, v.y, v.z, tj, 1, cj);
        }
    }

    public static void band(PoseStack ms, TexBuf b, Vec3[] edgeA, Vec3[] edgeB, int[] colorA, int[] colorB, float vA, float vB) {
        int n = Math.min(edgeA.length, edgeB.length);
        if (n < 2) return;
        Vec3 c = camPos();
        Matrix4f m = ms.last().pose();
        Vector3f v = new Vector3f();
        for (int i = 0; i < n - 1; i++) {
            int j = i + 1;
            int ai = premul(colorA[i]), aj = premul(colorA[j]), bi = premul(colorB[i]), bj = premul(colorB[j]);
            if (ai == 0xFF000000 && aj == 0xFF000000 && bi == 0xFF000000 && bj == 0xFF000000) continue;
            float ui = i / (float) (n - 1), uj = j / (float) (n - 1);
            m.transformPosition((float) (edgeA[i].x - c.x), (float) (edgeA[i].y - c.y), (float) (edgeA[i].z - c.z), v); b.vertex(v.x, v.y, v.z, ui, vA, ai);
            m.transformPosition((float) (edgeB[i].x - c.x), (float) (edgeB[i].y - c.y), (float) (edgeB[i].z - c.z), v); b.vertex(v.x, v.y, v.z, ui, vB, bi);
            m.transformPosition((float) (edgeB[j].x - c.x), (float) (edgeB[j].y - c.y), (float) (edgeB[j].z - c.z), v); b.vertex(v.x, v.y, v.z, uj, vB, bj);
            m.transformPosition((float) (edgeA[j].x - c.x), (float) (edgeA[j].y - c.y), (float) (edgeA[j].z - c.z), v); b.vertex(v.x, v.y, v.z, uj, vA, aj);
        }
    }

    public static void quad(PoseStack ms, TexBuf buf, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        Vec3 cam = camPos();
        Matrix4f m = ms.last().pose();
        Vector3f v = new Vector3f();
        Vec3[] pts = {a, b, c, d};
        int[] cols = {premul(ca), premul(cb), premul(cc), premul(cd)};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            m.transformPosition((float) (pts[i].x - cam.x), (float) (pts[i].y - cam.y), (float) (pts[i].z - cam.z), v);
            buf.vertex(v.x, v.y, v.z, uv[i][0], uv[i][1], cols[i]);
        }
    }

    public static void flatTexturedQuad(PoseStack ms, TexBuf b, double wx, double wy, double wz, float half, float rotationRad, int argb) {
        Vec3 c = camPos();
        float cx = (float) (wx - c.x), cy = (float) (wy - c.y), cz = (float) (wz - c.z);
        int color = premul(argb);
        if (color == 0xFF000000) return;
        float cos = (float) Math.cos(rotationRad), sin = (float) Math.sin(rotationRad);
        Matrix4f m = ms.last().pose();
        Vector3f v = new Vector3f();
        for (int i = 0; i < 4; i++) {
            float dx = (i == 1 || i == 2) ? 1 : -1, dz = (i >= 2) ? 1 : -1;
            float lx = (dx * cos - dz * sin) * half, lz = (dx * sin + dz * cos) * half;
            m.transformPosition(cx + lx, cy, cz + lz, v);
            b.vertex(v.x, v.y, v.z, (dx + 1) * 0.5f, (dz + 1) * 0.5f, color);
        }
    }

    public static void filledBox(PoseStack ms, AABB box, int color) {
        Vec3 c = camPos();
        float x1 = (float) (box.minX - c.x), y1 = (float) (box.minY - c.y), z1 = (float) (box.minZ - c.z);
        float x2 = (float) (box.maxX - c.x), y2 = (float) (box.maxY - c.y), z2 = (float) (box.maxZ - c.z);
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.QUADS);
        b.addVertex(m, x1, y1, z1).setColor(color); b.addVertex(m, x2, y1, z1).setColor(color); b.addVertex(m, x2, y1, z2).setColor(color); b.addVertex(m, x1, y1, z2).setColor(color);
        b.addVertex(m, x1, y2, z1).setColor(color); b.addVertex(m, x1, y2, z2).setColor(color); b.addVertex(m, x2, y2, z2).setColor(color); b.addVertex(m, x2, y2, z1).setColor(color);
        b.addVertex(m, x1, y1, z1).setColor(color); b.addVertex(m, x1, y2, z1).setColor(color); b.addVertex(m, x2, y2, z1).setColor(color); b.addVertex(m, x2, y1, z1).setColor(color);
        b.addVertex(m, x1, y1, z2).setColor(color); b.addVertex(m, x2, y1, z2).setColor(color); b.addVertex(m, x2, y2, z2).setColor(color); b.addVertex(m, x1, y2, z2).setColor(color);
        b.addVertex(m, x1, y1, z1).setColor(color); b.addVertex(m, x1, y1, z2).setColor(color); b.addVertex(m, x1, y2, z2).setColor(color); b.addVertex(m, x1, y2, z1).setColor(color);
        b.addVertex(m, x2, y1, z1).setColor(color); b.addVertex(m, x2, y2, z1).setColor(color); b.addVertex(m, x2, y2, z2).setColor(color); b.addVertex(m, x2, y1, z2).setColor(color);
        draw(b);
    }

    public static void outlineBox(PoseStack ms, AABB box, int color, float lineWidth) {
        Vec3 c = camPos();
        Matrix4f m = ms.last().pose();
        double[] xs = {box.minX - c.x, box.maxX - c.x}, ys = {box.minY - c.y, box.maxY - c.y}, zs = {box.minZ - c.z, box.maxZ - c.z};
        float[] seg = new float[12 * 6];
        int[] col = new int[24];
        int k = 0;
        Vector3f a = new Vector3f(), d = new Vector3f();
        int[][] edges = {
                {0,0,0, 1,0,0}, {0,0,1, 1,0,1}, {0,1,0, 1,1,0}, {0,1,1, 1,1,1},
                {0,0,0, 0,1,0}, {1,0,0, 1,1,0}, {0,0,1, 0,1,1}, {1,0,1, 1,1,1},
                {0,0,0, 0,0,1}, {1,0,0, 1,0,1}, {0,1,0, 0,1,1}, {1,1,0, 1,1,1}};
        for (int[] e : edges) {
            m.transformPosition((float) xs[e[0]], (float) ys[e[1]], (float) zs[e[2]], a);
            m.transformPosition((float) xs[e[3]], (float) ys[e[4]], (float) zs[e[5]], d);
            seg[k * 6] = a.x; seg[k * 6 + 1] = a.y; seg[k * 6 + 2] = a.z;
            seg[k * 6 + 3] = d.x; seg[k * 6 + 4] = d.y; seg[k * 6 + 5] = d.z;
            col[k * 2] = color; col[k * 2 + 1] = color;
            k++;
        }
        drawLines(seg, col, k, lineWidth);
    }

    public static void polyline(PoseStack ms, List<Vec3> points, IntUnaryOperator colorAt, float lineWidth) {
        if (points.size() < 2) return;
        Vec3 c = camPos();
        Matrix4f m = ms.last().pose();
        int cnt = points.size() - 1;
        float[] seg = new float[cnt * 6];
        int[] col = new int[cnt * 2];
        Vector3f a = new Vector3f(), d = new Vector3f();
        for (int i = 0; i < cnt; i++) {
            Vec3 p = points.get(i), q = points.get(i + 1);
            m.transformPosition((float) (p.x - c.x), (float) (p.y - c.y), (float) (p.z - c.z), a);
            m.transformPosition((float) (q.x - c.x), (float) (q.y - c.y), (float) (q.z - c.z), d);
            seg[i * 6] = a.x; seg[i * 6 + 1] = a.y; seg[i * 6 + 2] = a.z;
            seg[i * 6 + 3] = d.x; seg[i * 6 + 4] = d.y; seg[i * 6 + 5] = d.z;
            col[i * 2] = colorAt.applyAsInt(i);
            col[i * 2 + 1] = colorAt.applyAsInt(i + 1);
        }
        drawLines(seg, col, cnt, lineWidth);
    }

    public static void ring(PoseStack ms, double cx, double cy, double cz, float radius, float thickness, FloatColor colorAt) {
        Vec3 c = camPos();
        float x = (float) (cx - c.x), y = (float) (cy - c.y), z = (float) (cz - c.z);
        float r1 = Math.max(0, radius - thickness / 2f), r2 = radius + thickness / 2f;
        int seg = 72;
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.TRIANGLE_STRIP);
        for (int i = 0; i <= seg; i++) {
            float t = (float) i / seg;
            double a = Math.PI * 2 * t;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            int col = colorAt.get(t);
            b.addVertex(m, x + cos * r1, y, z + sin * r1).setColor(col);
            b.addVertex(m, x + cos * r2, y, z + sin * r2).setColor(col);
        }
        draw(b);
    }

    public static void ringGradient(PoseStack ms, double cx, double cy, double cz, float r1, float r2, int innerColor, int outerColor) {
        Vec3 c = camPos();
        float x = (float) (cx - c.x), y = (float) (cy - c.y), z = (float) (cz - c.z);
        int seg = 72;
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.TRIANGLE_STRIP);
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            b.addVertex(m, x + cos * r1, y, z + sin * r1).setColor(innerColor);
            b.addVertex(m, x + cos * r2, y, z + sin * r2).setColor(outerColor);
        }
        draw(b);
    }

    public static void disc(PoseStack ms, double cx, double cy, double cz, float radius, int centerColor, int edgeColor) {
        Vec3 c = camPos();
        float x = (float) (cx - c.x), y = (float) (cy - c.y), z = (float) (cz - c.z);
        int seg = 64;
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.TRIANGLE_FAN);
        b.addVertex(m, x, y, z).setColor(centerColor);
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            b.addVertex(m, x + (float) Math.cos(a) * radius, y, z + (float) Math.sin(a) * radius).setColor(edgeColor);
        }
        draw(b);
    }

    public static void wall(PoseStack ms, double cx, double cy, double cz, float radius, float height,
                            FloatColor bottomColor, FloatColor topColor) {
        Vec3 c = camPos();
        float x = (float) (cx - c.x), y = (float) (cy - c.y), z = (float) (cz - c.z);
        int seg = 72;
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.TRIANGLE_STRIP);
        for (int i = 0; i <= seg; i++) {
            float t = (float) i / seg;
            double a = Math.PI * 2 * t;
            float px = x + (float) Math.cos(a) * radius, pz = z + (float) Math.sin(a) * radius;
            b.addVertex(m, px, y, pz).setColor(bottomColor.get(t));
            b.addVertex(m, px, y + height, pz).setColor(topColor.get(t));
        }
        draw(b);
    }

    public static void cone(PoseStack ms, double cx, double baseY, double cz, float radius, float height,
                            float rotationDeg, FloatColor baseColor, int apexColor) {
        Vec3 c = camPos();
        float x = (float) (cx - c.x), y = (float) (baseY - c.y), z = (float) (cz - c.z);
        int seg = 64;
        double rot = Math.toRadians(rotationDeg);
        Matrix4f m = ms.last().pose();
        Buf b = buffer(Mode.TRIANGLE_FAN);
        b.addVertex(m, x, y + height, z).setColor(apexColor);
        for (int i = 0; i <= seg; i++) {
            float t = (float) i / seg;
            double a = Math.PI * 2 * t + rot;
            b.addVertex(m, x + (float) Math.cos(a) * radius, y, z + (float) Math.sin(a) * radius).setColor(baseColor.get(t));
        }
        draw(b);
    }

    public static final class Billboard {
        public final Vector3f right, up;
        public final Matrix4f matrix;
        public final Vec3 cam;

        public Billboard(PoseStack ms, Camera camera) {
            Quaternionf q = camera.rotation();
            this.right = new Vector3f(1, 0, 0).rotate(q);
            this.up = new Vector3f(0, 1, 0).rotate(q);
            this.matrix = ms.last().pose();
            this.cam = camera.position();
        }

        public void texturedQuad(TexBuf b, double wx, double wy, double wz, float half, float rotationRad, int argb) {
            texturedQuad(b, wx, wy, wz, half, half, rotationRad, argb);
        }

        public void texturedQuad(TexBuf b, double wx, double wy, double wz, float halfW, float halfH, float rotationRad, int argb) {
            int color = premul(argb);
            if (color == 0xFF000000) return;
            float cx = (float) (wx - cam.x), cy = (float) (wy - cam.y), cz = (float) (wz - cam.z);
            float cos = (float) Math.cos(rotationRad), sin = (float) Math.sin(rotationRad);
            for (int i = 0; i < 4; i++) {
                float dx = (i == 1 || i == 2) ? 1 : -1, dy = (i >= 2) ? 1 : -1;
                float lx = dx * halfW * cos - dy * halfH * sin, ly = dx * halfW * sin + dy * halfH * cos;
                matrix.transformPosition(cx + right.x * lx + up.x * ly, cy + right.y * lx + up.y * ly, cz + right.z * lx + up.z * ly, tmp);
                b.vertex(tmp.x, tmp.y, tmp.z, (dx + 1) * 0.5f, (1 - dy) * 0.5f, color);
            }
        }

        public void uprightQuad(TexBuf b, double wx, double wy, double wz, float halfW, float halfH, int argb) {
            int color = premul(argb);
            if (color == 0xFF000000) return;
            float hx = right.x, hz = right.z;
            float len = (float) Math.sqrt(hx * hx + hz * hz);
            if (len < 1e-4f) { hx = 1; hz = 0; } else { hx /= len; hz /= len; }
            float cx = (float) (wx - cam.x), cy = (float) (wy - cam.y), cz = (float) (wz - cam.z);
            for (int i = 0; i < 4; i++) {
                float dx = (i == 1 || i == 2) ? 1 : -1, dy = (i >= 2) ? 1 : -1;
                matrix.transformPosition(cx + hx * dx * halfW, cy + dy * halfH, cz + hz * dx * halfW, tmp);
                b.vertex(tmp.x, tmp.y, tmp.z, (dx + 1) * 0.5f, (1 - dy) * 0.5f, color);
            }
        }

        private final Vector3f tmp = new Vector3f();

        public void shape(Buf b, double wx, double wy, double wz, float[] poly, float size,
                          float rotationRad, int centerColor, int edgeColor) {
            float cx = (float) (wx - cam.x), cy = (float) (wy - cam.y), cz = (float) (wz - cam.z);
            float cos = (float) Math.cos(rotationRad), sin = (float) Math.sin(rotationRad);
            int n = poly.length / 2;
            float prevX = 0, prevY = 0, prevZ = 0;
            for (int i = 0; i <= n; i++) {
                int j = (i % n) * 2;
                float lx = (poly[j] * cos - poly[j + 1] * sin) * size;
                float ly = (poly[j] * sin + poly[j + 1] * cos) * size;
                float px = cx + right.x * lx + up.x * ly;
                float py = cy + right.y * lx + up.y * ly;
                float pz = cz + right.z * lx + up.z * ly;
                if (i > 0) {
                    b.addVertex(matrix, cx, cy, cz).setColor(centerColor);
                    b.addVertex(matrix, prevX, prevY, prevZ).setColor(edgeColor);
                    b.addVertex(matrix, px, py, pz).setColor(edgeColor);
                }
                prevX = px; prevY = py; prevZ = pz;
            }
        }
    }

    public static void text(PoseStack ms, Camera camera, String text, double wx, double wy, double wz, float scale, int color) {
        if (collector == null || ColorUtils.alpha(color) < 5) return;
        Vec3 c = camera.position();
        ms.pushPose();
        ms.translate(wx - c.x, wy - c.y, wz - c.z);
        ms.rotate(camera.rotation());
        float s = 0.025f * scale;
        ms.scale(s, -s, s);
        Font tr = mc.font;
        collector.submitText(ms, -tr.width(text) / 2f, 0, Component.literal(text).getVisualOrderText(), true,
                Font.DisplayMode.NORMAL, 15728880, color, 0, 0);
        ms.popPose();
    }

    @FunctionalInterface
    public interface FloatColor {
        int get(float t);

        static FloatColor of(int color) { return t -> color; }
    }
}
