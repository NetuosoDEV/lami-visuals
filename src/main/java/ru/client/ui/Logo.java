package ru.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.GuiMesh;
import ru.client.utils.render.RenderUtils;

public final class Logo {
    private Logo() {}

    private static final float COS30 = (float) Math.cos(Math.toRadians(30));
    private static final int[][] CUBES = {{0, 0, 0}, {0, 0, 1}, {1, 0, 0}, {0, 0, 2}};
    private static final float GAP = 0.07f;

    public static float markWidth(float size) {
        return size / 4.5f * 3 * COS30;
    }

    public static void mark(GuiGraphicsExtractor ctx, float x, float y, float size, int accent) {
        float a = size / 4.5f;
        float ox = x + COS30 * a, oy = y + 3 * a;
        int alpha = ColorUtils.alpha(accent);
        int top = ColorUtils.withAlpha(ColorUtils.lerp(accent | 0xFF000000, 0xFFFFFFFF, 0.35f), alpha);
        int right = ColorUtils.withAlpha(ColorUtils.lerp(accent | 0xFF000000, 0xFF000000, 0.36f), alpha);
        GuiMesh m = RenderUtils.mesh();
        for (int[] c : CUBES) {
            float x0 = c[0] + GAP, x1 = c[0] + 1 - GAP, y0 = c[1] + GAP, y1 = c[1] + 1 - GAP, z0 = c[2] + GAP, z1 = c[2] + 1 - GAP;
            face(m, ox, oy, a, top, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            face(m, ox, oy, a, accent, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
            face(m, ox, oy, a, right, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        }
        m.submit(ctx);
    }

    private static void face(GuiMesh m, float ox, float oy, float a, int color, float... p) {
        float[] s = new float[8];
        for (int i = 0; i < 4; i++) {
            float x = p[i * 3], y = p[i * 3 + 1], z = p[i * 3 + 2];
            s[i * 2] = ox + (x - y) * COS30 * a;
            s[i * 2 + 1] = oy + (x + y) * 0.5f * a - z * a;
        }
        m.quad(s[0], s[1], color, s[2], s[3], color, s[4], s[5], color, s[6], s[7], color);
    }
}
