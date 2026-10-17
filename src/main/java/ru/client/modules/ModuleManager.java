package ru.client.modules;

import ru.client.core.SessionTracker;

import ru.client.modules.impl.combat.HitMarker;
import ru.client.modules.impl.combat.HitSound;
import ru.client.modules.impl.combat.HitFx;
import ru.client.modules.impl.combat.KillStreak;
import ru.client.modules.impl.combat.TotemPop;
import ru.client.modules.impl.render.ChunkAnimator;
import ru.client.modules.impl.render.FreeLook;
import ru.client.modules.impl.render.Shaders;
import ru.client.modules.impl.render.Hitboxes;
import ru.client.modules.impl.render.Ambience;
import ru.client.modules.impl.render.Cosmetics;
import ru.client.modules.impl.render.GlintColor;
import ru.client.modules.impl.render.ParticleRain;
import ru.client.modules.impl.hud.FoodHud;
import ru.client.modules.impl.hud.Keystrokes;
import ru.client.modules.impl.hud.SessionStats;
import ru.client.modules.impl.hud.ChatTab;
import ru.client.modules.impl.hud.ShulkerPreview;
import ru.client.modules.impl.utility.MainMenu;
import com.mojang.blaze3d.platform.InputConstants;
import ru.client.core.ClientMod;
import ru.client.modules.impl.combat.DamageNumbers;
import ru.client.modules.impl.combat.HitCounter;
import ru.client.modules.impl.combat.HitParticles;
import ru.client.modules.impl.combat.KillEffect;
import ru.client.modules.impl.combat.TargetEsp;
import ru.client.modules.impl.combat.TargetHud;
import ru.client.modules.impl.hud.ArmorHud;
import ru.client.modules.impl.hud.ArrayListHud;
import ru.client.modules.impl.hud.Cooldowns;
import ru.client.modules.impl.hud.InventoryHud;
import ru.client.modules.impl.hud.LowHp;
import ru.client.modules.impl.hud.Notifications;
import ru.client.modules.impl.hud.Potions;
import ru.client.modules.impl.hud.Watermark;
import ru.client.modules.impl.render.BlockOverlay;
import ru.client.modules.impl.render.CameraTweaks;
import ru.client.modules.impl.render.ChinaHat;
import ru.client.modules.impl.render.Crosshair;
import ru.client.modules.impl.render.JumpCircles;
import ru.client.modules.impl.render.NoRender;
import ru.client.modules.impl.render.SwingAnimations;
import ru.client.modules.impl.render.Trails;
import ru.client.modules.impl.render.Trajectories;
import ru.client.modules.impl.render.ViewModel;
import ru.client.modules.impl.render.Zoom;
import ru.client.modules.impl.utility.ClickGui;
import ru.client.modules.impl.utility.FakePlayer;
import ru.client.modules.impl.utility.Screens;
import ru.client.ui.theme.Theme;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ModuleManager {

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();
    private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);

    public ModuleManager() {
        for (Category c : Category.values()) byCategory.put(c, new ArrayList<>());
    }

    public void init() {
        register(new Theme());
        register(new TargetHud());
        register(new TargetEsp());
        register(new HitParticles());
        register(new DamageNumbers());
        register(new HitCounter());
        register(new KillEffect());
        register(new HitMarker());
        register(new HitSound());
        register(new HitFx());
        register(new KillStreak());
        register(new TotemPop());
        register(new Trajectories());
        register(new JumpCircles());
        register(new ParticleRain());
        register(new ChinaHat());
        register(new Trails());
        register(new BlockOverlay());
        register(new Crosshair());
        register(new ViewModel());
        register(new SwingAnimations());
        register(new CameraTweaks());
        register(new Zoom());
        register(new FreeLook());
        register(new NoRender());
        register(new Hitboxes());
        register(new Ambience());
        register(new Cosmetics());
        register(new GlintColor());
        register(new Shaders());
        register(new ChunkAnimator());
        register(new Watermark());
        register(new ArrayListHud());
        register(new Notifications());
        register(new Potions());
        register(new ArmorHud());
        register(new Cooldowns());
        register(new InventoryHud());
        register(new LowHp());
        register(new FoodHud());
        register(new Keystrokes());
        register(new SessionStats());
        register(new ShulkerPreview());
        register(new ChatTab());
        register(new ClickGui());
        register(new MainMenu());
        register(new FakePlayer());
        register(new Screens());

        Module.setToggleListener(m -> {
            if (m.isHidden() || Minecraft.getInstance().player == null) return;
            Notifications n = get(Notifications.class);
            if (n != null && n.isToggled() && n != m) n.push(m);
        });
    }

    private void register(Module module) {
        if (byClass.containsKey(module.getClass()))
            throw new IllegalStateException("Duplicate module: " + module.getName());
        modules.add(module);
        byClass.put(module.getClass(), module);
        byCategory.get(module.getCategory()).add(module);
    }

    public void onKey(int key) {
        if (key == -1) return;
        for (Module m : modules) {
            if (m.getKey() == key) m.toggle();
        }
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onKeyPress(key); } catch (Exception e) { fail(m, "key", e); }
        }
    }

    public void onTick() {
        SessionTracker.tick();
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onTick(); } catch (Exception e) { fail(m, "tick", e); }
        }
    }

    public void onRender2D(GuiGraphicsExtractor context, float tickDelta) {
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onRender2D(context, tickDelta); } catch (Exception e) { fail(m, "render2D", e); }
        }
    }

    public void onRender3D(PoseStack matrices, Camera camera, float tickDelta) {
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onRender3D(matrices, camera, tickDelta); } catch (Exception e) { fail(m, "render3D", e); }
        }
    }

    public void onAttack(Entity target) {
        SessionTracker.onAttack(target);
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onAttack(target); } catch (Exception e) { fail(m, "attack", e); }
        }
    }

    public void onKill(LivingEntity target) {
        SessionTracker.onKill(target);
        for (Module m : modules) {
            if (!m.isToggled()) continue;
            try { m.onKill(target); } catch (Exception e) { fail(m, "kill", e); }
        }
    }

    private void fail(Module m, String stage, Exception e) {
        ClientMod.LOGGER.error("Module {} crashed in {}, disabling", m.getName(), stage, e);
        m.setToggled(false);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) { return (T) byClass.get(clazz); }

    public Optional<Module> get(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }

    public List<Module> getModules() { return Collections.unmodifiableList(modules); }

    public List<Module> getModules(Category category) {
        return Collections.unmodifiableList(byCategory.get(category));
    }
}
