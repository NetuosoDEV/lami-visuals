package ru.client.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.client.modules.impl.render.FreeLook;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void lamivisuals$freeLook(LocalPlayer player, double xo, double yo, Operation<Void> original) {
        if (!FreeLook.turn(xo, yo)) original.call(player, xo, yo);
    }
}
