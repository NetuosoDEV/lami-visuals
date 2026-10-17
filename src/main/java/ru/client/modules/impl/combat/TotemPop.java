package ru.client.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;
import ru.client.core.I18n;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.impl.hud.Notifications;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Fonts;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.WorldToScreen;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class TotemPop extends Module {

    private final BooleanSetting playersOnly = register(new BooleanSetting("Players Only", true));
    private final BooleanSetting self = register(new BooleanSetting("Self", false));
    private final BooleanSetting counter = register(new BooleanSetting("Counter", true));
    private final NumberSetting counterScale = register(new NumberSetting("Counter Scale", 1.0, 0.5, 2.0, 0.05));
    private final BooleanSetting popup = register(new BooleanSetting("Popup", true));
    private final BooleanSetting burst = register(new BooleanSetting("Burst", true));
    private final BooleanSetting hideVanilla = register(new BooleanSetting("Hide Vanilla", false));
    private final BooleanSetting notify = register(new BooleanSetting("Notify", true));
    private final ModeSetting color = register(new ModeSetting("Color", "Totem", "Totem", "Theme", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFFFFD24A));

    private static final int GOLD = 0xFFFFD24A, GREEN = 0xFF7CFF8E;
    private static final ParticleSystem.Physics SPARK = new ParticleSystem.Physics(true, 4f, 0.4f, 0.3f);
    private static final ParticleSystem.Physics FLOAT = new ParticleSystem.Physics(false, 0f, 0.6f, 0f);
    private static ItemStack totem;

    private final Map<Integer, Integer> pops = new HashMap<>();
    private final List<Pop> recent = new ArrayList<>();
    private final ParticleSystem sparks = new ParticleSystem(400);
    private final ParticleSystem motes = new ParticleSystem(200);
    private final Render3D.TexBuf beamBuf = new Render3D.TexBuf();

    private static final class Pop {
        Entity entity;
        Vec3 at;
        long born;
        int count;
    }

    public TotemPop() {
        super("TotemPop", "Счётчик тотемов над игроками и своя анимация срабатывания", Category.COMBAT);
        counterScale.visibleWhen(counter::isEnabled);
        customColor.visibleWhen(() -> color.is("Custom"));
    }

    @Override
    protected void onDisable() {
        pops.clear();
        recent.clear();
        sparks.clear();
        motes.clear();
    }

    public static boolean hideVanilla(Entity e) {
        TotemPop m = Modules.enabled(TotemPop.class);
        return m != null && m.hideVanilla.isEnabled() && m.counts(e);
    }

    public static void onPop(Entity e) {
        TotemPop m = Modules.enabled(TotemPop.class);
        if (m != null) m.pop(e);
    }

    private boolean counts(Entity e) {
        if (!(e instanceof LivingEntity)) return false;
        if (e == mc.player) return self.isEnabled();
        return !playersOnly.isEnabled() || e instanceof Player;
    }

    private void pop(Entity e) {
        if (!counts(e)) return;
        int n = pops.merge(e.getId(), 1, Integer::sum);
        Pop p = new Pop();
        p.entity = e;
        p.at = e.position();
        p.born = System.currentTimeMillis();
        p.count = n;
        recent.add(p);
        if (recent.size() > 8) recent.remove(0);
        if (burst.isEnabled()) spawnBurst(p);
        if (notify.isEnabled() && e != mc.player) {
            Notifications.show(I18n.tr(n == 1 ? "%s: тотем сработал" : "%s: тотем сработал (%d)", e.getName().getString(), n), baseColor());
        }
    }

    private int baseColor() {
        return switch (color.get()) {
            case "Theme" -> Theme.get().accentA();
            case "Custom" -> customColor.getArgb();
            default -> GOLD;
        };
    }

    private int secondColor() {
        return switch (color.get()) {
            case "Theme" -> Theme.get().accentB();
            case "Custom" -> ColorUtils.lerp(customColor.getArgb(), 0xFFFFFFFF, 0.4f);
            default -> GREEN;
        };
    }

    @Override
    public void onTick() {
        if (mc.level == null) {
            pops.clear();
            recent.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> it = pops.entrySet().iterator();
        while (it.hasNext()) {
            Entity e = mc.level.getEntity(it.next().getKey());
            if (e == null || !e.isAlive() || (e instanceof LivingEntity le && le.isDeadOrDying())) it.remove();
        }
    }

    private void spawnBurst(Pop p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int c1 = baseColor(), c2 = secondColor();
        double h = p.entity.getBbHeight();
        for (int i = 0; i < 36; i++) {
            ParticleSystem.Particle s = new ParticleSystem.Particle();
            double a = i / 36.0 * Math.PI * 4, rad = 0.55 + r.nextDouble(0, 0.15);
            s.x = p.at.x + Math.cos(a) * rad;
            s.y = p.at.y + 0.1 + i / 36.0 * h;
            s.z = p.at.z + Math.sin(a) * rad;
            s.vx = -Math.sin(a) * 1.2;
            s.vz = Math.cos(a) * 1.2;
            s.vy = r.nextDouble(0.8, 1.6);
            s.size = (float) r.nextDouble(0.05, 0.09);
            s.lifeMs = (int) r.nextDouble(700, 1200);
            s.delayMs = i * 12;
            s.color = ColorUtils.lerp(c1, c2, (float) (i / 36.0));
            s.texture = r.nextBoolean() ? ParticleTexture.STAR : ParticleTexture.SPARKLE;
            s.rotationSpeed = (float) r.nextDouble(-6, 6);
            s.tailTime = 0.06f;
            motes.add(s);
        }
        for (int i = 0; i < 26; i++) {
            ParticleSystem.Particle s = new ParticleSystem.Particle();
            s.x = p.at.x; s.y = p.at.y + h * 0.6; s.z = p.at.z;
            double th = r.nextDouble(Math.PI * 2), up = r.nextDouble(0.2, 1.1), v = r.nextDouble(2.5, 5);
            s.vx = Math.cos(th) * v * 0.7; s.vy = up * v; s.vz = Math.sin(th) * v * 0.7;
            s.size = (float) r.nextDouble(0.05, 0.1);
            s.lifeMs = (int) r.nextDouble(700, 1300);
            s.color = ColorUtils.lerp(c1, 0xFFFFFFFF, (float) r.nextDouble(0, 0.4));
            s.texture = ParticleTexture.SPARKLE;
            s.tailTime = 0.05f;
            sparks.add(s);
        }
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        long now = System.currentTimeMillis();
        recent.removeIf(p -> now - p.born > 2000);
        beamBuf.clear();
        int c1 = baseColor(), c2 = secondColor();
        for (Pop p : recent) {
            float age = (now - p.born) / 1000f;
            if (!burst.isEnabled()) continue;
            for (int w = 0; w < 2; w++) {
                float k = (age - w * 0.14f) / 0.65f;
                if (k <= 0 || k >= 1) continue;
                float e = 1f - (1f - k) * (1f - k) * (1f - k);
                float radius = 0.3f + e * 2.2f;
                float a = (1f - k) * (w == 0 ? 1f : 0.6f);
                List<Vec3> loop = new ArrayList<>(49);
                for (int i = 0; i <= 48; i++) {
                    double ang = Math.PI * 2 * i / 48;
                    loop.add(new Vec3(p.at.x + Math.cos(ang) * radius, p.at.y + 0.05, p.at.z + Math.sin(ang) * radius));
                }
                int col = w == 0 ? c1 : c2;
                Render3D.beam(ms, beamBuf, loop, true, Render3D.FloatFn.of(0.05f + 0.14f * (1f - k)), t -> ColorUtils.multAlpha(col, a));
            }
            if (age < 0.8f) {
                float k = age / 0.8f;
                float a = k < 0.15f ? k / 0.15f : 1f - (k - 0.15f) / 0.85f;
                List<Vec3> pillar = List.of(p.at, p.at.add(0, p.entity.getBbHeight() + 1.2, 0));
                Render3D.beam(ms, beamBuf, pillar, false, t -> (0.7f - 0.4f * t), t -> ColorUtils.multAlpha(ColorUtils.lerp(c1, c2, t), a * (1f - t) * 0.7f));
            }
        }
        if (!beamBuf.isEmpty()) Render3D.drawTextured(Render3D.FX_BEAM, beamBuf);
        sparks.render(ms, camera, SPARK, true);
        motes.render(ms, camera, FLOAT, true);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float td) {
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        if (counter.isEnabled()) {
            for (Map.Entry<Integer, Integer> en : pops.entrySet()) {
                Entity e = mc.level.getEntity(en.getKey());
                if (e == null || e == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                drawCounter(ctx, e, en.getValue(), td);
            }
        }
        if (popup.isEnabled()) {
            for (Pop p : recent) drawPopup(ctx, p, (now - p.born) / 1000f, td);
        }
    }

    private void drawCounter(GuiGraphicsExtractor ctx, Entity e, int n, float td) {
        Vec3 pos = e.getPosition(td).add(0, e.getBbHeight() + 0.75, 0);
        Vector3f sc = WorldToScreen.project(pos);
        if (sc == null) return;
        float k = Mth.clamp(6f / Math.max(1f, sc.z()), 0.5f, 1.3f) * counterScale.getFloat();
        String text = "×" + n;
        float tw = Fonts.width(text, Fonts.BOLD);
        float w = 16 + 3 + tw;
        Matrix3x2fStack ms = ctx.pose();
        ms.pushMatrix();
        ms.translate(sc.x(), sc.y());
        ms.scale(k, k);
        ms.translate(-w / 2f, -8);
        if (totem == null) totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        ctx.item(totem, 0, 0);
        Fonts.draw(ctx, text, 19 + 1, 5 + 1, ColorUtils.withAlpha(0xFF000000, 150), false, Fonts.BOLD);
        Fonts.draw(ctx, text, 19, 5, n >= 3 ? 0xFFFF6B6B : baseColor(), false, Fonts.BOLD);
        ms.popMatrix();
    }

    private void drawPopup(GuiGraphicsExtractor ctx, Pop p, float age, float td) {
        float life = 1.4f;
        if (age > life) return;
        Vec3 base = (p.entity.isAlive() ? p.entity.getPosition(td) : p.at).add(0, p.entity.getBbHeight() * 0.7 + age * 0.6, 0);
        Vector3f sc = WorldToScreen.project(base);
        if (sc == null) return;
        float k = Mth.clamp(6f / Math.max(1f, sc.z()), 0.5f, 1.3f);
        float in = Mth.clamp(age / 0.15f, 0f, 1f), out = Mth.clamp((life - age) / 0.4f, 0f, 1f);
        float pop = age < 0.15f ? 0.6f + 0.6f * (age / 0.15f) : 1.2f - 0.2f * Mth.clamp((age - 0.15f) / 0.2f, 0f, 1f);
        String text = p.count > 1 ? "POP ×" + p.count : "POP";
        float size = 18f * k * pop;
        float tw = Fonts.logoWidth(text, size);
        int col = ColorUtils.multAlpha(p.count >= 3 ? 0xFFFF6B6B : baseColor(), in * out);
        Fonts.drawLogo(ctx, text, sc.x() - tw / 2f, sc.y(), size, col, ColorUtils.multAlpha(secondColor(), in * out));
    }
}
