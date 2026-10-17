package ru.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.modules.Category;
import ru.client.utils.render.RenderUtils;
import ru.client.utils.render.Shapes;

public final class Icons {
    private Icons() {}

    public enum Glyph { SEARCH, GEAR, LAYOUT, CHEVRON, CLOSE, PLUS, TRASH, STAR, STAR_OUTLINE, DICE, FOLDER, USER, SWAP, CHECK, SAVE, DOWNLOAD }

    static void draw(GuiGraphicsExtractor ctx, Category c, float cx, float cy, int color, int bg) {
        switch (c) {
            case COMBAT -> RenderUtils.drawPolygon(ctx, cx, cy, 5f, Shapes.STAR, color);
            case RENDER -> {
                RenderUtils.drawRing(ctx, cx, cy, 3.9f, 1.3f, color);
                RenderUtils.drawCircle(ctx, cx, cy, 1.6f, color);
            }
            case HUD -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 5, cy - 4, 10, 8, 2, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 3, cy - 1.5f, 3, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 3, cy + 0.8f, 5, 1.2f, color);
            }
            case UTILITY -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    RenderUtils.drawCircle(ctx, cx + (float) Math.cos(a) * 4f, cy + (float) Math.sin(a) * 4f, 1.4f, color);
                }
                RenderUtils.drawRing(ctx, cx, cy, 2.6f, 2f, color);
            }
            case CONFIGS -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 4.5f, cy - 5, 9, 10, 1.5f, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 2.5f, cy - 2.5f, 5, 1.1f, color);
                RenderUtils.drawRect(ctx, cx - 2.5f, cy - 0.3f, 5, 1.1f, color);
                RenderUtils.drawRect(ctx, cx - 2.5f, cy + 1.9f, 3, 1.1f, color);
            }
            case COLORS -> {
                RenderUtils.drawRing(ctx, cx, cy, 4.4f, 1.3f, color);
                RenderUtils.drawCircle(ctx, cx - 1.9f, cy - 1.3f, 1.05f, color);
                RenderUtils.drawCircle(ctx, cx + 0.6f, cy - 2.3f, 1.05f, color);
                RenderUtils.drawCircle(ctx, cx + 2.2f, cy - 0.2f, 1.05f, color);
                RenderUtils.drawCircle(ctx, cx + 0.6f, cy + 2.1f, 1.3f, color);
            }
            case ACCOUNTS -> draw(ctx, Glyph.USER, cx, cy, color);
        }
    }

    public static void draw(GuiGraphicsExtractor ctx, Glyph g, float cx, float cy, int color) {
        draw(ctx, g, cx, cy, color, 0f);
    }

    public static void draw(GuiGraphicsExtractor ctx, Glyph g, float cx, float cy, int color, float turn) {
        switch (g) {
            case SEARCH -> {
                RenderUtils.drawRing(ctx, cx - 1f, cy - 1f, 3.4f, 1.3f, color);
                RenderUtils.drawLine(ctx, cx + 1.6f, cy + 1.6f, cx + 4.2f, cy + 4.2f, 1.6f, color);
            }
            case GEAR -> {
                double rot = turn * Math.PI / 4;
                for (int i = 0; i < 8; i++) {
                    double a = rot + Math.PI * 2 * i / 8;
                    float c = (float) Math.cos(a), s = (float) Math.sin(a);
                    RenderUtils.drawLine(ctx, cx + c * 3.2f, cy + s * 3.2f, cx + c * 5.1f, cy + s * 5.1f, 1.9f, color);
                }
                RenderUtils.drawRing(ctx, cx, cy, 3.1f, 1.7f, color);
            }
            case LAYOUT -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 5, cy - 4.5f, 10, 9, 2, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 3f, cy - 2.5f, 3f, 2.5f, color);
                RenderUtils.drawRect(ctx, cx + 0.8f, cy - 2.5f, 2.2f, 1.1f, color);
                RenderUtils.drawRect(ctx, cx - 3f, cy + 1.3f, 6f, 1.1f, color);
            }
            case CHEVRON -> {
                float d = 2.2f * (1f - 2f * turn);
                RenderUtils.drawLine(ctx, cx - 3f, cy - d / 2f, cx, cy + d / 2f, 1.3f, color);
                RenderUtils.drawLine(ctx, cx, cy + d / 2f, cx + 3f, cy - d / 2f, 1.3f, color);
            }
            case CLOSE -> {
                RenderUtils.drawLine(ctx, cx - 3.2f, cy - 3.2f, cx + 3.2f, cy + 3.2f, 1.4f, color);
                RenderUtils.drawLine(ctx, cx - 3.2f, cy + 3.2f, cx + 3.2f, cy - 3.2f, 1.4f, color);
            }
            case PLUS -> {
                RenderUtils.drawRect(ctx, cx - 3.5f, cy - 0.65f, 7f, 1.3f, color);
                RenderUtils.drawRect(ctx, cx - 0.65f, cy - 3.5f, 1.3f, 7f, color);
            }
            case TRASH -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 3f, cy - 2f, 6f, 7f, 1.2f, 1.1f, color);
                RenderUtils.drawRect(ctx, cx - 4.5f, cy - 3.6f, 9f, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 1.5f, cy - 5f, 3f, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 0.5f, cy - 0.5f, 1f, 3.5f, color);
            }
            case STAR -> RenderUtils.drawPolygon(ctx, cx, cy, 4.8f, Shapes.STAR, color);
            case STAR_OUTLINE -> {
                float[] p = Shapes.STAR;
                int n = p.length / 2;
                for (int i = 0; i < n; i++) {
                    int j = i * 2, k = ((i + 1) % n) * 2;
                    RenderUtils.drawLine(ctx, cx + p[j] * 4.8f, cy - p[j + 1] * 4.8f, cx + p[k] * 4.8f, cy - p[k + 1] * 4.8f, 1.1f, color);
                }
            }
            case DICE -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 4.5f, cy - 4.5f, 9f, 9f, 2.2f, 1.2f, color);
                RenderUtils.drawCircle(ctx, cx - 1.9f, cy - 1.9f, 0.95f, color);
                RenderUtils.drawCircle(ctx, cx, cy, 0.95f, color);
                RenderUtils.drawCircle(ctx, cx + 1.9f, cy + 1.9f, 0.95f, color);
            }
            case FOLDER -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 5f, cy - 2.8f, 10f, 7.3f, 1.5f, 1.2f, color);
                RenderUtils.drawRoundedRect(ctx, cx - 5f, cy - 4.4f, 4.2f, 2.6f, 1f, color);
            }
            case USER -> {
                RenderUtils.drawCircle(ctx, cx, cy - 2.2f, 2.3f, color);
                RenderUtils.drawArc(ctx, cx, cy + 5.2f, 3.6f, 1.4f, 180f, 180f, color);
            }
            case SWAP -> {
                RenderUtils.drawLine(ctx, cx - 4f, cy - 1.8f, cx + 3.5f, cy - 1.8f, 1.2f, color);
                RenderUtils.drawLine(ctx, cx + 1.6f, cy - 3.8f, cx + 3.8f, cy - 1.8f, 1.2f, color);
                RenderUtils.drawLine(ctx, cx + 4f, cy + 1.8f, cx - 3.5f, cy + 1.8f, 1.2f, color);
                RenderUtils.drawLine(ctx, cx - 1.6f, cy + 3.8f, cx - 3.8f, cy + 1.8f, 1.2f, color);
            }
            case CHECK -> {
                RenderUtils.drawLine(ctx, cx - 3.4f, cy + 0.2f, cx - 1f, cy + 2.6f, 1.5f, color);
                RenderUtils.drawLine(ctx, cx - 1f, cy + 2.6f, cx + 3.6f, cy - 2.4f, 1.5f, color);
            }
            case SAVE -> {
                RenderUtils.drawRoundedOutline(ctx, cx - 4.5f, cy - 4.5f, 9f, 9f, 1.5f, 1.2f, color);
                RenderUtils.drawRect(ctx, cx - 2.2f, cy - 4.5f, 4.4f, 3f, color);
                RenderUtils.drawRect(ctx, cx - 2.5f, cy + 1f, 5f, 1.1f, color);
            }
            case DOWNLOAD -> {
                RenderUtils.drawLine(ctx, cx, cy - 4.5f, cx, cy + 1.6f, 1.3f, color);
                RenderUtils.drawLine(ctx, cx - 2.8f, cy - 1f, cx, cy + 1.8f, 1.3f, color);
                RenderUtils.drawLine(ctx, cx + 2.8f, cy - 1f, cx, cy + 1.8f, 1.3f, color);
                RenderUtils.drawRect(ctx, cx - 4f, cy + 3.4f, 8f, 1.2f, color);
            }
        }
    }
}
