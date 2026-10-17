package ru.client.utils.render.particles;

import net.minecraft.resources.Identifier;
import ru.client.core.ClientMod;

import java.util.concurrent.ThreadLocalRandom;

public enum JumpTexture {
    GEAR("Gear", "gear"),
    SUN("Sun", "sun"),
    FLOWER("Flower", "flower"),
    MANDALA("Mandala", "mandala"),
    WHEEL("Wheel", "wheel"),
    HEXAGON("Hexagon", "hexagon"),
    COMPASS("Compass", "compass"),
    TICKS("Ticks", "ticks"),
    DOUBLE_RING("Double Ring", "double_ring"),
    DASHED("Dashed", "dashed"),
    VORTEX("Vortex", "vortex"),
    SHURIKEN("Shuriken", "shuriken"),
    DOTS("Dots", "dots"),
    STAR_RING("Star Ring", "star_ring"),
    ATOM("Atom", "atom");

    public static final String CLASSIC = "Classic";
    public static final String RANDOM = "Random";
    public static final float RADIUS_FRACTION = 0.84f;

    public final String title;
    public final Identifier texture;

    JumpTexture(String title, String file) {
        this.title = title;
        this.texture = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "textures/jumpcircle/" + file + ".png");
    }

    public static String[] modes() {
        JumpTexture[] all = values();
        String[] out = new String[all.length + 2];
        out[0] = CLASSIC;
        for (int i = 0; i < all.length; i++) out[i + 1] = all[i].title;
        out[all.length + 1] = RANDOM;
        return out;
    }

    public static String[] titles() {
        JumpTexture[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].title;
        return out;
    }

    public static JumpTexture pick(String mode) {
        if (CLASSIC.equalsIgnoreCase(mode)) return null;
        if (RANDOM.equalsIgnoreCase(mode)) return values()[ThreadLocalRandom.current().nextInt(values().length)];
        for (JumpTexture t : values()) if (t.title.equalsIgnoreCase(mode)) return t;
        return GEAR;
    }
}
