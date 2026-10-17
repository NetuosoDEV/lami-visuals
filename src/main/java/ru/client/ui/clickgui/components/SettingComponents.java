package ru.client.ui.clickgui.components;

import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.KeySetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.modules.settings.Setting;

public final class SettingComponents {
    private SettingComponents() {}

    public static Component create(Setting<?> setting) {
        return switch (setting) {
            case BooleanSetting b -> new BooleanComponent(b);
            case NumberSetting n -> new NumberComponent(n);
            case ModeSetting m -> new ModeComponent(m);
            case ColorSetting c -> new ColorComponent(c);
            case KeySetting k -> new BindComponent(k::getDisplayName, k::get, k::set);
            default -> throw new IllegalArgumentException("Unsupported setting type: " + setting.getClass().getName());
        };
    }
}
