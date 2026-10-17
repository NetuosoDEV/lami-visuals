package ru.client.core;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public enum CustomSound {
    BONK("Bonk", "bonk"),
    PUNCH("Punch", "punch"),
    SPARK("Spark", "spark"),
    SLASH("Slash", "slash"),
    POP("Pop", "pop"),
    COIN("Coin", "coin"),
    GLASS("Glass", "glass"),
    LASER("Laser", "laser"),
    CLAP("Clap", "clap"),
    RETRO("Retro", "retro"),
    WOOD("Wood", "wood"),
    BOOM("Boom", "boom"),
    VICTORY("Victory", "victory"),
    SOUL("Soul", "soul"),
    SHATTER("Shatter", "shatter");

    public final String title;
    public final SoundEvent event;

    CustomSound(String title, String file) {
        this.title = title;
        this.event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "hit." + file));
    }

    public static String[] titles() {
        CustomSound[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].title;
        return out;
    }

    public static CustomSound byTitle(String title) {
        for (CustomSound s : values()) if (s.title.equalsIgnoreCase(title)) return s;
        return null;
    }

    public static String[] withTitles(String... head) {
        String[] t = titles();
        String[] out = new String[head.length + t.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(t, 0, out, head.length, t.length);
        return out;
    }
}
