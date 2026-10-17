package ru.client.modules.impl.combat;

import ru.client.core.I18n;
import ru.client.utils.render.Fonts;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.HudModule;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.animation.Animation;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.RenderUtils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TargetHud extends HudModule {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final ModeSetting targets = register(new ModeSetting("Targets", "Players", "Players", "All"));
    private final ModeSetting healthColor = register(new ModeSetting("Health Color", "Health", "Health", "Theme"));
    private final BooleanSetting armor = register(new BooleanSetting("Armor", true));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final NumberSetting timeout = register(new NumberSetting("Timeout", 5, 1, 15, 0.5));

    private final Animation appear = new Animation(10f, 0f);
    private final Animation hpAnim = new Animation(9f, 1f);
    private final Animation trailAnim = new Animation(2.5f, 1f);
    private LivingEntity displayed;

    public TargetHud() {
        super("TargetHUD", "Плашка цели: голова, HP, броня", Category.COMBAT, -1, -1);
        width = 140;
        height = 46;
    }

    private LivingEntity findTarget() {
        LivingEntity t = CombatTracker.getTarget((long) (timeout.get() * 1000));
        if (t == null && mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) t = le;
        if (t != null && targets.is("Players") && !(t instanceof Player)) t = null;
        if (t == null && isEditing()) t = mc.player;
        return t;
    }

    @Override
    protected void renderHud(GuiGraphicsExtractor ctx, float x, float y, float tickDelta) {
        if (nullCheck()) return;

        LivingEntity t = findTarget();
        if (t != null) {
            if (t != displayed) {
                float pct = pct(t);
                hpAnim.snap(pct);
                trailAnim.snap(pct);
            }
            displayed = t;
            appear.setTarget(1f);
        } else {
            appear.setTarget(0f);
        }
        float a = appear.update();
        if (a < 0.02f || displayed == null) return;

        LivingEntity e = displayed;
        Theme theme = Theme.get();
        float pct = pct(e);
        hpAnim.setTarget(pct);
        trailAnim.setTarget(pct);
        float hp = hpAnim.update();
        float trail = trailAnim.update();
        if (trail < hp) { trailAnim.snap(hp); trail = hp; }

        var m = ctx.pose();
        m.pushMatrix();
        float cx = x + width / 2f, cy = y + height / 2f;
        m.translate(cx, cy);
        float s = 0.85f + 0.15f * a;
        m.scale(s, s);
        m.translate(-cx, -cy);

        int accA = theme.accentA(), accB = theme.accentB();
        if (glow.isEnabled()) RenderUtils.drawGlow(ctx, x, y, width, height, 7, 6, ColorUtils.multAlpha(accA, 0.5f * a));
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 7, ColorUtils.multAlpha(theme.panel(), a));

        int head = 34;
        int hx = Math.round(x + 6), hy = Math.round(y + 6);
        if (e instanceof AbstractClientPlayer p) {
            PlayerFaceExtractor.extractRenderState(ctx, p.getSkin(), hx, hy, head);
        } else {
            RenderUtils.drawRoundedRect(ctx, hx, hy, head, head, 5, 0xFF26262E);
            String letter = e.getName().getString().substring(0, 1).toUpperCase(Locale.ROOT);
            Fonts.draw(ctx, letter, hx + head / 2 - Fonts.width(letter) / 2, hy + 13, theme.text(), false);
        }
        if (e.hurtTime > 0) {
            int red = (int) (e.hurtTime / 10f * 140);
            RenderUtils.drawRect(ctx, hx, hy, head, head, ColorUtils.rgba(255, 40, 40, red));
        }

        float tx = x + 46;
        float right = x + width - 6;

        String name = Fonts.trim(e.getName().getString(), (int) (width - 46 - 40));
        Fonts.draw(ctx, name, Math.round(tx), Math.round(y + 7), theme.text(), false);
        float health = e.getHealth() + e.getAbsorptionAmount();
        String hpText = String.format(Locale.ROOT, "%.1f", health);
        int hpColor = healthColor(pct, theme);
        Fonts.draw(ctx, hpText, Math.round(right - Fonts.width(hpText)), Math.round(y + 7), hpColor, false);

        if (armor.isEnabled()) {
            float ix = tx;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = e.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                m.pushMatrix();
                m.translate(ix, y + 18);
                m.scale(0.625f, 0.625f);
                ctx.item(stack, 0, 0);
                m.popMatrix();
                ix += 11;
            }
        }
        if (e != mc.player) {
            String dist = I18n.tr("%.1f м", mc.player.distanceTo(e));
            Fonts.draw(ctx, dist, Math.round(right - Fonts.width(dist)), Math.round(y + 19), theme.textDim(), false);
        }

        float bx = tx, by = y + 33, bw = right - tx, bh = 6;
        RenderUtils.drawRoundedRect(ctx, bx, by, bw, bh, 3, 0xFF26262E);
        RenderUtils.drawRoundedRect(ctx, bx, by, bw * trail, bh, 3, 0x90FFFFFF);
        if (healthColor.is("Theme")) {
            RenderUtils.drawRoundedHorizontalGradient(ctx, bx, by, bw * hp, bh, 3, accA, accB);
        } else {
            RenderUtils.drawRoundedHorizontalGradient(ctx, bx, by, bw * hp, bh, 3,
                    ColorUtils.lerp(0xFFFF3B3B, hpColor, 0.4f), hpColor);
        }

        m.popMatrix();
    }

    @Override
    protected void initPosition(float sw, float sh) { setPosition(sw / 2f + 20, sh / 2f + 20); }

    private static float pct(LivingEntity e) {
        float max = Math.max(1f, e.getMaxHealth());
        return Mth.clamp((e.getHealth() + e.getAbsorptionAmount()) / max, 0f, 1f);
    }

    private int healthColor(float pct, Theme theme) {
        if (healthColor.is("Theme")) return theme.text();
        return pct > 0.5f ? ColorUtils.lerp(0xFFFFD84D, 0xFF4DFF88, (pct - 0.5f) * 2f)
                          : ColorUtils.lerp(0xFFFF3B3B, 0xFFFFD84D, pct * 2f);
    }
}
