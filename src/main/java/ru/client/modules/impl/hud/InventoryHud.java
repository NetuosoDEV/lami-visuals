package ru.client.modules.impl.hud;

import ru.client.utils.render.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.RenderUtils;

public final class InventoryHud extends HudModule {

    public InventoryHud() {
        super("InventoryHUD", "Инвентарь на экране", Category.HUD, 6, 260);
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        if (nullCheck()) return;
        Theme t = Theme.get();
        width = 9 * 18 + 8;
        height = 3 * 18 + 8;
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());
        var inv = mc.player.getInventory();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                float sx = x + 4 + col * 18, sy = y + 4 + row * 18;
                RenderUtils.drawRoundedRect(ctx, sx, sy, 17, 17, 3, 0x18FFFFFF);
                ItemStack stack = inv.getItem(9 + row * 9 + col);
                if (stack.isEmpty()) continue;
                ctx.item(stack, Math.round(sx), Math.round(sy));
                ctx.itemDecorations(mc.font, stack, Math.round(sx), Math.round(sy));
            }
        }
    }
}
