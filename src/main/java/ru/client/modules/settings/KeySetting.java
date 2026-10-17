package ru.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class KeySetting extends Setting<Integer> {
    public KeySetting(String name, int key) { super(name, key); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { value = e.getAsInt(); }
}
