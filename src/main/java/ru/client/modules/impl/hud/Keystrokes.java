package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Keystrokes extends HudModule {

    private static final float K = 22, G = 2;

    private final BooleanSetting mouse = register(new BooleanSetting("Mouse + CPS", true));
    private final BooleanSetting space = register(new BooleanSetting("Space", true));

    private final Animation[] anims = new Animation[7];
    private final Deque<Long> lmb = new ArrayDeque<>(), rmb = new ArrayDeque<>();
    private boolean lastL, lastR;

    public Keystrokes() {
        super("Keystrokes", "Нажатия клавиш и CPS", Category.HUD, -1, -1);
        for (int i = 0; i < anims.length; i++) anims[i] = new Animation(16f, 0f);
    }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(6, 125); }

    private static int cps(Deque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        var o = mc.options;
        long now = System.currentTimeMillis();
        boolean l = o.keyAttack.isDown(), r = o.keyUse.isDown();
        if (l && !lastL) lmb.addLast(now);
        if (r && !lastR) rmb.addLast(now);
        lastL = l;
        lastR = r;

        width = K * 3 + G * 2;
        float cy = y;
        key(ctx, 0, o.keyUp, "W", x + K + G, cy, K, K);
        cy += K + G;
        key(ctx, 1, o.keyLeft, "A", x, cy, K, K);
        key(ctx, 2, o.keyDown, "S", x + K + G, cy, K, K);
        key(ctx, 3, o.keyRight, "D", x + (K + G) * 2, cy, K, K);
        cy += K + G;
        if (mouse.isEnabled()) {
            float mw = (width - G) / 2f;
            box(ctx, 4, l, I18n.tr("ЛКМ"), I18n.tr("%d кл/с", cps(lmb, now)), x, cy, mw, K);
            box(ctx, 5, r, I18n.tr("ПКМ"), I18n.tr("%d кл/с", cps(rmb, now)), x + mw + G, cy, mw, K);
            cy += K + G;
        }
        if (space.isEnabled()) {
            boolean p = o.keyJump.isDown();
            Theme t = Theme.get();
            float a = press(6, p);
            base(ctx, x, cy, width, 12, a, t);
            RenderUtils.drawRoundedRect(ctx, x + width / 2f - 12, cy + 5.5f, 24, 1.5f, 0.75f, ColorUtils.lerp(t.textDim(), 0xFFFFFFFF, a));
            cy += 12 + G;
        }
        height = cy - y - G;
    }

    private float press(int i, boolean pressed) {
        anims[i].setTarget(pressed ? 1f : 0f);
        return anims[i].update();
    }

    private void base(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float a, Theme t) {
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 5, t.panel());
        if (a > 0.01f) RenderUtils.drawRoundedHorizontalGradient(ctx, x, y, w, h, 5,
                ColorUtils.multAlpha(t.accentA(), a * 0.9f), ColorUtils.multAlpha(t.accentB(), a * 0.9f));
    }

    private void key(GuiGraphicsExtractor ctx, int i, KeyMapping kb, String label, float x, float y, float w, float h) {
        Theme t = Theme.get();
        float a = press(i, kb.isDown());
        base(ctx, x, y, w, h, a, t);
        Fonts.drawCentered(ctx, label, x + w / 2f, y + h / 2f - 4, ColorUtils.lerp(t.text(), 0xFFFFFFFF, a), Fonts.BOLD);
    }

    private void box(GuiGraphicsExtractor ctx, int i, boolean pressed, String label, String sub, float x, float y, float w, float h) {
        Theme t = Theme.get();
        float a = press(i, pressed);
        base(ctx, x, y, w, h, a, t);
        Fonts.drawCentered(ctx, label, x + w / 2f, y + 3, ColorUtils.lerp(t.text(), 0xFFFFFFFF, a), Fonts.BOLD);
        var m = ctx.pose();
        m.pushMatrix();
        m.translate(x + w / 2f, y + 13.5f);
        m.scale(0.7f, 0.7f);
        Fonts.drawCentered(ctx, sub, 0, 0, ColorUtils.lerp(t.textDim(), 0xFFFFFFFF, a), Fonts.REGULAR);
        m.popMatrix();
    }
}
