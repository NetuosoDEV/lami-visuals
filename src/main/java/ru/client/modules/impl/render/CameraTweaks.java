package ru.client.modules.impl.render;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;

public final class CameraTweaks extends Module {

    private final BooleanSetting noHurtCam = register(new BooleanSetting("No Hurt Cam", true));

    public CameraTweaks() {
        super("Camera", "Без тряски камеры при уроне", Category.RENDER);
    }

    public boolean noHurtCam() { return noHurtCam.isEnabled(); }
}
