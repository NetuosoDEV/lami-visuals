package ru.client.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import ru.client.core.ClientMod;
import ru.client.ui.theme.Theme;

public final class Fonts {
    private Fonts() {}

    public static final Identifier REGULAR = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "regular");
    public static final Identifier BOLD = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "bold");
    public static final Identifier LOGO = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "logo");

    public static final float LOGO_SIZE = 32f;
    private static final float VANILLA_LOGO_SIZE = 10f;

    public static float logoSize() { return custom() ? LOGO_SIZE : VANILLA_LOGO_SIZE; }

    private static Font tr() { return Minecraft.getInstance().font; }

    private static boolean forceVanilla;

    public static void setForceVanilla(boolean value) { forceVanilla = value; }

    private static boolean custom() {
        if (forceVanilla) return false;
        Theme t = Theme.get();
        return t == null || t.customFont();
    }

    private static final java.util.Map<Identifier, FontDescription> DESCS = new java.util.HashMap<>();

    private static FontDescription desc(Identifier id) {
        return DESCS.computeIfAbsent(id, FontDescription.Resource::new);
    }

    public static MutableComponent text(String s, Identifier font) {
        MutableComponent t = Component.literal(s);
        if (custom()) t.setStyle(Style.EMPTY.withFont(desc(font)));
        return t;
    }

    public static int width(String s) { return width(s, REGULAR); }
    public static int width(String s, Identifier font) { return tr().width(text(s, font)); }

    public static int height() { return 9; }

    public static String trim(String s, int maxWidth) { return trim(s, maxWidth, REGULAR); }

    public static String trim(String s, int maxWidth, Identifier font) {
        if (width(s, font) <= maxWidth) return s;
        int ell = width("…", font);
        int end = s.length();
        while (end > 0 && width(s.substring(0, end), font) + ell > maxWidth) end--;
        return s.substring(0, end) + "…";
    }

    public static void draw(GuiGraphicsExtractor ctx, String s, float x, float y, int color, boolean shadow) {
        draw(ctx, s, x, y, color, shadow, REGULAR);
    }

    public static void draw(GuiGraphicsExtractor ctx, String s, float x, float y, int color) {
        draw(ctx, s, x, y, color, false, REGULAR);
    }

    public static void drawBold(GuiGraphicsExtractor ctx, String s, float x, float y, int color) {
        draw(ctx, s, x, y, color, false, BOLD);
    }

    public static void draw(GuiGraphicsExtractor ctx, String s, float x, float y, int color, boolean shadow, Identifier font) {
        if ((color & 0xFC000000) == 0) return;
        ctx.text(tr(), text(s, font), Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawCentered(GuiGraphicsExtractor ctx, String s, float cx, float y, int color, Identifier font) {
        draw(ctx, s, cx - width(s, font) / 2f, y, color, false, font);
    }

    public static void drawGradient(GuiGraphicsExtractor ctx, String s, float x, float y, int c1, int c2, Identifier font) {
        float total = Math.max(1, width(s, font));
        for (int i = 0; i < s.length(); i++) {
            float cx = x + (i == 0 ? 0 : tr().getSplitter().stringWidth(text(s.substring(0, i), font)));
            float k = (cx - x) / total;
            draw(ctx, String.valueOf(s.charAt(i)), cx, y, ColorUtils.lerp(c1, c2, k), false, font);
        }
    }

    public static void drawLogo(GuiGraphicsExtractor ctx, String s, float x, float y, float size, int c1, int c2) {
        float scale = size / logoSize();
        var ms = ctx.pose();
        ms.pushMatrix();
        ms.translate(x, y);
        ms.scale(scale, scale);
        drawGradient(ctx, s, 0, 0, c1, c2, LOGO);
        ms.popMatrix();
    }

    public static float logoWidth(String s, float size) { return width(s, LOGO) * size / logoSize(); }

    public static float logoCapCenter(float size) {
        float capHalf = custom() ? 0.727f * LOGO_SIZE / 2f : 3.5f;
        return (7f - capHalf) * size / logoSize();
    }

    public static void drawScaled(GuiGraphicsExtractor ctx, String s, float x, float y, float scale, int color, Identifier font) {
        var ms = ctx.pose();
        ms.pushMatrix();
        ms.translate(x, y);
        ms.scale(scale, scale);
        draw(ctx, s, 0, 0, color, false, font);
        ms.popMatrix();
    }

    public static float width(String s, float scale, Identifier font) { return width(s, font) * scale; }
}
