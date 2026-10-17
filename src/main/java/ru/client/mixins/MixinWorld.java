package ru.client.mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.Ambience;

@Mixin(Level.class)
public abstract class MixinWorld {

    @Inject(method = {"getRainLevel", "getThunderLevel"}, at = @At("HEAD"), cancellable = true)
    private void lamivisuals$clearWeather(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientLevel)) return;
        Ambience a = Modules.enabled(Ambience.class);
        if (a != null && a.clearWeather()) cir.setReturnValue(0f);
    }
}
