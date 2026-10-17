package ru.client.utils.render;

import net.minecraft.util.Mth;

public final class ColorUtils {
    private ColorUtils() {}

    public static int alpha(int c) { return c >>> 24; }
    public static int red(int c)   { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c)  { return c & 0xFF; }

    public static int rgba(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16)
             | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    public static int withAlpha(int c, int a) { return (c & 0x00FFFFFF) | (Mth.clamp(a, 0, 255) << 24); }

    public static int multAlpha(int c, float f) { return withAlpha(c, (int) (alpha(c) * Mth.clamp(f, 0f, 1f))); }

    public static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        return rgba(
            (int) (red(a)   + (red(b)   - red(a))   * t),
            (int) (green(a) + (green(b) - green(a)) * t),
            (int) (blue(a)  + (blue(b)  - blue(a))  * t),
            (int) (alpha(a) + (alpha(b) - alpha(a)) * t));
    }

    public static int bilinear(float u, float v, int tl, int tr, int br, int bl) {
        return lerp(lerp(tl, tr, u), lerp(bl, br, u), v);
    }

    public static int fade(int c1, int c2, int speedMs, int offset) {
        double t = ((System.currentTimeMillis() + offset) % speedMs) / (double) speedMs;
        return lerp(c1, c2, (float) (Math.sin(t * Math.PI * 2) * 0.5 + 0.5));
    }

    public static int rainbow(int speedMs, int offset, float sat, float bri, int alpha) {
        float hue = ((System.currentTimeMillis() + offset) % speedMs) / (float) speedMs;
        return withAlpha(java.awt.Color.HSBtoRGB(hue, sat, bri), alpha);
    }
}
