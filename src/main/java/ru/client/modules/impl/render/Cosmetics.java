package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.CosmeticWings;
import ru.client.utils.render.Render3D;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class Cosmetics extends Module {

    private final BooleanSetting halo = register(new BooleanSetting("Halo", true));
    private final ModeSetting wings = register(new ModeSetting("Wings", "Angel", "Off", "Angel", "Bone", "Demon"));
    private final NumberSetting wingSize = register(new NumberSetting("Wing Size", 1.0, 0.5, 1.6, 0.05));
    private final NumberSetting wingHeight = register(new NumberSetting("Wing Height", 0.0, -0.5, 0.5, 0.02));
    private final ModeSetting cape = register(new ModeSetting("Cape", "Lami", "Off", "Lami", "Aurora", "Galaxy", "Flame", "Ice"));
    private final BooleanSetting aura = register(new BooleanSetting("Aura", false));
    private final ModeSetting auraTexture = register(new ModeSetting("Aura Texture", "Sparkle", ParticleTexture.modes()));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFF8A5CFF));
    private final BooleanSetting firstPerson = register(new BooleanSetting("First Person Aura", false));

    private static final ParticleSystem.Physics FLOAT = new ParticleSystem.Physics(false, 0f, 0.8f, 0f);

    private final Render3D.TexBuf beamBuf = new Render3D.TexBuf();
    private final ParticleSystem particles = new ParticleSystem(400);
    private long lastNs = System.nanoTime();
    private double auraDebt;

    private float wingPhase, wingFreq = 1.6f, wingSpread = 32f, wingSpreadAmp = 5f, wingLift, wingLiftAmp = 3f;
    private long wingNs;

    public Cosmetics() {
        super("Cosmetics", "Нимб, крылья, плащ и аура — видишь только ты", Category.RENDER);
        wingSize.visibleWhen(() -> !wings.is("Off"));
        wingHeight.visibleWhen(() -> !wings.is("Off"));
        auraTexture.visibleWhen(aura::isEnabled);
        firstPerson.visibleWhen(aura::isEnabled);
        customColor.visibleWhen(() -> color.is("Custom"));
    }

    @Override
    protected void onDisable() { particles.clear(); }

    @Override
    public void onTick() {
        CosmeticWings.Type type = wingType();
        if (type != null) CosmeticWings.preload(type);
    }

    private CosmeticWings.Type wingType() {
        return switch (wings.get()) {
            case "Angel" -> CosmeticWings.Type.ANGEL;
            case "Bone" -> CosmeticWings.Type.BONE;
            case "Demon" -> CosmeticWings.Type.DEMON;
            default -> null;
        };
    }

    public static CosmeticWings.Pose wingPose(AvatarRenderState s) {
        Cosmetics c = Modules.enabled(Cosmetics.class);
        LocalPlayer p = mc.player;
        if (c == null || p == null || s.id != p.getId()) return null;
        CosmeticWings.Type type = c.wingType();
        if (type == null || s.isInvisible || s.isSpectator || s.pose == Pose.SLEEPING || s.chestEquipment.is(Items.ELYTRA)) return null;
        return c.animateWings(type, s, p);
    }

    private CosmeticWings.Pose animateWings(CosmeticWings.Type type, AvatarRenderState s, LocalPlayer p) {
        long now = System.nanoTime();
        float dt = wingNs == 0 ? 0f : Math.min((now - wingNs) / 1e9f, 0.1f);
        wingNs = now;
        float speed = (float) p.getDeltaMovement().horizontalDistance();
        boolean air = !p.onGround() && !p.isInWater() && !p.isPassenger();
        float f, sp, spA, li, liA;
        if (s.isVisuallySwimming) { f = 2.0f; sp = 58; spA = 4; li = -4; liA = 3; }
        else if (air) { f = 7.5f; sp = 20; spA = 10; li = 8; liA = 22; }
        else if (speed > 0.2f) { f = 4.4f; sp = 34; spA = 10; li = 4; liA = 7; }
        else if (speed > 0.03f) { f = 3.2f; sp = 30; spA = 8; li = 2; liA = 5; }
        else { f = 1.6f; sp = 32; spA = 5; li = 0; liA = 3; }
        if (s.isCrouching) sp += 14;
        float k = 1f - (float) Math.exp(-dt * 5f);
        wingFreq += (f - wingFreq) * k;
        wingSpread += (sp - wingSpread) * k;
        wingSpreadAmp += (spA - wingSpreadAmp) * k;
        wingLift += (li - wingLift) * k;
        wingLiftAmp += (liA - wingLiftAmp) * k;
        wingPhase = (wingPhase + dt * wingFreq) % (float) (Math.PI * 2000);
        float spread = wingSpread + wingSpreadAmp * Mth.sin(wingPhase + 1.1f);
        float lift = wingLift + wingLiftAmp * Mth.sin(wingPhase);
        return new CosmeticWings.Pose(type, (float) Math.toRadians(spread), (float) Math.toRadians(lift), wingSize.getFloat(), wingHeight.getFloat());
    }

    public static ClientAsset.Texture capeTexture() {
        Cosmetics c = Modules.enabled(Cosmetics.class);
        if (c == null || c.cape.is("Off")) return null;
        String name = c.cape.get().toLowerCase(Locale.ROOT);
        Identifier id = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "cape/" + name);
        return new ClientAsset.ResourceTexture(id, Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "textures/cape/" + name + ".png"));
    }

    private int colorA() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(4000, 0, 0.6f, 1f, 255);
            case "Custom" -> customColor.getArgb();
            default -> Theme.get().accentA();
        };
    }

    private int colorB() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(4000, 1500, 0.6f, 1f, 255);
            case "Custom" -> ColorUtils.lerp(customColor.getArgb(), 0xFFFFFFFF, 0.4f);
            default -> Theme.get().accentB();
        };
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        long nowNs = System.nanoTime();
        double dt = Math.min((nowNs - lastNs) / 1e9, 0.1);
        lastNs = nowNs;
        if (nullCheck()) return;
        LocalPlayer p = mc.player;
        boolean fp = mc.options.getCameraType().isFirstPerson();
        boolean upright = !p.isFallFlying() && !p.isVisuallySwimming() && !p.isSleeping() && !p.isInvisible();
        Vec3 pos = p.getPosition(td);
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        int cA = colorA(), cB = colorB();
        beamBuf.clear();

        if (!fp && upright && halo.isEnabled()) halo(ms, p, pos, time, cA, cB);
        if (aura.isEnabled() && !p.isInvisible() && (!fp || firstPerson.isEnabled())) spawnAura(p, pos, dt, cA, cB);
        if (!beamBuf.isEmpty()) Render3D.drawTextured(Render3D.FX_BEAM, beamBuf);
        particles.render(ms, camera, FLOAT, true);
    }

    private void halo(PoseStack ms, LocalPlayer p, Vec3 pos, float time, int cA, int cB) {
        double y = pos.y + p.getBbHeight() + 0.2 + Math.sin(time * 2.2) * 0.03;
        float r = 0.27f;
        List<Vec3> loop = new ArrayList<>(49);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2 * i / 48 + time * 0.6;
            loop.add(new Vec3(pos.x + Math.cos(a) * r, y, pos.z + Math.sin(a) * r));
        }
        Render3D.beam(ms, beamBuf, loop, true, Render3D.FloatFn.of(0.22f),
                t -> ColorUtils.multAlpha(ColorUtils.lerp(cA, cB, (float) Math.abs(Math.sin(t * Math.PI))), 0.35f));
        Render3D.beam(ms, beamBuf, loop, true, Render3D.FloatFn.of(0.07f),
                t -> ColorUtils.lerp(ColorUtils.lerp(cA, cB, (float) Math.abs(Math.sin(t * Math.PI))), 0xFFFFFFFF, 0.35f));
    }

    private void spawnAura(LocalPlayer p, Vec3 pos, double dt, int cA, int cB) {
        auraDebt += 14 * dt;
        int n = (int) auraDebt;
        auraDebt -= n;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            ParticleSystem.Particle q = new ParticleSystem.Particle();
            double a = r.nextDouble(Math.PI * 2), d = r.nextDouble(0.45, 0.7);
            q.x = pos.x + Math.cos(a) * d;
            q.y = pos.y + r.nextDouble(0, p.getBbHeight());
            q.z = pos.z + Math.sin(a) * d;
            q.vy = r.nextDouble(0.25, 0.6);
            q.swayAmp = 0.15f;
            q.swayFreq = (float) r.nextDouble(1, 2.5);
            q.swayPhase = (float) r.nextDouble(Math.PI * 2);
            q.size = (float) r.nextDouble(0.035, 0.06);
            q.rotationSpeed = (float) r.nextDouble(-3, 3);
            q.lifeMs = (int) r.nextDouble(1100, 1800);
            q.fadeInMs = 250;
            q.color = ColorUtils.lerp(cA, cB, (float) r.nextDouble());
            q.texture = ParticleTexture.pick(auraTexture.get());
            particles.add(q);
        }
    }
}
