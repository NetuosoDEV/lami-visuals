package ru.client.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import ru.client.core.ClientMod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class SmoothFonts {
    private SmoothFonts() {}

    private static final String PREFIX = ClientMod.MOD_ID + ":logo";
    private static final Set<GpuTextureView> VIEWS = Collections.newSetFromMap(new WeakHashMap<>());

    public static boolean wants(String atlasName) {
        return atlasName != null && atlasName.startsWith(PREFIX + "/");
    }

    public static void register(GpuTextureView view) { VIEWS.add(view); }

    public static boolean isSmooth(GpuTextureView view) { return view != null && VIEWS.contains(view); }

    public static GpuSampler sampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }
}
