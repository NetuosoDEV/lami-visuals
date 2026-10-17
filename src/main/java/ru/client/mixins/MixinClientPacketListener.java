package ru.client.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.client.modules.impl.combat.TotemPop;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Redirect(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"))
    private void lamivisuals$totem(ParticleEngine engine, Entity entity, ParticleOptions particle, int lifeTime) {
        TotemPop.onPop(entity);
        if (!TotemPop.hideVanilla(entity)) engine.createTrackingEmitter(entity, particle, lifeTime);
    }
}
