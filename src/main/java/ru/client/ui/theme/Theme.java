package ru.client.ui.theme;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.utils.render.ColorUtils;

public final class Theme extends Module {

    private static Theme instance;

    private final ColorSetting accent1 = register(new ColorSetting("Accent 1", 0xFF8A5CFF));
    private final ColorSetting accent2 = register(new ColorSetting("Accent 2", 0xFF00D4FF));
    private final BooleanSetting animated = register(new BooleanSetting("Animated Accent", true));
    private final NumberSetting fadeSpeed = register(new NumberSetting("Fade Speed", 3, 1, 10, 0.5));
    private final ColorSetting panel = register(new ColorSetting("Panel", 0xF0121216));
    private final ColorSetting header = register(new ColorSetting("Header", 0xFF18181E));
    private final ColorSetting settingBg = register(new ColorSetting("Settings BG", 0xFF0E0E12));
    private final ColorSetting text = register(new ColorSetting("Text", 0xFFEDEDF5));
    private final ColorSetting textDim = register(new ColorSetting("Text Dim", 0xFF8A8A99));
    private final BooleanSetting customFont = register(new BooleanSetting("Custom Font", true));

    public Theme() {
        super("Theme", "Цвета интерфейса", Category.COLORS);
        fadeSpeed.visibleWhen(animated::isEnabled);
        setToggled(true);
        setHidden(true);
        instance = this;
    }

    public static Theme get() { return instance; }

    public int accent(int offsetMs) {
        if (!animated.isEnabled()) return offsetMs == 0 ? accent1.getArgb() : accent2.getArgb();
        return ColorUtils.fade(accent1.getArgb(), accent2.getArgb(), (int) (fadeSpeed.get() * 1000), offsetMs);
    }

    public int accentSolid() { return accent1.getArgb() | 0xFF000000; }

    public int accentA() { return accent(0); }
    public int accentB() { return accent((int) (fadeSpeed.get() * 500)); }

    public int panel() { return panel.getArgb(); }
    public int header() { return header.getArgb(); }
    public int settingBg() { return settingBg.getArgb(); }
    public int text() { return text.getArgb(); }
    public int textDim() { return textDim.getArgb(); }
    public boolean customFont() { return customFont.isEnabled(); }
}
