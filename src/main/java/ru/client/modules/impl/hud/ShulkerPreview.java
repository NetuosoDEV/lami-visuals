package ru.client.modules.impl.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.RenderUtils;

public final class ShulkerPreview extends Module {

    private final BooleanSetting requireShift = register(new BooleanSetting("Hold Shift", false));
    private final BooleanSetting tint = register(new BooleanSetting("Box Color", true));

    public ShulkerPreview() {
        super("ShulkerPreview", "Содержимое шалкера в подсказке", Category.HUD);
        setToggled(true);
    }

    public boolean render(GuiGraphicsExtractor ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock box)) return false;
        if (requireShift.isEnabled() && !net.minecraft.client.Minecraft.getInstance().hasShiftDown()) return false;
        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        container.copyInto(items);

        Theme t = Theme.get();
        DyeColor dye = box.getColor();
        int accent = tint.isEnabled() && dye != null ? (0xFF000000 | dye.getTextureDiffuseColor()) : t.accentA();

        float w = 9 * 18 + 10, h = 3 * 18 + 24;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        float x = mouseX + 12, y = mouseY - 12;
        if (x + w > sw - 4) x = mouseX - 12 - w;
        y = Math.max(4, Math.min(y, sh - h - 4));

        var m = ctx.pose();
        m.pushMatrix();
        m.translate(0, 0);
        RenderUtils.drawGlow(ctx, x, y, w, h, 6, 5, ColorUtils.withAlpha(accent, 90));
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, 6, 0xF20E0E13);
        RenderUtils.drawRoundedHorizontalGradient(ctx, x + 6, y + 16, w - 12, 1, 0.5f, accent, ColorUtils.withAlpha(accent, 0));
        String title = Fonts.trim(stack.getHoverName().getString(), (int) w - 40);
        Fonts.drawBold(ctx, title, x + 6, y + 5, t.text());
        int filled = 0;
        for (ItemStack it : items) if (!it.isEmpty()) filled++;
        String cnt = filled + "/27";
        Fonts.draw(ctx, cnt, x + w - 6 - Fonts.width(cnt), y + 5, t.textDim());

        for (int i = 0; i < 27; i++) {
            float sx = x + 5 + (i % 9) * 18, sy = y + 20 + (i / 9) * 18;
            RenderUtils.drawRoundedRect(ctx, sx, sy, 17, 17, 3, 0x40FFFFFF & 0x22FFFFFF);
        }
        for (int i = 0; i < 27; i++) {
            ItemStack it = items.get(i);
            if (it.isEmpty()) continue;
            int sx = Math.round(x + 5 + (i % 9) * 18 + 0.5f), sy = Math.round(y + 20 + (i / 9) * 18 + 0.5f);
            ctx.item(it, sx, sy);
            ctx.itemDecorations(mc.font, it, sx, sy);
        }
        m.popMatrix();
        return true;
    }
}
