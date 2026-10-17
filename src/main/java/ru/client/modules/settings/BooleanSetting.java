package ru.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean def) { super(name, def); }

    public boolean isEnabled() { return value; }
    public void toggle() { value = !value; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { value = e.getAsBoolean(); }
}
