package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;

public final class SwingAnimations extends Module {

    public static final String[] MODES = {
            "Vanilla Swing", "Lift Swing", "Bob", "Side Slash", "Shake", "Wrist Roll", "Flip Spin", "Spin",
            "Shrink", "Block Sway", "Block Sweep", "Block Tilt", "Stab", "Chop", "Heavy Slash"
    };

    private final ModeSetting mode = register(new ModeSetting("Animation", "Vanilla Swing", MODES));
    private final NumberSetting duration = register(new NumberSetting("Duration (ticks)", 12, 1, 50, 1));
    private final BooleanSetting vanillaSway = register(new BooleanSetting("Vanilla Sway", false));
    private final ModeSetting hands = register(new ModeSetting("Hands", "Both", "Both", "Main Only"));
    private final BooleanSetting noEquip = register(new BooleanSetting("No Equip Anim", false));

    private float lastProgress;
    private boolean flipSign;

    public SwingAnimations() {
        super("SwingAnimations", "15 анимаций удара + длительность", Category.RENDER);
        vanillaSway.visibleWhen(() -> mode.is("Vanilla Swing"));
    }

    public boolean noEquip() { return noEquip.isEnabled(); }

    public int durationTicks() { return duration.getInt(); }

    private static boolean rightOnly(String m) {
        return switch (m) {
            case "Side Slash", "Block Sway", "Block Sweep", "Block Tilt", "Chop", "Heavy Slash" -> true;
            default -> false;
        };
    }

    public boolean apply(PoseStack m, HumanoidArm arm, float p, float e, boolean mainHand) {
        if (hands.is("Main Only") && !mainHand) return false;
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        String anim = mode.get();
        if (i < 0 && rightOnly(anim)) anim = "Vanilla Swing";

        float pi = (float) Math.PI;
        float s = Mth.sin(Mth.sqrt(p) * pi);
        float q = Mth.sin(p * p * pi);
        float w = Mth.sin(Mth.sqrt(p) * 2 * pi);

        if (mainHand) {
            if (p > 0 && (lastProgress == 0 || p < lastProgress)) flipSign = !flipSign;
            lastProgress = p;
        }

        switch (anim) {
            case "Vanilla Swing" -> {
                if (vanillaSway.isEnabled()) m.translate(i * (-0.4f * s), 0.2f * w, -0.2f * Mth.sin(p * pi));
                base(m, i, e);
                ry(m, i * (45f - 20f * q));
                rz(m, i * (-20f * s));
                rx(m, -80f * s);
                ry(m, i * -45f);
            }
            case "Lift Swing" -> {
                base(m, i, -0.4f * s);
                ry(m, i * (45f - 20f * q));
                rz(m, i * (-20f * s));
                ry(m, i * -45f);
            }
            case "Bob" -> base(m, i, 0.2f * w);
            case "Side Slash" -> {
                base(m, i, -0.4f * s);
                ry(m, i * 45f);
                rz(m, i * (-70f * s));
                rx(m, -70f);
                ry(m, i * -45f);
            }
            case "Shake" -> {
                base(m, i, 0);
                rx(m, p > 0 ? -37f * Mth.sin(13f * p) : 0f);
            }
            case "Wrist Roll" -> {
                base(m, i, 0);
                rz(m, i * (-20f * s));
            }
            case "Flip Spin" -> {
                base(m, i, e);
                rx(m, (flipSign ? 360f : -360f) * p);
            }
            case "Spin" -> {
                base(m, i, e);
                rx(m, -360f * p);
            }
            case "Shrink" -> {
                base(m, i, e);
                float k = 1f - Mth.sin(3f * p) / 2f;
                m.scale(k, k, k);
            }
            case "Block Sway" -> {
                base(m, i, 0);
                rx(m, 50f);
                ry(m, -30f * (1f - s) - 30f);
                rz(m, 110f);
            }
            case "Block Sweep" -> {
                base(m, i, 0);
                rx(m, 50f);
                ry(m, -60f * s - 50f);
                rz(m, 110f);
            }
            case "Block Tilt" -> {
                base(m, i, 0);
                rx(m, 50f);
                ry(m, -60f);
                rz(m, 110f + 20f * s);
            }
            case "Stab" -> {
                base(m, i, 0);
                m.translate(0, 0, -s / 4f);
                rx(m, -120f);
            }
            case "Chop" -> {
                base(m, i, 0);
                rx(m, -60f * Mth.sin(3f * p));
                rz(m, -60f * s);
            }
            case "Heavy Slash" -> {
                if (p > 0) {
                    m.translate(0.56f, -0.5f - 0.2f * e, -0.7f);
                    ry(m, 45f);
                    rx(m, -85f * s);
                    m.translate(-0.1f, 0.28f, 0.2f);
                    rx(m, -85f);
                } else {
                    base(m, i, e);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private static void base(PoseStack m, int i, float e) { m.translate(0.56f * i, -0.52f - 0.6f * e, -0.72f); }

    private static void rx(PoseStack m, float deg) { m.rotate(Axis.XP.rotationDegrees(deg)); }
    private static void ry(PoseStack m, float deg) { m.rotate(Axis.YP.rotationDegrees(deg)); }
    private static void rz(PoseStack m, float deg) { m.rotate(Axis.ZP.rotationDegrees(deg)); }
}
