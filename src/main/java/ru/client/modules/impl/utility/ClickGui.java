package ru.client.modules.impl.utility;

import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.core.I18n;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.clickgui.ClickGuiScreen;

public final class ClickGui extends Module {

    public final ModeSetting style = register(new ModeSetting("Style", "Modern", "Modern", "Legacy"));
    public final ModeSetting language = register(new ModeSetting("Language", I18n.AUTO, I18n.AUTO, I18n.ENGLISH, I18n.RUSSIAN, I18n.SPANISH));
    public final NumberSetting scale = register(new NumberSetting("Scale", 1.0, 0.7, 1.4, 0.05));
    public final BooleanSetting blur = register(new BooleanSetting("Blur", true));
    public final NumberSetting dim = register(new NumberSetting("Background Dim", 45, 0, 90, 5));
    public final BooleanSetting sounds = register(new BooleanSetting("UI Sounds", true));
    public final NumberSetting volume = register(new NumberSetting("Sound Volume", 70, 0, 100, 5));

    private ClickGuiScreen screen;

    public ClickGui() {
        super("ClickGUI", "Меню модулей и его стиль", Category.UTILITY, InputConstants.KEY_RSHIFT);
        scale.visibleWhen(this::modern);
        volume.visibleWhen(sounds::isEnabled);
        setHidden(true);
    }

    public boolean modern() { return !style.is("Legacy"); }

    @Override
    protected void onEnable() {
        mc.schedule(() -> openScreen(null, null));
        setToggled(false);
    }

    private void openScreen(Screen parent, Category page) {
        if (screen == null) screen = new ClickGuiScreen();
        if (page != null) screen.openPage(page);
        if (mc.gui.screen() == screen) return;
        screen.setParent(parent);
        mc.gui.setScreen(screen);
    }

    public static void open(Screen parent) {
        ClickGui m = Modules.get(ClickGui.class);
        if (m != null) m.openScreen(parent, null);
    }

    public static void open(Screen parent, Category page) {
        ClickGui m = Modules.get(ClickGui.class);
        if (m != null) m.openScreen(parent, page);
    }

    public static ClickGui get() { return Modules.get(ClickGui.class); }
}
