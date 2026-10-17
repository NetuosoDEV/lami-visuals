package ru.client.utils.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class RenderUtils {
    private RenderUtils() {}

    private static final int MAX_SEGMENTS = 24;
    private static final float[] OUTER = new float[4 * (MAX_SEGMENTS + 1) * 2];
    private static final float[] INNER = new float[OUTER.length];

    public static void drawRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int color) {
        drawGradientRect(ctx, x, y, w, h, color, color, color, color);
    }

    public static void drawGradientRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                                        int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0) return;
        GuiMesh m = mesh();
        m.quad(x, y, tl, x, y + h, bl, x + w, y + h, br, x + w, y, tr);
        m.submit(ctx);
    }

    public static void drawHorizontalGradient(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int left, int right) {
        drawGradientRect(ctx, x, y, w, h, left, right, right, left);
    }

    public static void drawVerticalGradient(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int top, int bottom) {
        drawGradientRect(ctx, x, y, w, h, top, top, bottom, bottom);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color) {
        drawRoundedGradient(ctx, x, y, w, h, radius, color, color, color, color);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                                       float rTL, float rTR, float rBR, float rBL, int color) {
        if (w <= 0 || h <= 0) return;
        int n = perimeter(OUTER, x, y, w, h, rTL, rTR, rBR, rBL);
        GuiMesh m = mesh();
        float cx = x + w / 2f, cy = y + h / 2f;
        for (int i = 0; i < n; i++) {
            int j = i * 2, k = ((i + 1) % n) * 2;
            m.tri(cx, cy, color, OUTER[j], OUTER[j + 1], color, OUTER[k], OUTER[k + 1], color);
        }
        m.submit(ctx);
    }

    public static int perimeter(float[] out, float x, float y, float w, float h, float rTL, float rTR, float rBR, float rBL) {
        float lim = Math.min(w, h) / 2f;
        int idx = 0;
        for (int corner = 0; corner < 4; corner++) {
            float r = Mth.clamp(corner == 0 ? rTL : corner == 1 ? rTR : corner == 2 ? rBR : rBL, 0f, lim);
            float cx = (corner == 0 || corner == 3) ? x + r : x + w - r;
            float cy = (corner < 2) ? y + r : y + h - r;
            if (r < 0.01f) {
                out[idx++] = cx;
                out[idx++] = cy;
                continue;
            }
            int seg = segments(r);
            float start = 180f + corner * 90f;
            for (int i = 0; i <= seg; i++) {
                double a = Math.toRadians(start + 90f * i / seg);
                out[idx++] = cx + (float) Math.cos(a) * r;
                out[idx++] = cy + (float) Math.sin(a) * r;
            }
        }
        return idx / 2;
    }

    public static final int PERIMETER_CAPACITY = 4 * (MAX_SEGMENTS + 1) * 2;

    public static void drawRoundedHorizontalGradient(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                                                     float radius, int left, int right) {
        drawRoundedGradient(ctx, x, y, w, h, radius, left, right, right, left);
    }

    public static void drawRoundedGradient(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius,
                                           int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0) return;
        float r = clampRadius(radius, w, h);
        int n = buildPerimeter(OUTER, x, y, w, h, r, segments(r));

        GuiMesh m = mesh();
        float cx = x + w / 2f, cy = y + h / 2f;
        int cc = ColorUtils.bilinear(0.5f, 0.5f, tl, tr, br, bl);
        for (int i = 0; i < n; i++) {
            int j = i * 2, k = ((i + 1) % n) * 2;
            float ax = OUTER[j], ay = OUTER[j + 1], bx = OUTER[k], by = OUTER[k + 1];
            m.tri(cx, cy, cc,
                    ax, ay, ColorUtils.bilinear((ax - x) / w, (ay - y) / h, tl, tr, br, bl),
                    bx, by, ColorUtils.bilinear((bx - x) / w, (by - y) / h, tl, tr, br, bl));
        }
        m.submit(ctx);
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                                          float radius, float thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        float r = clampRadius(radius, w, h);
        int seg = segments(r);
        float t = Math.min(thickness, Math.min(w, h) / 2f);
        int n = buildPerimeter(OUTER, x, y, w, h, r, seg);
        buildPerimeter(INNER, x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0f, r - t), seg);

        GuiMesh m = mesh();
        for (int i = 0; i < n; i++) {
            int j = i * 2, k = ((i + 1) % n) * 2;
            m.quad(OUTER[j], OUTER[j + 1], color, INNER[j], INNER[j + 1], color,
                    INNER[k], INNER[k + 1], color, OUTER[k], OUTER[k + 1], color);
        }
        m.submit(ctx);
    }

    public static void drawGlow(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                                float radius, float size, int color) {
        final int layers = 8;
        int baseAlpha = ColorUtils.alpha(color);
        for (int i = layers; i >= 1; i--) {
            float expand = size * i / layers;
            float k = 1f - (float) i / (layers + 1);
            int a = (int) (baseAlpha * k * k / layers * 2.5f);
            if (a <= 0) continue;
            drawRoundedRect(ctx, x - expand, y - expand, w + expand * 2, h + expand * 2,
                    radius + expand, ColorUtils.withAlpha(color, a));
        }
    }

    public static void drawCircle(GuiGraphicsExtractor ctx, float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        int seg = Mth.clamp((int) (radius * 2), 12, 64);
        GuiMesh m = mesh();
        float px = cx + radius, py = cy;
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            float nx = cx + (float) Math.cos(a) * radius, ny = cy + (float) Math.sin(a) * radius;
            m.tri(cx, cy, color, px, py, color, nx, ny, color);
            px = nx; py = ny;
        }
        m.submit(ctx);
    }

    public static void drawRing(GuiGraphicsExtractor ctx, float cx, float cy, float radius, float thickness, int color) {
        drawArc(ctx, cx, cy, radius, thickness, 0f, 360f, color);
    }

    public static void drawArc(GuiGraphicsExtractor ctx, float cx, float cy, float radius, float thickness,
                               float startDeg, float sweepDeg, int color) {
        if (radius <= 0 || thickness <= 0 || sweepDeg == 0) return;
        float r1 = Math.max(0, radius - thickness / 2f), r2 = radius + thickness / 2f;
        int seg = Mth.clamp((int) (Math.abs(sweepDeg) / 360f * radius * 4), 6, 72);
        GuiMesh m = mesh();
        m.stripStart();
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / seg);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            m.strip(cx + cos * r1, cy + sin * r1, color, cx + cos * r2, cy + sin * r2, color);
        }
        m.submit(ctx);
    }

    public static void drawPolygon(GuiGraphicsExtractor ctx, float cx, float cy, float size, float[] poly, int color) {
        int n = poly.length / 2;
        GuiMesh m = mesh();
        for (int i = 0; i < n; i++) {
            int j = i * 2, k = ((i + 1) % n) * 2;
            m.tri(cx, cy, color, cx + poly[j] * size, cy - poly[j + 1] * size, color,
                    cx + poly[k] * size, cy - poly[k + 1] * size, color);
        }
        m.submit(ctx);
    }

    public static void drawLine(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, float width, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len * width / 2f, ny = dx / len * width / 2f;
        GuiMesh m = mesh();
        m.quad(x1 + nx, y1 + ny, color, x1 - nx, y1 - ny, color, x2 - nx, y2 - ny, color, x2 + nx, y2 + ny, color);
        m.submit(ctx);
    }

    public static void drawSoftCircle(GuiGraphicsExtractor ctx, float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        int seg = 64;
        float mid = radius * 0.45f;
        int c0 = color, c1 = ColorUtils.multAlpha(color, 0.55f), c2 = ColorUtils.withAlpha(color, 0);
        GuiMesh m = mesh();
        float pc = 1f, ps = 0f;
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            m.tri(cx, cy, c0, cx + pc * mid, cy + ps * mid, c1, cx + cos * mid, cy + sin * mid, c1);
            m.quad(cx + pc * mid, cy + ps * mid, c1, cx + pc * radius, cy + ps * radius, c2,
                    cx + cos * radius, cy + sin * radius, c2, cx + cos * mid, cy + sin * mid, c1);
            pc = cos; ps = sin;
        }
        m.submit(ctx);
    }

    public static void drawHeart(GuiGraphicsExtractor ctx, float cx, float cy, float size, int color) {
        drawPolygon(ctx, cx, cy, size, Shapes.HEART, color);
    }

    private static final GuiMesh SHARED = new GuiMesh();

    public static GuiMesh mesh() { return SHARED; }

    private static float clampRadius(float r, float w, float h) {
        return Mth.clamp(r, 0f, Math.min(w, h) / 2f);
    }

    private static int segments(float r) {
        return Mth.clamp((int) (r * 1.5f), 4, MAX_SEGMENTS);
    }

    private static int buildPerimeter(float[] out, float x, float y, float w, float h, float r, int seg) {
        int idx = 0;
        for (int corner = 0; corner < 4; corner++) {
            float cx = (corner == 0 || corner == 3) ? x + r : x + w - r;
            float cy = (corner < 2) ? y + r : y + h - r;
            float start = 180f + corner * 90f;
            for (int i = 0; i <= seg; i++) {
                double a = Math.toRadians(start + 90f * i / seg);
                out[idx++] = cx + (float) Math.cos(a) * r;
                out[idx++] = cy + (float) Math.sin(a) * r;
            }
        }
        return idx / 2;
    }
}
