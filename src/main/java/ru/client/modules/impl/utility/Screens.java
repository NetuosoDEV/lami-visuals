package ru.client.modules.impl.utility;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;

public final class Screens extends Module {

    private final BooleanSetting pauseMenu = register(new BooleanSetting("Pause Menu", true));
    private final BooleanSetting deathScreen = register(new BooleanSetting("Death Screen", true));

    public Screens() {
        super("Screens", "Свои экраны паузы и смерти", Category.UTILITY);
        setHidden(true);
        setToggled(true);
    }

    public boolean pauseMenu() { return pauseMenu.isEnabled(); }

    public boolean deathScreen() { return deathScreen.isEnabled(); }
}
