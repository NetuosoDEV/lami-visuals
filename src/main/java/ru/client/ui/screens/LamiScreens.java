package ru.client.ui.screens;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import ru.client.mixins.DeathScreenAccessor;
import ru.client.modules.Modules;
import ru.client.modules.impl.utility.Screens;

public final class LamiScreens {
    private LamiScreens() {}

    private static boolean vanillaOnce;

    public static void allowVanillaOnce() { vanillaOnce = true; }

    public static Screen replace(Screen screen) {
        if (screen == null) return null;
        Screens s = Modules.enabled(Screens.class);
        if (screen.getClass() == PauseScreen.class) {
            if (vanillaOnce) { vanillaOnce = false; return null; }
            if (s == null || !s.pauseMenu() || !((PauseScreen) screen).showsPauseMenu()) return null;
            return new LamiPauseScreen();
        }
        if (screen.getClass() == DeathScreen.class && s != null && s.deathScreen()) {
            DeathScreenAccessor d = (DeathScreenAccessor) screen;
            return new LamiDeathScreen(d.lamivisuals$causeOfDeath(), d.lamivisuals$hardcore(), d.lamivisuals$player());
        }
        return null;
    }
}
