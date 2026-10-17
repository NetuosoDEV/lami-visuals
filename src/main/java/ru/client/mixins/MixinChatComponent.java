package ru.client.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.impl.hud.ChatTab;
import ru.client.ui.chat.ChatStyle;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private void lamivisuals$beginChat(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
                                       ChatComponent.DisplayMode mode, boolean insertions, CallbackInfo ci) {
        ChatStyle.graphics = graphics;
        ChatStyle.ticks = ticks;
        ChatStyle.drawing = true;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("RETURN"))
    private void lamivisuals$endChat(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
                                     ChatComponent.DisplayMode mode, boolean insertions, CallbackInfo ci) {
        ChatStyle.drawing = false;
        ChatStyle.graphics = null;
    }

    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"), argsOnly = true)
    private ChatComponent.ChatGraphicsAccess lamivisuals$wrap(ChatComponent.ChatGraphicsAccess access) {
        if (!ChatStyle.drawing || (!ChatTab.roundedChat() && !ChatTab.slideIn())) return access;
        return new ChatStyle(access, trimmedMessages);
    }
}
