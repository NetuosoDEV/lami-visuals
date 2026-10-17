package ru.client.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.client.modules.Modules;
import ru.client.modules.impl.utility.MainMenu;
import ru.client.ui.menu.LamiMainMenu;
import ru.client.ui.screens.LamiScreens;

@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class MixinMinecraftClient {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen lamivisuals$mainMenu(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class && Modules.enabled(MainMenu.class) != null) {
            return new LamiMainMenu();
        }
        Screen replaced = LamiScreens.replace(screen);
        return replaced != null ? replaced : screen;
    }
}
