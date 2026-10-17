package ru.client.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;
import ru.client.utils.render.ScreenFx;

public final class HitFx extends Module {

    private final BooleanSetting edgeFlash = register(new BooleanSetting("Edge Flash", true));
    private final ModeSetting flashColor = register(new ModeSetting("Flash Color", "Theme", "Theme", "Red", "White", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final NumberSetting flashSize = register(new NumberSetting("Flash Size", 45, 15, 120, 5));
    private final NumberSetting flashStrength = register(new NumberSetting("Flash Strength", 0.6, 0.1, 1.0, 0.05));
    private final BooleanSetting critOnly = register(new BooleanSetting("Crit Only", false));
    private final BooleanSetting chromatic = register(new BooleanSetting("Chromatic", true));
    private final NumberSetting chromaticStrength = register(new NumberSetting("Chromatic Strength", 1.0, 0.2, 2.0, 0.1));
    private final NumberSetting duration = register(new NumberSetting("Duration", 0.3, 0.1, 1.0, 0.05));

    private long flashAt, critAt;
    private boolean lastCrit;

    public HitFx() {
        super("HitFX", "Вспышка по краям экрана и RGB-сдвиг на крите", Category.COMBAT);
        flashColor.visibleWhen(edgeFlash::isEnabled);
        customColor.visibleWhen(() -> edgeFlash.isEnabled() && flashColor.is("Custom"));
        flashSize.visibleWhen(edgeFlash::isEnabled);
        flashStrength.visibleWhen(edgeFlash::isEnabled);
        critOnly.visibleWhen(edgeFlash::isEnabled);
        chromaticStrength.visibleWhen(chromatic::isEnabled);
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        long now = System.currentTimeMillis();
        lastCrit = CombatTracker.isRecentCrit(target, 50);
        if (!critOnly.isEnabled() || lastCrit) flashAt = now;
        if (lastCrit) critAt = now;
    }

    private float fade(long at, float mult) {
        float k = (System.currentTimeMillis() - at) / (duration.getFloat() * 1000f * mult);
        if (k >= 1f || k < 0f) return 0f;
        return (1f - k) * (1f - k);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (chromatic.isEnabled()) ScreenFx.setStrength(fade(critAt, 1.2f) * chromaticStrength.getFloat());
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (!edgeFlash.isEnabled() || nullCheck()) return;
        float f = fade(flashAt, lastCrit ? 1.3f : 1f);
        if (f <= 0.01f) return;
        int base = switch (flashColor.get()) {
            case "Red" -> 0xFFFF2A3A;
            case "White" -> 0xFFFFFFFF;
            case "Custom" -> customColor.getArgb();
            default -> lastCrit ? Theme.get().accentB() : Theme.get().accentA();
        };
        float strength = flashStrength.getFloat() * (lastCrit ? 1f : 0.7f);
        int edge = ColorUtils.withAlpha(base, (int) (255 * Math.min(1f, f * strength)));
        int clear = ColorUtils.withAlpha(base, 0);
        float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        float d = flashSize.getFloat() * (lastCrit ? 1.3f : 1f);
        RenderUtils.drawVerticalGradient(ctx, 0, 0, sw, d, edge, clear);
        RenderUtils.drawVerticalGradient(ctx, 0, sh - d, sw, d, clear, edge);
        RenderUtils.drawHorizontalGradient(ctx, 0, 0, d, sh, edge, clear);
        RenderUtils.drawHorizontalGradient(ctx, sw - d, 0, d, sh, clear, edge);
    }
}
