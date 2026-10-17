package ru.client.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;

public final class BlockOverlay extends Module {

    private final NumberSetting fill = register(new NumberSetting("Fill Alpha", 0.2, 0, 1, 0.05));
    private final NumberSetting outline = register(new NumberSetting("Outline Width", 2, 1, 5, 0.5));
    private final NumberSetting smooth = register(new NumberSetting("Smooth", 18, 4, 40, 1));

    private AABB current;
    private long lastNs = System.nanoTime();

    public BlockOverlay() {
        super("BlockOverlay", "Подсветка блока под прицелом", Category.RENDER);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNs) / 1_000_000_000f, 0.1f);
        lastNs = now;
        if (nullCheck() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            current = null;
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.of(mc.player));
        if (shape.isEmpty()) { current = null; return; }
        AABB target = shape.bounds().move(pos).inflate(0.002);

        if (current == null) current = target;
        else {
            double k = 1 - Math.exp(-smooth.get() * dt);
            current = new AABB(
                    current.minX + (target.minX - current.minX) * k, current.minY + (target.minY - current.minY) * k,
                    current.minZ + (target.minZ - current.minZ) * k, current.maxX + (target.maxX - current.maxX) * k,
                    current.maxY + (target.maxY - current.maxY) * k, current.maxZ + (target.maxZ - current.maxZ) * k);
        }

        Theme t = Theme.get();
        Render3D.begin();
        if (fill.get() > 0) Render3D.filledBox(ms, current, ColorUtils.multAlpha(t.accentA(), fill.getFloat()));
        Render3D.outlineBox(ms, current, t.accentB(), outline.getFloat());
        Render3D.end();
    }
}
