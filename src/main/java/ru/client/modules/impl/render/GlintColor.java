package ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import ru.client.core.ClientMod;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.ColorSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;

import java.io.InputStream;
import java.util.Optional;

public final class GlintColor extends Module {

    private final ModeSetting color = register(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Custom"));
    private final ColorSetting customColor = register(new ColorSetting("Custom Color", 0xFFFF4FD8));
    private final NumberSetting speed = register(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
    private final NumberSetting strength = register(new NumberSetting("Strength", 1.0, 0.1, 1.5, 0.05));

    private static final Identifier[] TEXTURES = {ItemFeatureRenderer.ENCHANTED_GLINT_ITEM, ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR};

    private final NativeImage[] originals = new NativeImage[TEXTURES.length];
    private final AbstractTexture[] owners = new AbstractTexture[TEXTURES.length];
    private int uploadedColor;

    public GlintColor() {
        super("GlintColor", "Цвет, скорость и яркость блеска зачарований", Category.RENDER);
        customColor.visibleWhen(() -> color.is("Custom"));
    }

    public static double speedMul() {
        GlintColor g = Modules.enabled(GlintColor.class);
        return g == null ? 1.0 : g.speed.get();
    }

    public static double strength(double vanilla) {
        GlintColor g = Modules.enabled(GlintColor.class);
        return g == null ? vanilla : Math.min(1.5, vanilla * g.strength.get());
    }

    private int target() {
        return switch (color.get()) {
            case "Rainbow" -> ColorUtils.rainbow(5000, 0, 0.75f, 1f, 255);
            case "Custom" -> customColor.getArgb();
            default -> Theme.get().accentA();
        };
    }

    @Override
    public void onTick() {
        if (mc.level == null) return;
        int c = target() | 0xFF000000;
        boolean reloaded = false;
        for (int i = 0; i < TEXTURES.length; i++) reloaded |= refreshOriginal(i);
        if (!reloaded && c == uploadedColor) return;
        for (int i = 0; i < TEXTURES.length; i++) upload(i, c);
        uploadedColor = c;
    }

    @Override
    protected void onDisable() {
        for (int i = 0; i < TEXTURES.length; i++) {
            if (originals[i] != null && owners[i] != null && owners[i] == mc.getTextureManager().getTexture(TEXTURES[i])) {
                write(owners[i], originals[i]);
            }
            if (originals[i] != null) originals[i].close();
            originals[i] = null;
            owners[i] = null;
        }
        uploadedColor = 0;
    }

    private boolean refreshOriginal(int i) {
        AbstractTexture tex = mc.getTextureManager().getTexture(TEXTURES[i]);
        if (tex == owners[i] && originals[i] != null) return false;
        if (originals[i] != null) originals[i].close();
        originals[i] = null;
        owners[i] = tex;
        Optional<Resource> res = mc.getResourceManager().getResource(TEXTURES[i]);
        if (res.isEmpty()) return false;
        try (InputStream in = res.get().open()) {
            NativeImage img = NativeImage.read(in);
            if (img.format() != NativeImage.Format.RGBA) { img.close(); return false; }
            originals[i] = img;
            return true;
        } catch (Exception e) {
            ClientMod.LOGGER.warn("GlintColor: не удалось прочитать {}", TEXTURES[i], e);
            return false;
        }
    }

    private void upload(int i, int c) {
        NativeImage src = originals[i];
        if (src == null || owners[i] == null) return;
        float cr = ColorUtils.red(c) / 255f, cg = ColorUtils.green(c) / 255f, cb = ColorUtils.blue(c) / 255f;
        try (NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), false)) {
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int p = src.getPixel(x, y);
                    float l = Math.max(ColorUtils.red(p), Math.max(ColorUtils.green(p), ColorUtils.blue(p))) / 255f;
                    out.setPixel(x, y, ColorUtils.rgba((int) (cr * l * 255), (int) (cg * l * 255), (int) (cb * l * 255), ColorUtils.alpha(p)));
                }
            }
            write(owners[i], out);
        }
    }

    private static void write(AbstractTexture tex, NativeImage img) {
        try {
            GpuTexture gpu = tex.getTexture();
            if (gpu == null || gpu.isClosed() || gpu.getWidth(0) != img.getWidth() || gpu.getHeight(0) != img.getHeight()) return;
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, img);
        } catch (Exception e) {
            ClientMod.LOGGER.warn("GlintColor: не удалось обновить текстуру блеска", e);
        }
    }
}
