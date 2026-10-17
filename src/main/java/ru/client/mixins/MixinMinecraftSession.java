package ru.client.mixins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.client.core.alt.AltManager;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftSession {

    @Inject(method = "getGameProfile", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$altProfile(CallbackInfoReturnable<GameProfile> cir) {
        AltManager am = AltManager.get();
        GameProfile p = am == null ? null : am.activeProfile();
        if (p != null) cir.setReturnValue(p);
    }

    @Inject(method = "getProfileResult", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$altProfileResult(CallbackInfoReturnable<ProfileResult> cir) {
        AltManager am = AltManager.get();
        GameProfile p = am == null ? null : am.activeProfile();
        if (p != null) cir.setReturnValue(new ProfileResult(p));
    }

    @Inject(method = "getProfileKeyPairManager", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$altKeys(CallbackInfoReturnable<ProfileKeyPairManager> cir) {
        AltManager am = AltManager.get();
        if (am != null && am.altActive()) cir.setReturnValue(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
    }
}
