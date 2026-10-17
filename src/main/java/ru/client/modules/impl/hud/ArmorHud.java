package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

public final class ArmorHud extends HudModule {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final BooleanSetting hands = register(new BooleanSetting("Hands", true));
    private final BooleanSetting percent = register(new BooleanSetting("Percent", true));
    private final BooleanSetting alert = register(new BooleanSetting("Low Alert", true));
    private final NumberSetting alertAt = register(new NumberSetting("Alert At %", 20, 5, 50, 1));

    private final boolean[] warned = new boolean[4];

    public ArmorHud() {
        super("ArmorHUD", "Прочность брони", Category.HUD, -1, -1);
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        if (nullCheck()) return;
        Theme t = Theme.get();
        int slots = 4 + (hands.isEnabled() ? 2 : 0);
        width = slots * 20 + 6;
        height = percent.isEnabled() ? 32 : 24;
        boolean any = false;
        for (int i = 0; i < slots; i++) {
            ItemStack st = i < 4 ? mc.player.getItemBySlot(ARMOR[i])
                    : mc.player.getItemBySlot(i == 4 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (!st.isEmpty()) { any = true; break; }
        }
        if (!any && !isEditing()) return;
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());

        float ix = x + 5;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = i < 4 ? mc.player.getItemBySlot(ARMOR[i])
                    : mc.player.getItemBySlot(i == 4 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (!stack.isEmpty()) {
                ctx.item(stack, Math.round(ix), Math.round(y + 4));
                ctx.itemDecorations(mc.font, stack, Math.round(ix), Math.round(y + 4));
                if (percent.isEnabled() && stack.isDamageableItem()) {
                    float pct = 1f - stack.getDamageValue() / (float) stack.getMaxDamage();
                    String s = Math.round(pct * 100) + "%";
                    int c = pct > 0.5f ? ColorUtils.lerp(0xFFFFD84D, 0xFF4DFF88, (pct - 0.5f) * 2) : ColorUtils.lerp(0xFFFF3B3B, 0xFFFFD84D, pct * 2);
                    var m = ctx.pose();
                    m.pushMatrix();
                    m.translate(ix + 8, y + 22);
                    m.scale(0.7f, 0.7f);
                    Fonts.draw(ctx, s, -Fonts.width(s) / 2, 0, c, true);
                    m.popMatrix();
                }
            }
            ix += 20;
        }
    }

    @Override
    public void onTick() {
        if (nullCheck() || !alert.isEnabled()) return;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = mc.player.getItemBySlot(ARMOR[i]);
            if (stack.isEmpty() || !stack.isDamageableItem()) { warned[i] = false; continue; }
            float pct = 100f * (1f - stack.getDamageValue() / (float) stack.getMaxDamage());
            if (pct <= alertAt.get() && !warned[i]) {
                warned[i] = true;
                Notifications.show(I18n.tr("%s: %d%% прочности", stack.getHoverName().getString(), Math.round(pct)), 0xFFFFB020);
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 0.8f, 0.6f));
            } else if (pct > alertAt.get() + 5) {
                warned[i] = false;
            }
        }
    }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(sw / 2f + 96, sh - 34); }
}
