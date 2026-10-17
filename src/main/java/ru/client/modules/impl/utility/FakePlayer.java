package ru.client.modules.impl.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import ru.client.core.CombatTracker;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;

import java.util.UUID;

public final class FakePlayer extends Module {

    public static final int ENTITY_ID = -1_337_000;

    private final BooleanSetting copySkin = register(new BooleanSetting("Copy Skin", true));
    private final BooleanSetting copyArmor = register(new BooleanSetting("Copy Armor", true));
    private final NumberSetting maxHealth = register(new NumberSetting("Health", 20, 1, 100, 1));
    private final BooleanSetting respawn = register(new BooleanSetting("Respawn", true));
    private final BooleanSetting sounds = register(new BooleanSetting("Hurt Sounds", true));

    private FakePlayerEntity fake;
    private long lastHitAt, diedAt;

    public FakePlayer() {
        super("FakePlayer", "Клиентский манекен для теста боевого визуала", Category.UTILITY);
    }

    public static boolean isFake(Entity e) { return e instanceof FakePlayerEntity; }

    @Override
    protected void onEnable() {
        if (!nullCheck()) spawn();
    }

    @Override
    protected void onDisable() { despawn(); }

    @Override
    public void onTick() {
        if (nullCheck()) { fake = null; return; }
        if (fake == null || fake.level() != mc.level || fake.isRemoved()) { spawn(); return; }
        if (fake.getHealth() <= 0 && respawn.isEnabled() && diedAt > 0 && System.currentTimeMillis() - diedAt > 1500) {
            fake.setHealth(fake.getMaxHealth());
            fake.deathTime = 0;
            diedAt = 0;
        }
    }

    private void spawn() {
        despawn();
        LocalPlayer self = mc.player;
        FakePlayerEntity f = new FakePlayerEntity(mc.level, new GameProfile(UUID.randomUUID(), "FakePlayer"), copySkin.isEnabled());
        f.setId(ENTITY_ID);
        f.copyPosition(self);
        f.setYHeadRot(self.getYHeadRot());
        f.setYBodyRot(self.yBodyRot);
        f.setOldPosAndRot();
        AttributeInstance hp = f.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(maxHealth.get());
        f.setHealth(maxHealth.getFloat());
        if (copyArmor.isEnabled()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = self.getItemBySlot(slot);
                if (!stack.isEmpty()) f.setItemSlot(slot, stack.copy());
            }
        }
        mc.level.addEntity(f);
        fake = f;
        diedAt = 0;
    }

    private void despawn() {
        if (fake != null && mc.level != null && fake.level() == mc.level) {
            mc.level.removeEntity(ENTITY_ID, Entity.RemovalReason.DISCARDED);
        }
        fake = null;
    }

    public static void onHit(LocalPlayer player, FakePlayerEntity target) {
        FakePlayer m = Modules.get(FakePlayer.class);
        player.resetAttackStrengthTicker();
        if (m == null || target.getHealth() <= 0) return;
        long now = System.currentTimeMillis();
        if (now - m.lastHitAt < 500) return;
        m.lastHitAt = now;
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float charge = player.getAttackStrengthScale(0.5f);
        float damage = base * (0.2f + charge * charge * 0.8f);
        if (CombatTracker.isRecentCrit(target, 50)) {
            damage *= 1.5f;
            player.crit(target);
        }
        target.setHealth(Math.max(0f, target.getHealth() - damage));
        target.animateHurt(player.getYRot());
        boolean dead = target.getHealth() <= 0;
        if (dead) m.diedAt = now;
        if (m.sounds.isEnabled()) {
            mc.level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                    dead ? SoundEvents.PLAYER_DEATH : SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 1f, false);
        }
    }
}
