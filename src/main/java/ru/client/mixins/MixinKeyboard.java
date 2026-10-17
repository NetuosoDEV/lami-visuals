package ru.client.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.core.ClientMod;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void lamivisuals$onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (action != InputConstants.PRESS || client.gui.screen() != null) return;
        ClientMod.getInstance().getModuleManager().onKey(event.key());
    }
}
