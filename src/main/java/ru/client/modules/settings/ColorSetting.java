package ru.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int argb) { super(name, argb); }

    public int getArgb() { return value; }
    public String toHex() { return String.format("#%08X", value); }

    public void fromHex(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() == 6) s = "FF" + s;
        value = (int) Long.parseLong(s, 16);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(toHex()); }
    @Override public void fromJson(JsonElement e) { fromHex(e.getAsString()); }
}
