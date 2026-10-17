package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;

public final class ChinaHat extends Module {

    private final NumberSetting radius = register(new NumberSetting("Radius", 0.65, 0.3, 1.2, 0.05));
    private final NumberSetting height = register(new NumberSetting("Height", 0.3, 0.1, 0.8, 0.05));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", 0.55, 0.1, 1, 0.05));
    private final BooleanSetting firstPerson = register(new BooleanSetting("First Person", false));
    private final BooleanSetting others = register(new BooleanSetting("Others", false));

    public ChinaHat() {
        super("ChinaHat", "Конус над головой", Category.RENDER);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) return;
        Theme t = Theme.get();
        Render3D.begin();
        for (Player p : mc.level.players()) {
            if (p != mc.player && !others.isEnabled()) continue;
            if (p.isInvisible()) continue;
            if (p == mc.player && mc.options.getCameraType().isFirstPerson() && !firstPerson.isEnabled()) continue;

            Vec3 pos = p.getPosition(td);
            double baseY = pos.y + p.getBbHeight() + 0.02;
            float yaw = -p.getYHeadRot();
            float a = alpha.getFloat();
            int cA = t.accentA(), cB = t.accentB();
            Render3D.cone(ms, pos.x, baseY, pos.z, radius.getFloat(), height.getFloat(), yaw,
                    tt -> ColorUtils.multAlpha(ColorUtils.lerp(cA, cB, (float) Math.abs(Math.sin(tt * Math.PI))), a),
                    ColorUtils.multAlpha(0xFFFFFFFF, a * 0.6f));
            Render3D.ring(ms, pos.x, baseY, pos.z, radius.getFloat(), 0.03f,
                    tt -> ColorUtils.lerp(cA, cB, (float) Math.abs(Math.sin(tt * Math.PI))));
        }
        Render3D.end();
    }
}
