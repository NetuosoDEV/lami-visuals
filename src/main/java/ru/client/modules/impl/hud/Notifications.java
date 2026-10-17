package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class Notifications extends Module {

    private final NumberSetting duration = register(new NumberSetting("Duration", 1.6, 0.5, 5, 0.1));

    private String text = "";
    private long shownAt;
    private final Animation open = new Animation(12f, 0f);
    private final Animation widthAnim = new Animation(14f, 40f);

    public Notifications() {
        super("Notifications", "Уведомления о включении модулей", Category.HUD);
        setToggled(true);
        setHidden(true);
    }

    private int dotColor = 0xFF4DFF88;

    public void push(Module m) {
        push(I18n.tr(m.isToggled() ? "%s: вкл" : "%s: выкл", m.getDisplayName()), m.isToggled() ? 0xFF4DFF88 : 0xFFFF4D4D);
    }

    public void push(String message, int color) {
        text = message;
        dotColor = color;
        shownAt = System.currentTimeMillis();
    }

    public static void show(String message, int color) {
        Notifications n = ru.client.modules.Modules.enabled(Notifications.class);
        if (n != null) n.push(message, color);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        boolean active = !text.isEmpty() && System.currentTimeMillis() - shownAt < duration.get() * 1000;
        open.setTarget(active ? 1f : 0f);
        float a = open.update();
        if (a < 0.02f) return;

        Theme t = Theme.get();
        float targetW = Fonts.width(text) + 30;
        widthAnim.setTarget(active ? targetW : 40f);
        float w = widthAnim.update();
        float h = 18;
        float x = mc.getWindow().getGuiScaledWidth() / 2f - w / 2f;
        float y = 4 - (1f - a) * 24;

        RenderUtils.drawGlow(ctx, x, y, w, h, h / 2f, 5, ColorUtils.multAlpha(t.accentA(), 0.5f * a));
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, h / 2f, ColorUtils.multAlpha(0xF0080808, a));
        int dot = dotColor;
        RenderUtils.drawCircle(ctx, x + 10, y + h / 2f, 3, ColorUtils.multAlpha(dot, a));

        float progress = 1f - (System.currentTimeMillis() - shownAt) / (float) (duration.get() * 1000);
        if (active) RenderUtils.drawRoundedHorizontalGradient(ctx, x + 8, y + h - 2, (w - 16) * Math.max(0, progress), 1.2f, 0.6f, t.accentA(), t.accentB());

        if (a > 0.6f && w > targetW - 6) {
            Fonts.draw(ctx, text, Math.round(x + 18), Math.round(y + 5), ColorUtils.multAlpha(t.text(), a), false);
        }
    }
}
