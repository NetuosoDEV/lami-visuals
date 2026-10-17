package ru.client.mixins;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor("causeOfDeath")
    Component lamivisuals$causeOfDeath();

    @Accessor("hardcore")
    boolean lamivisuals$hardcore();

    @Accessor("player")
    LocalPlayer lamivisuals$player();
}
