package ru.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.SwingAnimations;
import ru.client.modules.impl.render.ViewModel;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Unique private boolean lami$normal;
    @Unique private boolean lami$skipSwing;
    @Unique private float lami$attack;
    @Unique private boolean lami$main;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void lamivisuals$head(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
                                  float xRot, InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                  PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        lami$skipSwing = false;
        lami$attack = attack;
        lami$main = hand == InteractionHand.MAIN_HAND;
        AvatarRenderState a = playerState.avatarRenderState;
        boolean using = a != null && a.isUsingItem && state.useItemRemainingTicks > 0 && a.useItemHand == hand;
        lami$normal = a != null && !state.isScoping && !stack.isEmpty() && !stack.has(DataComponents.MAP_ID)
                && !stack.is(Items.CROSSBOW) && !using && !a.isAutoSpinAttack;
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void lamivisuals$tail(CallbackInfo ci) {
        lami$normal = false;
        lami$skipSwing = false;
    }

    @Inject(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void lamivisuals$viewModel(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
                                       float xRot, InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                       PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        ViewModel vm = Modules.enabled(ViewModel.class);
        AvatarRenderState a = playerState.avatarRenderState;
        if (vm == null || a == null) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? a.mainArm : a.mainArm.getOpposite();
        vm.apply(pose, arm == HumanoidArm.RIGHT ? 1 : -1);
    }

    @Inject(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),
            require = 0)
    private void lamivisuals$itemScale(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
                                       float xRot, InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
                                       PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        ViewModel vm = Modules.enabled(ViewModel.class);
        if (vm != null) vm.applyScale(pose);
    }

    @Inject(method = "renderPlayerArm",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;renderPlayerHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;)V"),
            require = 0)
    private void lamivisuals$armScale(PoseStack pose, SubmitNodeCollector collector, int light, float inverseArmHeight,
                                      float attack, HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        ViewModel vm = Modules.enabled(ViewModel.class);
        if (vm != null) vm.applyScale(pose);
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float lamivisuals$equip(float inverseArmHeight) {
        SwingAnimations sa = Modules.enabled(SwingAnimations.class);
        return sa != null && sa.noEquip() ? 0f : inverseArmHeight;
    }

    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$armTransform(PoseStack pose, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
        if (!lami$normal) return;
        lami$normal = false;
        SwingAnimations sa = Modules.enabled(SwingAnimations.class);
        if (sa == null) return;
        if (sa.apply(pose, arm, lami$attack, inverseArmHeight, lami$main)) {
            lami$skipSwing = true;
            ci.cancel();
        }
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void lamivisuals$swingArm(float animation, PoseStack pose, int invert, HumanoidArm arm, CallbackInfo ci) {
        if (lami$skipSwing) {
            lami$skipSwing = false;
            ci.cancel();
        }
    }
}
