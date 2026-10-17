package ru.client.modules.impl.hud;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class Cooldowns extends HudModule {

    private final List<ItemStack> active = new ArrayList<>();
    private final Set<Identifier> seen = new HashSet<>();

    public Cooldowns() {
        super("Cooldowns", "Перезарядки предметов", Category.HUD, 6, 200);
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float td) {
        if (nullCheck()) return;
        var cd = mc.player.getCooldowns();
        active.clear();
        seen.clear();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) add(inv.getItem(i), cd);
        add(mc.player.getOffhandItem(), cd);

        Theme t = Theme.get();
        width = 110;
        if (active.isEmpty() && !isEditing()) { height = 16; return; }
        height = 16 + Math.max(1, active.size()) * 18 + 2;
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 6, t.panel());
        Fonts.draw(ctx, I18n.tr("Перезарядка"), Math.round(x + 6), Math.round(y + 4), t.text(), false);
        RenderUtils.drawHorizontalGradient(ctx, x + 6, y + 14, width - 12, 1, t.accentA(), t.accentB());

        float ry = y + 17;
        if (active.isEmpty()) {
            Fonts.draw(ctx, I18n.tr("Нет перезарядок"), Math.round(x + 6), Math.round(ry + 4), t.textDim(), false);
            return;
        }
        for (ItemStack stack : active) {
            float progress = cd.getCooldownPercent(stack, td);
            ctx.item(stack, Math.round(x + 5), Math.round(ry));
            float bx = x + 26, bw = width - 32;
            RenderUtils.drawRoundedRect(ctx, bx, ry + 7, bw, 3, 1.5f, 0xFF26262E);
            RenderUtils.drawRoundedHorizontalGradient(ctx, bx, ry + 7, bw * (1f - progress), 3, 1.5f, t.accentA(), t.accentB());
            ry += 18;
        }
    }

    private void add(ItemStack stack, net.minecraft.world.item.ItemCooldowns cd) {
        if (stack.isEmpty() || !cd.isOnCooldown(stack)) return;
        Identifier group = cd.getCooldownGroup(stack);
        if (seen.add(group)) active.add(stack);
    }
}
