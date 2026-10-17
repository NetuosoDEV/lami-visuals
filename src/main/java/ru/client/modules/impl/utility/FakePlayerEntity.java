package ru.client.modules.impl.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

public final class FakePlayerEntity extends RemotePlayer {

    private final boolean copySkin;

    public FakePlayerEntity(ClientLevel level, GameProfile profile, boolean copySkin) {
        super(level, profile);
        this.copySkin = copySkin;
    }

    @Override
    public PlayerSkin getSkin() {
        LocalPlayer self = Minecraft.getInstance().player;
        return copySkin && self != null ? self.getSkin() : super.getSkin();
    }
}
