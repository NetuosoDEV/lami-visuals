package ru.client.ui.menu;

import ru.client.modules.Modules;
import ru.client.modules.impl.utility.MainMenu;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.GuiMesh;
import ru.client.utils.render.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class MenuBackground {

    private static final MenuBackground INSTANCE = new MenuBackground();
    public static MenuBackground get() { return INSTANCE; }

    private final Random random = new Random();
    private final List<float[]> parts = new ArrayList<>();
    private String partsMode = "";
    private int partsW, partsH, partsCount;
    private long lastFrame = System.nanoTime();
    private float time;

    private MenuBackground() {}

    private static MainMenu cfg() { return Modules.get(MainMenu.class); }

    public String mode() {
        MainMenu m = cfg();
        return m == null ? "Constellation" : m.background.get();
    }

    public void render(GuiGraphicsExtractor ctx, int w, int h, int mouseX, int mouseY, Runnable panorama) {
        long now = System.nanoTime();
        MainMenu cfg = cfg();
        float speed = cfg == null ? 1f : cfg.speed.getFloat();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f) * speed;
        lastFrame = now;
        time += dt;
        boolean parallax = cfg == null || cfg.parallax.isEnabled();
        float px = parallax ? (mouseX - w / 2f) / Math.max(1, w) : 0, py = parallax ? (mouseY - h / 2f) / Math.max(1, h) : 0;
        Theme t = Theme.get();
        int a1 = t.accentA(), a2 = t.accentB();
        String mode = mode();
        int count = cfg == null ? 70 : cfg.particleCount.getInt();
        if (!mode.equals(partsMode) || w != partsW || h != partsH || count != partsCount) spawn(mode, w, h, count);

        switch (mode) {
            case "Aurora" -> aurora(ctx, w, h, px, py, a1, a2);
            case "Waves" -> waves(ctx, w, h, px, py, a1, a2);
            case "Starfield" -> starfield(ctx, w, h, dt, px, py, a1, a2);
            case "Snow" -> snow(ctx, w, h, dt, px, py, a1, a2);
            case "Bubbles" -> bubbles(ctx, w, h, dt, px, py, a1, a2);
            case "Panorama" -> {
                panorama.run();
                RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0x50000000, 0xA0050310);
                RenderUtils.drawSoftCircle(ctx, w * 0.5f, h * 1.1f, h * 0.8f, ColorUtils.withAlpha(a1, 60));
            }
            default -> constellation(ctx, w, h, dt, px, py, a1, a2, cfg == null || cfg.lines.isEnabled());
        }
    }

    private void blobs(GuiGraphicsExtractor ctx, int w, int h, float px, float py, int a1, int a2, int alpha1, int alpha2) {
        RenderUtils.drawSoftCircle(ctx, w * (0.25f + 0.08f * Mth.sin(time * 0.21f)) - px * 30,
                h * (0.35f + 0.10f * Mth.cos(time * 0.17f)) - py * 30, h * 0.75f, ColorUtils.withAlpha(a1, alpha1));
        RenderUtils.drawSoftCircle(ctx, w * (0.78f + 0.07f * Mth.cos(time * 0.19f)) - px * 45,
                h * (0.65f + 0.09f * Mth.sin(time * 0.23f)) - py * 45, h * 0.7f, ColorUtils.withAlpha(a2, alpha2));
    }

    private void constellation(GuiGraphicsExtractor ctx, int w, int h, float dt, float px, float py, int a1, int a2, boolean lines) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF0A0912, 0xFF150C27);
        blobs(ctx, w, h, px, py, a1, a2, 70, 55);
        RenderUtils.drawSoftCircle(ctx, w * 0.5f - px * 15, h * 1.05f, h * 0.6f, ColorUtils.withAlpha(0xFF6A2BFF, 40));
        int n = parts.size();
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            float[] p = parts.get(i);
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            if (p[1] < -10) { p[1] = h + 10; p[0] = random.nextFloat() * w; }
            if (p[0] < -10) p[0] = w + 10; else if (p[0] > w + 10) p[0] = -10;
            xs[i] = p[0] - px * 40 * p[5];
            ys[i] = p[1] - py * 40 * p[5];
        }
        if (lines) {
            float maxD = 70, maxD2 = maxD * maxD;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    float dx = xs[i] - xs[j], dy = ys[i] - ys[j], d2 = dx * dx + dy * dy;
                    if (d2 > maxD2) continue;
                    float k = 1f - (float) Math.sqrt(d2) / maxD;
                    int c = ColorUtils.lerp(a1, a2, xs[i] / Math.max(1, w));
                    RenderUtils.drawLine(ctx, xs[i], ys[i], xs[j], ys[j], 0.6f, ColorUtils.withAlpha(c, (int) (90 * k * k)));
                }
            }
        }
        for (int i = 0; i < n; i++) {
            float[] p = parts.get(i);
            int c = ColorUtils.lerp(a1, a2, xs[i] / Math.max(1, w));
            RenderUtils.drawCircle(ctx, xs[i], ys[i], p[4] * 2.2f, ColorUtils.withAlpha(c, (int) (30 * p[5])));
            RenderUtils.drawCircle(ctx, xs[i], ys[i], p[4], ColorUtils.withAlpha(0xFFFFFFFF, (int) (120 + 120 * p[5])));
        }
    }

    private void aurora(GuiGraphicsExtractor ctx, int w, int h, float px, float py, int a1, int a2) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF04060E, 0xFF0B0718);
        blobs(ctx, w, h, px, py, a1, a2, 45, 40);
        int[] colors = {a1, a2, 0xFFB45CFF, 0xFF3CFFB4};
        for (int r = 0; r < 4; r++) {
            float base = h * (0.22f + r * 0.11f) - py * 20 * (r + 1);
            float amp = h * (0.06f + 0.02f * r);
            float thick = h * (0.16f - 0.02f * r);
            int c = colors[r];
            GuiMesh top = RenderUtils.mesh().stripStart();
            int steps = 48;
            float[] mid = new float[steps + 1];
            for (int i = 0; i <= steps; i++) {
                float x = -20 + (w + 40) * i / (float) steps - px * 25 * (r + 1);
                float k = i / (float) steps;
                float y = base + amp * Mth.sin(k * 6.5f + time * (0.35f + 0.1f * r) + r * 1.7f)
                        + amp * 0.5f * Mth.sin(k * 13f - time * 0.5f + r);
                mid[i] = y;
                int a = (int) (70 + 50 * Mth.sin(k * 9f + time * 0.8f + r));
                top.strip(x, y - thick, ColorUtils.withAlpha(c, 0), x, y, ColorUtils.withAlpha(c, Math.max(0, a)));
            }
            GuiMesh bot = top.stripStart();
            for (int i = 0; i <= steps; i++) {
                float x = -20 + (w + 40) * i / (float) steps - px * 25 * (r + 1);
                float k = i / (float) steps;
                int a = (int) (70 + 50 * Mth.sin(k * 9f + time * 0.8f + r));
                bot.strip(x, mid[i], ColorUtils.withAlpha(c, Math.max(0, a)), x, mid[i] + thick * 0.35f, ColorUtils.withAlpha(c, 0));
            }
            bot.submit(ctx);
        }
        for (float[] p : parts) {
            float tw = 0.5f + 0.5f * Mth.sin(time * 2f + p[6]);
            RenderUtils.drawCircle(ctx, p[0] - px * 10, p[1] - py * 10, p[4] * 0.6f, ColorUtils.withAlpha(0xFFFFFFFF, (int) (40 + 140 * tw * p[5])));
        }
    }

    private void waves(GuiGraphicsExtractor ctx, int w, int h, float px, float py, int a1, int a2) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF0B0816, 0xFF120A22);
        RenderUtils.drawSoftCircle(ctx, w * 0.5f - px * 20, h * 0.3f - py * 20, h * 0.7f, ColorUtils.withAlpha(a1, 50));
        int layers = 5;
        for (int l = 0; l < layers; l++) {
            float k = l / (float) (layers - 1);
            float base = h * (0.48f + 0.1f * l) - py * 15 * (l + 1);
            float amp = h * (0.05f - 0.006f * l);
            int c = ColorUtils.lerp(a1, a2, k);
            int top = ColorUtils.withAlpha(c, (int) (110 + 60 * k));
            int bottom = ColorUtils.withAlpha(ColorUtils.lerp(c, 0xFF000000, 0.6f), 230);
            GuiMesh b = RenderUtils.mesh().stripStart();
            int steps = 60;
            for (int i = 0; i <= steps; i++) {
                float x = -10 + (w + 20) * i / (float) steps;
                float u = i / (float) steps;
                float y = base + amp * Mth.sin(u * (5f + l) + time * (0.6f + 0.25f * l) + l)
                        + amp * 0.4f * Mth.sin(u * 17f - time * (0.9f + 0.2f * l)) - px * 12 * (l + 1) * (u - 0.5f);
                b.strip(x, y, top, x, h + 2, bottom);
            }
            b.submit(ctx);
        }
    }

    private void starfield(GuiGraphicsExtractor ctx, int w, int h, float dt, float px, float py, int a1, int a2) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF030308, 0xFF090614);
        RenderUtils.drawSoftCircle(ctx, w / 2f - px * 60, h / 2f - py * 60, h * 0.55f, ColorUtils.withAlpha(a1, 40));
        float cx = w / 2f - px * 80, cy = h / 2f - py * 80, fov = Math.max(w, h) * 0.5f;
        for (float[] p : parts) {
            float pz = p[2];
            float nz = pz - dt * 0.55f;
            if (nz <= 0.02f) { reStar(p); continue; }
            float sx0 = cx + p[0] / pz * fov, sy0 = cy + p[1] / pz * fov;
            float sx1 = cx + p[0] / nz * fov, sy1 = cy + p[1] / nz * fov;
            p[2] = nz;
            if (sx1 < -20 || sx1 > w + 20 || sy1 < -20 || sy1 > h + 20) { reStar(p); continue; }
            float k = Mth.clamp(1f - nz, 0f, 1f);
            int c = ColorUtils.lerp(0xFFFFFFFF, ColorUtils.lerp(a1, a2, p[6]), 0.45f);
            RenderUtils.drawLine(ctx, sx0, sy0, sx1, sy1, 0.6f + k * 1.6f, ColorUtils.withAlpha(c, (int) (60 + 195 * k)));
        }
    }

    private void reStar(float[] p) {
        p[0] = (random.nextFloat() - 0.5f) * 2f;
        p[1] = (random.nextFloat() - 0.5f) * 2f;
        p[2] = 0.4f + random.nextFloat() * 0.6f;
        p[6] = random.nextFloat();
    }

    private void snow(GuiGraphicsExtractor ctx, int w, int h, float dt, float px, float py, int a1, int a2) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF070B16, 0xFF141A2E);
        blobs(ctx, w, h, px, py, a1, a2, 40, 30);
        for (float[] p : parts) {
            p[1] += p[3] * dt;
            p[0] += Mth.sin(time * 0.8f + p[6]) * 8f * dt * p[5];
            if (p[1] > h + 6) { p[1] = -6; p[0] = random.nextFloat() * w; }
            float x = p[0] - px * 40 * p[5], y = p[1] - py * 20 * p[5];
            RenderUtils.drawCircle(ctx, x, y, p[4] * 2.4f, ColorUtils.withAlpha(0xFFFFFFFF, (int) (18 * p[5])));
            RenderUtils.drawCircle(ctx, x, y, p[4], ColorUtils.withAlpha(0xFFFFFFFF, (int) (110 + 140 * p[5])));
        }
        RenderUtils.drawVerticalGradient(ctx, 0, h * 0.75f, w, h * 0.25f, 0x00FFFFFF, 0x22DDE6FF);
    }

    private void bubbles(GuiGraphicsExtractor ctx, int w, int h, float dt, float px, float py, int a1, int a2) {
        RenderUtils.drawVerticalGradient(ctx, 0, 0, w, h, 0xFF060A14, 0xFF0E0820);
        blobs(ctx, w, h, px, py, a1, a2, 55, 45);
        for (float[] p : parts) {
            p[1] += p[3] * dt * 1.4f;
            if (p[1] < -p[4] * 6) { p[1] = h + p[4] * 6; p[0] = random.nextFloat() * w; }
            float x = p[0] + Mth.sin(time * 1.1f + p[6]) * 6f - px * 40 * p[5], y = p[1] - py * 30 * p[5];
            float r = p[4] * 4f;
            int c = ColorUtils.lerp(a1, a2, p[6] / 6.28f);
            RenderUtils.drawCircle(ctx, x, y, r, ColorUtils.withAlpha(c, (int) (25 + 25 * p[5])));
            RenderUtils.drawRoundedOutline(ctx, x - r, y - r, r * 2, r * 2, r, 0.8f, ColorUtils.withAlpha(c, (int) (90 + 100 * p[5])));
            RenderUtils.drawCircle(ctx, x - r * 0.35f, y - r * 0.35f, r * 0.22f, ColorUtils.withAlpha(0xFFFFFFFF, (int) (80 + 80 * p[5])));
        }
    }

    private void spawn(String mode, int w, int h, int count) {
        partsMode = mode;
        partsW = w;
        partsH = h;
        partsCount = count;
        parts.clear();
        int n = switch (mode) {
            case "Starfield" -> count * 3;
            case "Snow" -> count * 2;
            case "Bubbles" -> Math.max(10, count / 3);
            case "Aurora" -> 60;
            case "Waves", "Panorama" -> 0;
            default -> count;
        };
        for (int i = 0; i < n; i++) {
            float[] p = new float[7];
            p[0] = random.nextFloat() * Math.max(1, w);
            p[1] = random.nextFloat() * Math.max(1, h) * (mode.equals("Aurora") ? 0.6f : 1f);
            p[2] = (random.nextFloat() - 0.5f) * 12f;
            p[3] = mode.equals("Snow") ? 10f + random.nextFloat() * 25f : -4f - random.nextFloat() * 10f;
            p[4] = 0.6f + random.nextFloat() * 1.4f;
            p[5] = 0.3f + random.nextFloat() * 0.7f;
            p[6] = random.nextFloat() * 6.28f;
            if (mode.equals("Starfield")) {
                reStar(p);
                p[2] = 0.05f + random.nextFloat() * 0.95f;
            }
            parts.add(p);
        }
    }
}
