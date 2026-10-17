package ru.client.modules.impl.utility;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;

public final class MainMenu extends Module {

    public static final String[] BACKGROUNDS = {"Constellation", "Aurora", "Waves", "Starfield", "Snow", "Bubbles", "Panorama"};

    public final ModeSetting background = register(new ModeSetting("Background", "Constellation", BACKGROUNDS));
    public final NumberSetting speed = register(new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1));
    public final BooleanSetting parallax = register(new BooleanSetting("Parallax", true));
    public final NumberSetting particleCount = register(new NumberSetting("Particle Count", 70, 20, 150, 5));
    public final BooleanSetting lines = register(new BooleanSetting("Constellation Lines", true));
    public final BooleanSetting changelog = register(new BooleanSetting("What's New", true));

    public MainMenu() {
        super("MainMenu", "Кастомное главное меню и его фон", Category.UTILITY);
        particleCount.visibleWhen(() -> !background.is("Panorama") && !background.is("Aurora") && !background.is("Waves"));
        lines.visibleWhen(() -> background.is("Constellation"));
        setHidden(true);
        setToggled(true);
    }
}
