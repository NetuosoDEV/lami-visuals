package ru.client.modules.impl.combat;

import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.particles.ParticleSystem;
import ru.client.utils.render.particles.ParticleTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class HitParticles extends Module {

    private final ModeSetting texture = register(new ModeSetting("Texture", "Star", ParticleTexture.modes()));
    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFFFFD36B));
    private final NumberSetting count = register(new NumberSetting("Count", 10, 2, 40, 1));
    private final NumberSetting size = register(new NumberSetting("Size", 0.12, 0.04, 0.4, 0.01));
    private final NumberSetting life = register(new NumberSetting("Lifetime", 1.2, 0.3, 4, 0.1));
    private final NumberSetting speed = register(new NumberSetting("Speed", 3.0, 0.5, 8, 0.1));
    private final BooleanSetting physics = register(new BooleanSetting("Physics", true));
    private final NumberSetting gravity = register(new NumberSetting("Gravity", 9.8, 1, 20, 0.2));
    private final NumberSetting bounce = register(new NumberSetting("Bounce", 0.45, 0, 0.9, 0.05));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", true));
    private final BooleanSetting critBoost = register(new BooleanSetting("Crit x2", true));

    private final ParticleSystem system = new ParticleSystem(600);

    public HitParticles() {
        super("HitParticles", "Частицы при ударе", Category.COMBAT);
        customColor.visibleWhen(() -> color.is("Custom"));
        gravity.visibleWhen(physics::isEnabled);
        bounce.visibleWhen(physics::isEnabled);
    }

    @Override
    protected void onDisable() { system.clear(); }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        int n = count.getInt() * (critBoost.isEnabled() && CombatTracker.isRecentCrit(target, 50) ? 2 : 1);
        spawn(target, n);
    }

    public void spawn(Entity target, int n) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        AABB box = target.getBoundingBox();
        Theme theme = Theme.get();
        for (int i = 0; i < n; i++) {
            ParticleSystem.Particle p = new ParticleSystem.Particle();
            p.x = r.nextDouble(box.minX, box.maxX);
            p.y = r.nextDouble(box.minY + (box.maxY - box.minY) * 0.3, box.maxY);
            p.z = r.nextDouble(box.minZ, box.maxZ);
            double theta = r.nextDouble(Math.PI * 2), phi = r.nextDouble(-0.3, 1.2);
            double v = speed.get() * r.nextDouble(0.5, 1.0);
            p.vx = Math.cos(theta) * Math.cos(phi) * v;
            p.vy = Math.sin(phi) * v;
            p.vz = Math.sin(theta) * Math.cos(phi) * v;
            p.size = (float) (size.get() * r.nextDouble(0.7, 1.2));
            p.rotation = (float) r.nextDouble(Math.PI * 2);
            p.rotationSpeed = (float) r.nextDouble(-6, 6);
            p.lifeMs = (int) (life.get() * 1000 * r.nextDouble(0.8, 1.1));
            p.color = switch (color.get()) {
                case "Rainbow" -> ColorUtils.rainbow(3000, r.nextInt(3000), 0.7f, 1f, 255);
                case "Custom" -> customColor.getArgb();
                default -> ColorUtils.lerp(theme.accentA(), theme.accentB(), (float) r.nextDouble());
            };
            p.texture = ParticleTexture.pick(texture.get());
            system.add(p);
        }
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        boolean ph = physics.isEnabled();
        system.render(ms, camera, new ParticleSystem.Physics(ph, gravity.getFloat(), ph ? 0.55f : 0.25f, bounce.getFloat()), glow.isEnabled());
    }
}
