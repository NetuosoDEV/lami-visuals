package ru.client.modules.impl.render;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;

public final class NoRender extends Module {

    private final BooleanSetting fire = register(new BooleanSetting("Fire Overlay", true));
    private final BooleanSetting pumpkin = register(new BooleanSetting("Pumpkin", true));
    private final BooleanSetting vignette = register(new BooleanSetting("Vignette", true));
    private final BooleanSetting bossBar = register(new BooleanSetting("Boss Bar", false));
    private final BooleanSetting scoreboard = register(new BooleanSetting("Scoreboard", false));

    public NoRender() {
        super("NoRender", "Скрыть оверлеи", Category.RENDER);
    }

    public boolean fire() { return fire.isEnabled(); }
    public boolean pumpkin() { return pumpkin.isEnabled(); }
    public boolean vignette() { return vignette.isEnabled(); }
    public boolean bossBar() { return bossBar.isEnabled(); }
    public boolean scoreboard() { return scoreboard.isEnabled(); }
}
