package ru.client.mixins;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.Ambience;

@Mixin(EnvironmentAttributeProbe.class)
public abstract class MixinEnvironmentProbe {

    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void lamivisuals$ambienceColors(EnvironmentAttribute<?> attribute, float partialTicks, CallbackInfoReturnable<Object> cir) {
        if (attribute != EnvironmentAttributes.SKY_COLOR && attribute != EnvironmentAttributes.FOG_COLOR
                && attribute != EnvironmentAttributes.CLOUD_COLOR) return;
        Ambience a = Modules.enabled(Ambience.class);
        if (a == null) return;
        Object value = cir.getReturnValue();
        Object out = null;
        if (attribute == EnvironmentAttributes.SKY_COLOR && value instanceof Vector3fc v) out = a.sky(v);
        else if (attribute == EnvironmentAttributes.FOG_COLOR && value instanceof Vector3fc v) out = a.fog(v);
        else if (attribute == EnvironmentAttributes.CLOUD_COLOR && value instanceof Vector4fc v) out = a.clouds(v);
        if (out != null) cir.setReturnValue(out);
    }
}
