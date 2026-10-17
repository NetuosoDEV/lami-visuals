package ru.client.modules.settings;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public Setting<T> visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return this;
    }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);

    public void reset() { value = defaultValue; }
    public String getName() { return name; }
    public String getDisplayName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public T getDefault() { return defaultValue; }
    public boolean isVisible() { return visibility.getAsBoolean(); }
}
