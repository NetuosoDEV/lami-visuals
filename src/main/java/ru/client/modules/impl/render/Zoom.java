package ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.KeySetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.utils.animation.Animation;

public final class Zoom extends Module {

    private final KeySetting zoomKey = register(new KeySetting("Zoom Key", InputConstants.KEY_C));
    private final NumberSetting factor = register(new NumberSetting("Factor", 4, 1.5, 10, 0.5));
    private final NumberSetting speed = register(new NumberSetting("Smoothness", 12, 3, 30, 1));

    private final Animation anim = new Animation(12f, 1f);

    public Zoom() {
        super("Zoom", "Плавное приближение", Category.RENDER);
    }

    public float fovMultiplier() {
        boolean active = isToggled() && mc.gui.screen() == null && zoomKey.get() != -1
                && InputConstants.isKeyDown(zoomKey.get());
        anim.setSpeed(speed.getFloat());
        anim.setTarget(active ? 1f / factor.getFloat() : 1f);
        return anim.update();
    }
}
