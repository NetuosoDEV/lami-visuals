package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.NumberSetting;

public final class ViewModel extends Module {

    private final NumberSetting x = register(new NumberSetting("X", 0, -1, 1, 0.01));
    private final NumberSetting y = register(new NumberSetting("Y", 0, -1, 1, 0.01));
    private final NumberSetting z = register(new NumberSetting("Z", 0, -1, 1, 0.01));
    private final NumberSetting scale = register(new NumberSetting("Scale", 1, 0.3, 2, 0.05));

    public ViewModel() {
        super("ViewModel", "Положение рук", Category.RENDER);
    }

    public void apply(PoseStack matrices, int side) {
        matrices.translate(x.get() * side, y.get(), z.get());
    }

    public void applyScale(PoseStack matrices) {
        float s = scale.getFloat();
        if (s != 1f) matrices.scale(s, s, s);
    }
}
