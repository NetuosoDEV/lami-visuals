package ru.client.modules.impl.render;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;

public final class Ambience extends Module {

    private final BooleanSetting customTime = register(new BooleanSetting("Custom Time", true));
    private final NumberSetting time = register(new NumberSetting("Time", 13000, 0, 24000, 250));
    private final BooleanSetting cycle = register(new BooleanSetting("Day Cycle", false));
    private final NumberSetting cycleSpeed = register(new NumberSetting("Cycle Speed", 50, 5, 500, 5));
    private final BooleanSetting clearWeather = register(new BooleanSetting("Clear Weather", true));

    private final ModeSetting skyShader = register(new ModeSetting("Sky Shader", "Off",
            "Off", "Aurora", "Galaxy", "Nebula", "Sunset", "Starfall", "Void", "Lava", "Synthwave", "Storm"));
    private final ModeSetting shaderColors = register(new ModeSetting("Shader Colors", "Preset", "Preset", "Theme"));
    private final NumberSetting shaderSpeed = register(new NumberSetting("Shader Speed", 1.0, 0.1, 3.0, 0.1));
    private final NumberSetting shaderBrightness = register(new NumberSetting("Shader Brightness", 1.0, 0.2, 2.0, 0.05));
    private final BooleanSetting nightOnly = register(new BooleanSetting("Night Only", false));

    private final BooleanSetting customSky = register(new BooleanSetting("Custom Sky", false));
    private final ColorSetting skyColor = register(new ColorSetting("Sky Color", 0xFF7A5CFF));
    private final BooleanSetting customFog = register(new BooleanSetting("Custom Fog", false));
    private final ColorSetting fogColor = register(new ColorSetting("Fog Color", 0xFFE38CFF));
    private final BooleanSetting customClouds = register(new BooleanSetting("Custom Clouds", false));
    private final ColorSetting cloudColor = register(new ColorSetting("Cloud Color", 0xFFFFD6F5));
    private final BooleanSetting themeSync = register(new BooleanSetting("Theme Sync", false));
    private final BooleanSetting nightShade = register(new BooleanSetting("Night Shade", true));

    private static final int[][] PALETTES = {
            {0xFF33FF9E, 0xFF9E4DFF, 0xFF09121F},
            {0xFF9E6BFF, 0xFF388CFF, 0xFF0A0817},
            {0xFFFF5C9E, 0xFF4D6BFF, 0xFF0F091A},
            {0xFFFF8C38, 0xFFE05794, 0xFFFF9E61},
            {0xFFBFE0FF, 0xFF8C73FF, 0xFF0A0D1C},
            {0xFF8040F2, 0xFF261F4D, 0xFF030306},
            {0xFFFF731A, 0xFFD91F0D, 0xFF380D05},
            {0xFFFF59BF, 0xFF40D9FF, 0xFF8C1F73},
            {0xFFBFD1FF, 0xFF4D5773, 0xFF1A1C24},
    };
    private static final int SUNSET = 3, VOID = 5;

    private float vanillaSkyBrightness = 1f;

    public Ambience() {
        super("Ambience", "Время суток, погода, шейдер неба, цвет неба и тумана", Category.RENDER);
        time.visibleWhen(() -> customTime.isEnabled() && !cycle.isEnabled());
        cycle.visibleWhen(customTime::isEnabled);
        cycleSpeed.visibleWhen(() -> customTime.isEnabled() && cycle.isEnabled());
        shaderColors.visibleWhen(this::shaderOn);
        shaderSpeed.visibleWhen(this::shaderOn);
        shaderBrightness.visibleWhen(this::shaderOn);
        nightOnly.visibleWhen(this::shaderOn);
        customSky.visibleWhen(() -> !shaderOn());
        skyColor.visibleWhen(() -> !shaderOn() && customSky.isEnabled() && !themeSync.isEnabled());
        fogColor.visibleWhen(() -> customFog.isEnabled() && !themeSync.isEnabled());
        cloudColor.visibleWhen(customClouds::isEnabled);
        themeSync.visibleWhen(() -> (customSky.isEnabled() && !shaderOn()) || customFog.isEnabled());
        nightShade.visibleWhen(() -> customSky.isEnabled() || customFog.isEnabled() || customClouds.isEnabled());
    }

    private boolean shaderOn() { return !skyShader.is("Off"); }

    public Long timeOfDay() {
        if (!customTime.isEnabled()) return null;
        if (cycle.isEnabled()) return (long) (System.currentTimeMillis() / 50.0 * cycleSpeed.get() / 10.0) % 24000L;
        return (long) time.get().doubleValue();
    }

    public boolean clearWeather() { return clearWeather.isEnabled(); }

    public record SkyParams(int mode, float opacity, float intensity, float speed, int colorA, int colorB, int horizon) {}

    public SkyParams skyParams() {
        int mode = skyShader.getModes().indexOf(skyShader.get()) - 1;
        if (mode < 0 || mode >= PALETTES.length) return null;
        float opacity = 1f;
        if (nightOnly.isEnabled()) {
            float t = Math.min(1f, Math.max(0f, (vanillaSkyBrightness - 0.2f) / 0.55f));
            opacity = 1f - t * t * (3 - 2 * t);
            if (opacity < 0.01f) return null;
        }
        int[] pal = PALETTES[mode];
        int a = pal[0], b = pal[1], horizon = pal[2];
        if (shaderColors.is("Theme")) {
            Theme t = Theme.get();
            a = t.accentA();
            b = t.accentB();
            if (mode == SUNSET) horizon = ColorUtils.lerp(pal[2], a, 0.35f);
            else if (mode != VOID) horizon = ColorUtils.lerp(pal[2], b, 0.12f);
        }
        if (customFog.isEnabled()) horizon = themeSync.isEnabled() ? Theme.get().accentB() : fogColor.getArgb();
        return new SkyParams(mode, opacity, shaderBrightness.getFloat(), shaderSpeed.getFloat(), a, b, horizon);
    }

    public Vector3fc sky(Vector3fc vanilla) {
        vanillaSkyBrightness = Math.max(vanilla.x(), Math.max(vanilla.y(), vanilla.z()));
        if (!customSky.isEnabled() || shaderOn()) return null;
        return rgb(themeSync.isEnabled() ? Theme.get().accentA() : skyColor.getArgb(), vanilla);
    }

    public Vector3fc fog(Vector3fc vanilla) {
        SkyParams p = skyParams();
        if (p != null) {
            int h = p.horizon();
            float k = p.opacity();
            return new Vector3f(
                    vanilla.x() + (ColorUtils.red(h) / 255f - vanilla.x()) * k,
                    vanilla.y() + (ColorUtils.green(h) / 255f - vanilla.y()) * k,
                    vanilla.z() + (ColorUtils.blue(h) / 255f - vanilla.z()) * k);
        }
        if (!customFog.isEnabled()) return null;
        return rgb(themeSync.isEnabled() ? Theme.get().accentB() : fogColor.getArgb(), vanilla);
    }

    public Vector4fc clouds(Vector4fc vanilla) {
        if (!customClouds.isEnabled()) return null;
        int c = cloudColor.getArgb();
        float k = shade(Math.max(vanilla.x(), Math.max(vanilla.y(), vanilla.z())));
        return new Vector4f(ColorUtils.red(c) / 255f * k, ColorUtils.green(c) / 255f * k, ColorUtils.blue(c) / 255f * k,
                vanilla.w() * ColorUtils.alpha(c) / 255f);
    }

    private Vector3fc rgb(int c, Vector3fc vanilla) {
        float k = shade(Math.max(vanilla.x(), Math.max(vanilla.y(), vanilla.z())));
        return new Vector3f(ColorUtils.red(c) / 255f * k, ColorUtils.green(c) / 255f * k, ColorUtils.blue(c) / 255f * k);
    }

    private float shade(float vanillaBrightness) {
        if (!nightShade.isEnabled()) return 1f;
        return Math.min(1f, Math.max(0f, vanillaBrightness));
    }
}
