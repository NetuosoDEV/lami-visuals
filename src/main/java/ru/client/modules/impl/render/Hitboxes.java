package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;

public final class Hitboxes extends Module {

    private final ModeSetting targets = register(new ModeSetting("Targets", "Players", "Players", "Mobs", "All"));
    private final NumberSetting range = register(new NumberSetting("Range", 24, 4, 64, 1));
    private final BooleanSetting themeColor = register(new BooleanSetting("Theme Color", true));
    private final ColorSetting color = register(new ColorSetting("Color", 0xFFFFFFFF));
    private final BooleanSetting fill = register(new BooleanSetting("Fill", true));
    private final NumberSetting fillAlpha = register(new NumberSetting("Fill Alpha", 35, 5, 120, 5));
    private final NumberSetting lineWidth = register(new NumberSetting("Line Width", 1.5, 0.5, 4, 0.5));
    private final BooleanSetting hitColor = register(new BooleanSetting("Hurt Color", true));

    public Hitboxes() {
        super("Hitboxes", "Цветные хитбоксы сущностей", Category.RENDER);
        color.visibleWhen(() -> !themeColor.isEnabled());
        fillAlpha.visibleWhen(fill::isEnabled);
    }

    private boolean matches(Entity e) {
        if (!(e instanceof LivingEntity l) || e == mc.player || !l.isAlive() || e.isInvisibleTo(mc.player)) return false;
        return switch (targets.get()) {
            case "Players" -> e instanceof Player;
            case "Mobs" -> e instanceof Enemy;
            default -> true;
        };
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) return;
        double r2 = range.get() * range.get();
        Theme t = Theme.get();
        Render3D.begin();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!matches(e) || e.distanceToSqr(mc.player) > r2) continue;
            LivingEntity l = (LivingEntity) e;
            Vec3 lerp = e.getPosition(td);
            AABB box = e.getBoundingBox().move(lerp.subtract(e.position()));
            int c = themeColor.isEnabled() ? t.accentA() : color.getArgb();
            if (CombatTracker.attackedRecently(e, 3000)) c = t.accentB();
            if (hitColor.isEnabled() && l.hurtTime > 0) c = ColorUtils.lerp(c, 0xFFFF3030, l.hurtTime / 10f);
            if (fill.isEnabled()) Render3D.filledBox(ms, box, ColorUtils.withAlpha(c, fillAlpha.getInt()));
            Render3D.outlineBox(ms, box, ColorUtils.withAlpha(c, 230), lineWidth.getFloat());
        }
        Render3D.end();
    }
}
