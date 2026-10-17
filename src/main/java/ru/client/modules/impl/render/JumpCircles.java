package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.particles.JumpTexture;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class JumpCircles extends Module {

    private static final double LIFT = 0.012;

    private final ModeSetting style = register(new ModeSetting("Style", "Gear", JumpTexture.modes()));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "White", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final NumberSetting radius = register(new NumberSetting("Radius", 1.3, 0.4, 3, 0.1));
    private final NumberSetting life = register(new NumberSetting("Lifetime", 1.0, 0.3, 3, 0.1));
    private final BooleanSetting rotation = register(new BooleanSetting("Rotation", true));
    private final NumberSetting rotationSpeed = register(new NumberSetting("Rotation Speed", 180, 30, 720, 10));
    private final BooleanSetting filled = register(new BooleanSetting("Filled", true));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final BooleanSetting others = register(new BooleanSetting("Others", false));

    private record Circle(Vec3 pos, long born, JumpTexture texture, float spin, float hue) {}

    private final List<Circle> circles = new ArrayList<>();
    private Int2DoubleOpenHashMap groundY = new Int2DoubleOpenHashMap();
    private final Map<JumpTexture, Render3D.TexBuf> buffers = new EnumMap<>(JumpTexture.class);

    public JumpCircles() {
        super("JumpCircles", "Круги при прыжке", Category.RENDER);
        customColor.visibleWhen(() -> color.is("Custom"));
        rotationSpeed.visibleWhen(rotation::isEnabled);
        filled.visibleWhen(() -> style.is(JumpTexture.CLASSIC));
        glow.visibleWhen(() -> style.is(JumpTexture.CLASSIC));
    }

    @Override
    protected void onDisable() {
        circles.clear();
        groundY.clear();
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        Int2DoubleOpenHashMap next = new Int2DoubleOpenHashMap();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Player p : mc.level.players()) {
            if (p != mc.player && !others.isEnabled()) continue;
            if (p.onGround()) {
                next.put(p.getId(), p.getY());
            } else if (groundY.containsKey(p.getId()) && p.getDeltaMovement().y > 0.2) {
                circles.add(new Circle(new Vec3(p.getX(), groundY.get(p.getId()) + LIFT, p.getZ()), System.currentTimeMillis(),
                        JumpTexture.pick(style.get()), r.nextBoolean() ? 1f : -1f, r.nextFloat()));
                if (circles.size() > 40) circles.remove(0);
            }
        }
        groundY = next;
    }

    private int colorOf(Circle c, float k) {
        Theme t = Theme.get();
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(3000, (int) (c.hue() * 3000), 0.6f, 1f, 255);
            case "White" -> 0xFFFFFFFF;
            case "Custom" -> customColor.getArgb();
            default -> ColorUtils.lerp(t.accentA(), t.accentB(), k);
        };
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (circles.isEmpty()) return;
        long now = System.currentTimeMillis();
        long lifeMs = (long) (life.get() * 1000);
        circles.removeIf(c -> now - c.born() > lifeMs);
        Theme t = Theme.get();
        for (Render3D.TexBuf b : buffers.values()) b.clear();
        Render3D.begin();
        Render3D.additive();
        for (Circle c : circles) {
            float k = (now - c.born()) / (float) lifeMs;
            float ease = 1f - (1f - k) * (1f - k) * (1f - k);
            float r = radius.getFloat() * ease;
            float a = 1f - k;
            float angle = rotation.isEnabled() ? (float) Math.toRadians(rotationSpeed.get() * (now - c.born()) / 1000.0) * c.spin() : 0f;
            double x = c.pos().x, y = c.pos().y, z = c.pos().z;

            if (c.texture() != null) {
                Render3D.TexBuf b = buffers.computeIfAbsent(c.texture(), key -> new Render3D.TexBuf());
                Render3D.flatTexturedQuad(ms, b, x, y, z, r / JumpTexture.RADIUS_FRACTION, angle,
                        ColorUtils.multAlpha(colorOf(c, k), a));
                continue;
            }

            boolean themed = color.is("Theme");
            int solid = colorOf(c, k);
            float phase = angle / (float) (Math.PI * 2);
            if (filled.isEnabled()) {
                Render3D.disc(ms, x, y, z, r, ColorUtils.withAlpha(themed ? t.accentA() : solid, 0),
                        ColorUtils.multAlpha(themed ? t.accentB() : solid, a * 0.35f));
            }
            if (glow.isEnabled()) {
                float g = 0.16f;
                Render3D.ringGradient(ms, x, y, z, r, r + g,
                        ColorUtils.multAlpha(themed ? t.accentA() : solid, a * 0.45f), ColorUtils.withAlpha(themed ? t.accentA() : solid, 0));
                Render3D.ringGradient(ms, x, y, z, Math.max(0, r - g), r,
                        ColorUtils.withAlpha(themed ? t.accentB() : solid, 0), ColorUtils.multAlpha(themed ? t.accentB() : solid, a * 0.3f));
            }
            Render3D.ring(ms, x, y, z, r, 0.07f, tt -> ColorUtils.multAlpha(themed
                    ? ColorUtils.lerp(t.accentA(), t.accentB(), (float) Math.abs(Math.sin((tt + phase) * Math.PI)))
                    : solid, a));
        }
        Render3D.end();
        for (Map.Entry<JumpTexture, Render3D.TexBuf> e : buffers.entrySet()) {
            if (!e.getValue().isEmpty()) Render3D.drawTextured(e.getKey().texture, e.getValue());
        }
    }
}
