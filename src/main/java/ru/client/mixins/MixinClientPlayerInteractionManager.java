package ru.client.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.core.ClientMod;
import ru.client.core.CombatTracker;
import ru.client.modules.impl.utility.FakePlayer;
import ru.client.modules.impl.utility.FakePlayerEntity;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinClientPlayerInteractionManager {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$onAttack(Player player, Entity target, CallbackInfo ci) {
        CombatTracker.onAttack(target);
        ClientMod.getInstance().getModuleManager().onAttack(target);
        if (target instanceof FakePlayerEntity fake && player instanceof LocalPlayer local) {
            FakePlayer.onHit(local, fake);
            ci.cancel();
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$noInteractFake(Player player, Entity target, EntityHitResult hit, InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (target instanceof FakePlayerEntity) cir.setReturnValue(InteractionResult.PASS);
    }
}
