package ru.client.modules.impl.render;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;

public final class Shaders extends Module {

    private final BooleanSetting motionBlur = register(new BooleanSetting("Motion Blur", false));
    private final NumberSetting blurAmount = register(new NumberSetting("Blur Amount", 0.5, 0.1, 0.9, 0.05));
    private final BooleanSetting bloom = register(new BooleanSetting("Bloom", true));
    private final NumberSetting bloomStrength = register(new NumberSetting("Bloom Strength", 0.6, 0.1, 2.0, 0.05));
    private final NumberSetting bloomThreshold = register(new NumberSetting("Bloom Threshold", 0.7, 0.3, 1.0, 0.02));
    private final NumberSetting bloomRadius = register(new NumberSetting("Bloom Radius", 1.5, 0.5, 3.0, 0.1));
    private final ModeSetting grading = register(new ModeSetting("Grading", "Cinematic",
            "Off", "Cinematic", "Warm", "Cold", "Vivid", "Noir", "Neon", "Faded"));
    private final NumberSetting gradeStrength = register(new NumberSetting("Grade Strength", 1.0, 0.0, 1.0, 0.05));
    private final NumberSetting vignette = register(new NumberSetting("Vignette", 0.25, 0.0, 1.0, 0.05));

    public record Grade(float[] lift, float[] gamma, float[] gain, float saturation, float contrast, float exposure) {}

    private static final Grade NEUTRAL = new Grade(new float[]{0, 0, 0}, new float[]{1, 1, 1}, new float[]{1, 1, 1}, 1f, 1f, 0f);

    public Shaders() {
        super("Shaders", "Размытие в движении, свечение ярких мест и цветокоррекция", Category.RENDER);
        blurAmount.visibleWhen(motionBlur::isEnabled);
        bloomStrength.visibleWhen(bloom::isEnabled);
        bloomThreshold.visibleWhen(bloom::isEnabled);
        bloomRadius.visibleWhen(bloom::isEnabled);
        gradeStrength.visibleWhen(() -> !grading.is("Off"));
    }

    public static Shaders active() { return Modules.enabled(Shaders.class); }

    public boolean motionBlur() { return motionBlur.isEnabled(); }
    public float blurAmount() { return blurAmount.getFloat(); }
    public boolean bloom() { return bloom.isEnabled(); }
    public float bloomStrength() { return bloomStrength.getFloat(); }
    public float bloomThreshold() { return bloomThreshold.getFloat(); }
    public float bloomRadius() { return bloomRadius.getFloat(); }
    public float gradeStrength() { return grading.is("Off") ? 0f : gradeStrength.getFloat(); }
    public float vignette() { return vignette.getFloat(); }

    public Grade grade() {
        return switch (grading.get()) {
            case "Cinematic" -> new Grade(new float[]{0.00f, 0.02f, 0.05f}, new float[]{1f, 1f, 1.02f}, new float[]{1.07f, 1.00f, 0.90f}, 0.92f, 1.12f, 0f);
            case "Warm" -> new Grade(new float[]{0.03f, 0.015f, 0f}, new float[]{1.03f, 1f, 0.97f}, new float[]{1.08f, 1.01f, 0.86f}, 1.05f, 1.05f, 0.05f);
            case "Cold" -> new Grade(new float[]{0f, 0.01f, 0.04f}, new float[]{0.98f, 1f, 1.03f}, new float[]{0.90f, 0.99f, 1.10f}, 0.95f, 1.06f, 0f);
            case "Vivid" -> new Grade(new float[]{0f, 0f, 0f}, new float[]{1f, 1f, 1f}, new float[]{1.02f, 1.02f, 1.02f}, 1.38f, 1.10f, 0.05f);
            case "Noir" -> new Grade(new float[]{0.01f, 0.01f, 0.01f}, new float[]{0.95f, 0.95f, 0.95f}, new float[]{1.05f, 1.05f, 1.05f}, 0f, 1.32f, 0f);
            case "Neon" -> new Grade(new float[]{0.04f, 0f, 0.07f}, new float[]{1f, 0.97f, 1.03f}, new float[]{1.06f, 0.95f, 1.12f}, 1.25f, 1.16f, 0f);
            case "Faded" -> new Grade(new float[]{0.07f, 0.06f, 0.05f}, new float[]{1.02f, 1f, 0.97f}, new float[]{0.95f, 0.93f, 0.88f}, 0.78f, 0.90f, 0.05f);
            default -> NEUTRAL;
        };
    }
}
