package ru.client.modules.impl.hud;

import ru.client.utils.render.Fonts;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class LowHp extends Module {

    private final NumberSetting threshold = register(new NumberSetting("Threshold", 6, 1, 15, 0.5));
    private final BooleanSetting vignette = register(new BooleanSetting("Edges", true));
    private final BooleanSetting heart = register(new BooleanSetting("Heart", true));
    private final BooleanSetting sound = register(new BooleanSetting("Heartbeat", true));

    private long lastBeat;

    public LowHp() {
        super("LowHP", "Предупреждение о низком HP", Category.HUD);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (nullCheck() || mc.player.isCreative() || mc.player.isSpectator()) return;
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (hp > threshold.get() || mc.player.isDeadOrDying()) return;

        float severity = 1f - hp / threshold.getFloat();
        long now = System.currentTimeMillis();
        long period = (long) (900 - 450 * severity);
        float beat = (float) Math.pow(Math.max(0, Math.sin((now % period) / (double) period * Math.PI)), 6);
        float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();

        if (vignette.isEnabled()) {
            int a = (int) (60 + 110 * severity * (0.6f + 0.4f * beat));
            int edge = ColorUtils.rgba(220, 20, 30, a), clear = ColorUtils.rgba(220, 20, 30, 0);
            float d = 50;
            RenderUtils.drawVerticalGradient(ctx, 0, 0, sw, d, edge, clear);
            RenderUtils.drawVerticalGradient(ctx, 0, sh - d, sw, d, clear, edge);
            RenderUtils.drawHorizontalGradient(ctx, 0, 0, d, sh, edge, clear);
            RenderUtils.drawHorizontalGradient(ctx, sw - d, 0, d, sh, clear, edge);
        }
        if (heart.isEnabled()) {
            float cx = sw / 2f, cy = sh / 2f + 30;
            float size = 7 + 2.5f * beat;
            RenderUtils.drawHeart(ctx, cx, cy, size + 2, 0x90000000);
            RenderUtils.drawHeart(ctx, cx, cy, size, ColorUtils.lerp(0xFFB0101A, 0xFFFF3040, beat));
            String s = String.format(Locale.ROOT, "%.1f", hp);
            Fonts.draw(ctx, s, Math.round(cx - Fonts.width(s) / 2f), Math.round(cy + 11), 0xFFFFFFFF, true);
        }
        if (sound.isEnabled() && now - lastBeat > period) {
            lastBeat = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1f, 0.6f));
        }
    }
}
