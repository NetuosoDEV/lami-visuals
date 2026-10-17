package ru.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import ru.client.core.ClientMod;
import ru.client.modules.impl.utility.ClickGui;

public final class UiSounds {
    private UiSounds() {}

    public enum Sfx {
        BUTTON_CLICK("button_click"),
        MENU_OPEN("menu_open"),
        MENU_CLOSE("menu_close"),
        TAB_SWITCH("tab_switch"),
        TOGGLE_ON("toggle_on"),
        TOGGLE_OFF("toggle_off"),
        SLIDER_TICK("slider_tick"),
        SLIDER_LOOP("slider_loop");

        final SoundEvent event;

        Sfx(String id) {
            event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "ui." + id));
        }
    }

    private static final Minecraft mc = Minecraft.getInstance();

    private static boolean enabled() {
        ClickGui g = ClickGui.get();
        return g == null || g.sounds.isEnabled();
    }

    private static float volume() {
        ClickGui g = ClickGui.get();
        return g == null ? 0.7f : g.volume.getFloat() / 100f;
    }

    public static void play(Sfx s) { play(s, 1f); }

    public static void play(Sfx s, float pitch) {
        if (!enabled() || volume() <= 0.001f) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(s.event, pitch, volume()));
    }

    public static void click() { play(Sfx.BUTTON_CLICK); }

    public static void dropdown(boolean open) { play(Sfx.BUTTON_CLICK, open ? 1.18f : 0.86f); }

    public static void toggle(boolean on) { play(on ? Sfx.TOGGLE_ON : Sfx.TOGGLE_OFF); }

    private static SliderLoop loop;
    private static long lastTickAt, lastMoveAt;

    public static void sliderGrab() {
        if (!enabled()) return;
        if (loop == null || loop.isStopped()) {
            loop = new SliderLoop();
            mc.getSoundManager().play(loop);
        }
    }

    public static void sliderStep(float pct) {
        long now = System.currentTimeMillis();
        lastMoveAt = now;
        if (now - lastTickAt < 45) return;
        lastTickAt = now;
        play(Sfx.SLIDER_TICK, 0.85f + 0.35f * Math.max(0f, Math.min(1f, pct)));
    }

    public static void sliderRelease() {
        if (loop != null) loop.release();
        loop = null;
    }

    private static final class SliderLoop extends AbstractTickableSoundInstance {
        private boolean released;

        SliderLoop() {
            super(Sfx.SLIDER_LOOP.event, SoundSource.UI, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = Attenuation.NONE;
            volume = 0.0001f;
        }

        void release() { released = true; }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            boolean moving = !released && System.currentTimeMillis() - lastMoveAt < 150;
            float target = moving ? volume() * 0.55f : 0f;
            volume += (target - volume) * 0.4f;
            if (released && volume < 0.01f) stop();
        }
    }
}
