package ru.client.modules.impl.render;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.ModeSetting;
import ru.client.modules.settings.NumberSetting;

public final class ChunkAnimator extends Module {

    private final ModeSetting direction = register(new ModeSetting("Direction", "Below", "Below", "Above"));
    private final NumberSetting depth = register(new NumberSetting("Depth", 16, 4, 48, 1));
    private final NumberSetting duration = register(new NumberSetting("Duration", 0.7, 0.2, 2.0, 0.05));
    private final BooleanSetting fade = register(new BooleanSetting("Fade", true));

    public ChunkAnimator() {
        super("ChunkAnimator", "Новые чанки плавно выезжают снизу или опускаются сверху", Category.RENDER);
    }

    public static float visibility(SectionRenderDispatcher.RenderSection section, long now, long vanillaFade) {
        ChunkAnimator m = Modules.enabled(ChunkAnimator.class);
        if (m == null) return section.getVisibility(now, vanillaFade);
        float p = section.getVisibility(now, Math.round(m.duration.get() * 1000));
        if (p >= 1f) return 1f;
        int code = (m.depth.getInt() << 2) | (m.fade.isEnabled() ? 2 : 0) | (m.direction.is("Above") ? 1 : 0);
        return 2f + code + Math.min(p, 0.999f);
    }
}
