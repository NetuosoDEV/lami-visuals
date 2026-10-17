package ru.client.modules.impl.combat;

import ru.client.core.CustomSound;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class HitSound extends Module {

    private final ModeSetting sound = register(new ModeSetting("Sound", "Bonk",
            CustomSound.withTitles("Click", "Bell", "Orb", "Pling", "Bass", "Crit")));
    private final NumberSetting volume = register(new NumberSetting("Volume", 0.7, 0.1, 1.0, 0.05));
    private final NumberSetting pitch = register(new NumberSetting("Pitch", 1.0, 0.5, 2.0, 0.05));
    private final BooleanSetting randomPitch = register(new BooleanSetting("Random Pitch", true));
    private final BooleanSetting comboPitch = register(new BooleanSetting("Combo Pitch", true));
    private final NumberSetting pitchStep = register(new NumberSetting("Pitch Step", 0.05, 0.01, 0.15, 0.01));
    private final NumberSetting comboWindow = register(new NumberSetting("Combo Window", 1.5, 0.5, 4.0, 0.1));

    private int combo;
    private int lastTargetId = Integer.MIN_VALUE;
    private long lastHitAt;

    public HitSound() {
        super("HitSound", "Кастомный звук при ударе", Category.COMBAT);
        pitchStep.visibleWhen(comboPitch::isEnabled);
        comboWindow.visibleWhen(comboPitch::isEnabled);
    }

    private SoundEvent event() {
        CustomSound custom = CustomSound.byTitle(sound.get());
        if (custom != null) return custom.event;
        return switch (sound.get()) {
            case "Bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "Orb" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "Pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
            case "Bass" -> SoundEvents.NOTE_BLOCK_BASS.value();
            case "Crit" -> SoundEvents.PLAYER_ATTACK_CRIT;
            default -> SoundEvents.UI_BUTTON_CLICK.value();
        };
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        float p = pitch.getFloat();
        long now = System.currentTimeMillis();
        boolean same = target.getId() == lastTargetId && now - lastHitAt < comboWindow.get() * 1000;
        combo = same ? combo + 1 : 0;
        lastTargetId = target.getId();
        lastHitAt = now;
        if (comboPitch.isEnabled()) p *= 1f + pitchStep.getFloat() * Math.min(combo, 12);
        float spread = comboPitch.isEnabled() ? 0.06f : 0.2f;
        if (randomPitch.isEnabled()) p *= 1f - spread / 2 + ThreadLocalRandom.current().nextFloat() * spread;
        p = Math.min(p, 2f);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(event(), p, volume.getFloat()));
    }
}
