package ru.client.utils.render.particles;

import net.minecraft.resources.Identifier;
import ru.client.core.ClientMod;

import java.util.concurrent.ThreadLocalRandom;

public enum ParticleTexture {
    DOLLAR("Dollar", "dollar"),
    STAR("Star", "star"),
    HEART("Heart", "heart"),
    SPARKLE("Sparkle", "sparkle"),
    DIAMOND("Diamond", "diamond"),
    ORB("Orb", "orb"),
    RING("Ring", "ring"),
    SNOWFLAKE("Snowflake", "snowflake"),
    BOLT("Bolt", "bolt"),
    CROWN("Crown", "crown"),
    NOTE("Note", "note"),
    MOON("Moon", "moon"),
    FLAME("Flame", "flame"),
    CLOVER("Clover", "clover"),
    BUTTERFLY("Butterfly", "butterfly");

    public static final String RANDOM = "Random";
    public static final float ICON_FRACTION = 0.54f;

    public final String title;
    public final Identifier texture;

    ParticleTexture(String title, String file) {
        this.title = title;
        this.texture = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "textures/particle/" + file + ".png");
    }

    public static String[] modes() {
        ParticleTexture[] all = values();
        String[] out = new String[all.length + 1];
        for (int i = 0; i < all.length; i++) out[i] = all[i].title;
        out[all.length] = RANDOM;
        return out;
    }

    public static ParticleTexture pick(String mode) {
        if (RANDOM.equalsIgnoreCase(mode)) return values()[ThreadLocalRandom.current().nextInt(values().length)];
        for (ParticleTexture t : values()) if (t.title.equalsIgnoreCase(mode)) return t;
        return STAR;
    }
}
