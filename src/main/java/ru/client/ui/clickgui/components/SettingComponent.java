package ru.client.ui.clickgui.components;

import ru.client.modules.settings.Setting;

public abstract class SettingComponent<S extends Setting<?>> extends Component {
    protected final S setting;

    protected SettingComponent(S setting) { this.setting = setting; }

    @Override
    public boolean isVisible() { return setting.isVisible(); }
}
