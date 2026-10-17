package ru.client.ui.clickgui;

import ru.client.core.I18n;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.entity.player.PlayerSkin;
import ru.client.ui.UiSounds;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class Ui {
    private Ui() {}

    private static final Minecraft mc = Minecraft.getInstance();

    public static final float RADIUS = 5;

    private static int opaque(int c) { return c | 0xFF000000; }

    public static int bg() { return opaque(Theme.get().panel()); }

    public static int side() { return ColorUtils.lerp(bg(), 0xFF000000, 0.28f); }

    public static int surface() { return ColorUtils.lerp(bg(), 0xFFFFFFFF, 0.028f); }

    public static int surfaceHover() { return ColorUtils.lerp(bg(), 0xFFFFFFFF, 0.055f); }

    public static int inset() { return ColorUtils.lerp(bg(), 0xFF000000, 0.35f); }

    public static int accent() { return Theme.get().accentSolid(); }

    public static final int LINE = 0x14FFFFFF;
    public static final int LINE_HI = 0x26FFFFFF;
    public static final int DANGER = 0xFFE5484D;
    public static final int SUCCESS = 0xFF46A758;

    public static int text() { return Theme.get().text(); }

    public static int dim() { return Theme.get().textDim(); }

    public static int faint() { return ColorUtils.multAlpha(Theme.get().textDim(), 0.6f); }

    private static final Map<String, Animation> ANIMS = new HashMap<>();

    public static float anim(String id, boolean on) { return anim(id, on, 16f); }

    public static float anim(String id, boolean on, float speed) {
        Animation a = ANIMS.computeIfAbsent(id, k -> new Animation(speed, on ? 1f : 0f));
        a.setTarget(on ? 1f : 0f);
        return a.update();
    }

    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static void click() { UiSounds.click(); }

    public static final class Hits {
        private record Hit(float x, float y, float w, float h, Runnable action, boolean silent) {}

        private final List<Hit> list = new ArrayList<>();

        public void clear() { list.clear(); }

        public void add(float x, float y, float w, float h, Runnable action) { list.add(new Hit(x, y, w, h, action, false)); }

        public void addSilent(float x, float y, float w, float h, Runnable action) { list.add(new Hit(x, y, w, h, action, true)); }

        public boolean click(double mx, double my) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Hit h = list.get(i);
                if (inside(mx, my, h.x, h.y, h.w, h.h)) {
                    if (!h.silent) Ui.click();
                    h.action.run();
                    return true;
                }
            }
            return false;
        }
    }

    public enum Kind { PRIMARY, GHOST, DANGER }

    public static float buttonWidth(String label, Icons.Glyph glyph) {
        boolean hasLabel = label != null && !label.isEmpty();
        float w = (hasLabel ? Fonts.width(label) : 0) + 18;
        if (glyph != null) w += hasLabel ? 14 : 2;
        return w;
    }

    public static void button(GuiGraphicsExtractor ctx, Hits hits, String id, String label, Icons.Glyph glyph,
                              float x, float y, float w, float h, Kind kind, int mx, int my, Runnable action) {
        boolean hover = inside(mx, my, x, y, w, h);
        float a = anim("btn." + id, hover);
        int fg;
        switch (kind) {
            case PRIMARY -> {
                RenderUtils.drawRoundedRect(ctx, x, y, w, h, RADIUS, ColorUtils.lerp(accent(), 0xFFFFFFFF, 0.1f * a));
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                RenderUtils.drawRoundedRect(ctx, x, y, w, h, RADIUS, ColorUtils.lerp(surface(), surfaceHover(), a));
                RenderUtils.drawRoundedOutline(ctx, x, y, w, h, RADIUS, 1, ColorUtils.lerp(LINE, ColorUtils.withAlpha(DANGER, 120), a));
                fg = ColorUtils.lerp(dim(), DANGER, a);
            }
            default -> {
                RenderUtils.drawRoundedRect(ctx, x, y, w, h, RADIUS, ColorUtils.lerp(surface(), surfaceHover(), a));
                RenderUtils.drawRoundedOutline(ctx, x, y, w, h, RADIUS, 1, ColorUtils.lerp(LINE, LINE_HI, a));
                fg = ColorUtils.lerp(ColorUtils.lerp(dim(), text(), 0.5f), text(), a);
            }
        }
        boolean hasLabel = label != null && !label.isEmpty();
        float cw = (hasLabel ? Fonts.width(label) : 0) + (glyph != null ? (hasLabel ? 14 : 10) : 0);
        float cx = x + w / 2f - cw / 2f, cy = y + h / 2f;
        if (glyph != null) {
            Icons.draw(ctx, glyph, cx + 5, cy, fg);
            cx += 14;
        }
        if (hasLabel) Fonts.draw(ctx, label, cx, cy - 4, fg);
        if (hits != null && action != null) hits.add(x, y, w, h, action);
    }

    public static void iconButton(GuiGraphicsExtractor ctx, Hits hits, String id, Icons.Glyph glyph, float x, float y, float size,
                                  boolean active, int activeColor, int mx, int my, Runnable action) {
        boolean hover = inside(mx, my, x, y, size, size);
        float a = anim("ib." + id, hover);
        RenderUtils.drawRoundedRect(ctx, x, y, size, size, RADIUS, ColorUtils.lerp(surface(), surfaceHover(), a));
        RenderUtils.drawRoundedOutline(ctx, x, y, size, size, RADIUS, 1, ColorUtils.lerp(LINE, LINE_HI, a));
        int fg = active ? activeColor : ColorUtils.lerp(dim(), text(), a);
        Icons.draw(ctx, glyph, x + size / 2f, y + size / 2f, fg);
        if (hits != null && action != null) hits.add(x, y, size, size, action);
    }

    public static void toggle(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float on) {
        float r = h / 2f;
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, r, ColorUtils.lerp(0xFF2E2E35, accent(), on));
        float kr = r - 2f;
        RenderUtils.drawCircle(ctx, x + r + (w - 2 * r) * on, y + r, kr, ColorUtils.lerp(0xFFA1A1AA, 0xFFFFFFFF, on));
    }

    public static void label(GuiGraphicsExtractor ctx, String s, float x, float y, int color) {
        Fonts.drawScaled(ctx, s, x, y, 0.85f, color, Fonts.REGULAR);
    }

    public static float labelWidth(String s) { return Fonts.width(s) * 0.85f; }

    public static void tooltip(GuiGraphicsExtractor ctx, String text, float mx, float my, float screenW, float alpha) {
        if (alpha < 0.05f || text == null || text.isEmpty()) return;
        float tw = Fonts.width(text) + 12, th = 16;
        float tx = Math.max(4, Math.min(mx + 10, screenW - tw - 4)), ty = my + 14;
        RenderUtils.drawRoundedRect(ctx, tx, ty, tw, th, 4, ColorUtils.withAlpha(0xFF0C0C0E, (int) (245 * alpha)));
        RenderUtils.drawRoundedOutline(ctx, tx, ty, tw, th, 4, 1, ColorUtils.multAlpha(LINE_HI, alpha));
        Fonts.draw(ctx, text, tx + 6, ty + 4, ColorUtils.multAlpha(text(), alpha));
    }

    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yy");

    public static String ago(long time) {
        if (time <= 0) return "";
        long s = (System.currentTimeMillis() - time) / 1000;
        if (s < 60) return I18n.tr("только что");
        if (s < 3600) return I18n.tr("%d мин назад", s / 60);
        if (s < 86400) return I18n.tr("%d ч назад", s / 3600);
        if (s < 86400 * 7) return I18n.tr("%d дн назад", s / 86400);
        return DATE.format(new Date(time));
    }

    public static void letterAvatar(GuiGraphicsExtractor ctx, String name, float x, float y, float size) {
        float hue = (name.toLowerCase().hashCode() & 0xFFFF) / 65536f;
        RenderUtils.drawRoundedRect(ctx, x, y, size, size, 3, Color.HSBtoRGB(hue, 0.32f, 0.48f) | 0xFF000000);
        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        float scale = size / 16f;
        float lw = Fonts.width(letter, Fonts.BOLD) * scale;
        Fonts.drawScaled(ctx, letter, x + size / 2f - lw / 2f, y + size / 2f - 4f * scale, scale, 0xFFFFFFFF, Fonts.BOLD);
    }

    private static final Map<UUID, Supplier<PlayerSkin>> SKINS = new HashMap<>();

    public static void face(GuiGraphicsExtractor ctx, GameProfile profile, float x, float y, int size) {
        try {
            Supplier<PlayerSkin> skin = SKINS.computeIfAbsent(profile.id(), id -> mc.getSkinManager().createLookup(profile, false));
            PlayerFaceExtractor.extractRenderState(ctx, skin.get(), Math.round(x), Math.round(y), size);
        } catch (Exception e) {
            letterAvatar(ctx, profile.name(), x, y, size);
        }
    }

    public static final class TextField {
        public String text = "";
        public boolean focused;
        private final String placeholder;
        private final int max;
        private long caretAt, errorAt;
        private float x, y, w, h;
        private Runnable onEnter = () -> {};
        private String hint;

        public TextField(String placeholder, int max) {
            this.placeholder = placeholder;
            this.max = max;
        }

        public TextField onEnter(Runnable r) { this.onEnter = r; return this; }

        public TextField hint(String hint) { this.hint = hint; return this; }

        public void focus() { focused = true; caretAt = System.currentTimeMillis(); }

        public void error() { errorAt = System.currentTimeMillis(); focus(); }

        public void render(GuiGraphicsExtractor ctx, float x, float y, float w, float h, Icons.Glyph icon, int mx, int my) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            long since = System.currentTimeMillis() - errorAt;
            float err = since < 700 ? 1f - since / 700f : 0f;
            boolean hover = inside(mx, my, x, y, w, h);
            int outline = focused ? ColorUtils.withAlpha(accent(), 200) : hover ? LINE_HI : LINE;
            if (err > 0) outline = ColorUtils.lerp(outline, DANGER, err);
            RenderUtils.drawRoundedRect(ctx, x, y, w, h, RADIUS, inset());
            RenderUtils.drawRoundedOutline(ctx, x, y, w, h, RADIUS, 1, outline);
            float tx = x + 8;
            if (icon != null) {
                Icons.draw(ctx, icon, x + 11, y + h / 2f, focused ? dim() : faint());
                tx = x + 21;
            }
            boolean empty = text.isEmpty();
            float avail = x + w - 8 - tx;
            if (empty && hint != null && !focused) {
                float hw = Fonts.width(hint);
                Fonts.draw(ctx, hint, x + w - 8 - hw, y + h / 2f - 4, faint());
                avail -= hw + 6;
            }
            String shown = text;
            while (!shown.isEmpty() && Fonts.width(shown) > avail) shown = shown.substring(1);
            Fonts.draw(ctx, empty ? Fonts.trim(I18n.tr(placeholder), (int) avail) : shown, tx, y + h / 2f - 4, empty ? faint() : text());
            if (focused && (System.currentTimeMillis() - caretAt) % 1000 < 520) {
                float cx = tx + (empty ? 0 : Fonts.width(shown)) + 1;
                RenderUtils.drawRect(ctx, cx, y + h / 2f - 5, 1, 10, text());
            }
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (inside(mx, my, x, y, w, h)) {
                if (button == 1) text = "";
                focus();
                return true;
            }
            focused = false;
            return false;
        }

        public boolean keyPressed(int key, boolean ctrl) {
            if (!focused) return false;
            switch (key) {
                case InputConstants.KEY_ESCAPE -> focused = false;
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> onEnter.run();
                case InputConstants.KEY_BACKSPACE -> {
                    if (!text.isEmpty()) text = ctrl ? "" : text.substring(0, text.length() - 1);
                    caretAt = System.currentTimeMillis();
                }
                default -> { }
            }
            return true;
        }

        public boolean charTyped(char c) {
            if (!focused) return false;
            if (c >= 32 && text.length() < max) {
                text += c;
                caretAt = System.currentTimeMillis();
            }
            return true;
        }
    }
}
