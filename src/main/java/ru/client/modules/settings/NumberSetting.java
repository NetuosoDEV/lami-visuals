package ru.client.modules.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

public final class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double clamped = Mth.clamp(v, min, max);
        value = step > 0 ? Math.round(clamped / step) * step : clamped;
    }

    public float getFloat() { return value.floatValue(); }
    public int getInt() { return value.intValue(); }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
}
