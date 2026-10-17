package ru.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
        if (!this.modes.contains(def)) throw new IllegalArgumentException("Default mode not in list: " + def);
    }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(boolean forward) {
        int i = modes.indexOf(value);
        int size = modes.size();
        value = modes.get(((forward ? i + 1 : i - 1) % size + size) % size);
    }

    public List<String> getModes() { return modes; }

    public String display(String mode) { return mode; }

    public String displayValue() { return display(value); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        String s = e.getAsString();
        if (modes.contains(s)) value = s;
    }
}
