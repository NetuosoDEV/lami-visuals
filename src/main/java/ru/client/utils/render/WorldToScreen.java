package ru.client.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class WorldToScreen {
    private WorldToScreen() {}

    public static Vector3f project(Vec3 world) {
        Minecraft mc = Minecraft.getInstance();
        CameraRenderState cam = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (!cam.initialized) return null;
        float dx = (float) (world.x - cam.pos.x), dy = (float) (world.y - cam.pos.y), dz = (float) (world.z - cam.pos.z);
        Vector4f v = new Vector4f(dx, dy, dz, 1f);
        cam.viewRotationMatrix.transform(v);
        cam.projectionMatrix.transform(v);
        if (v.w <= 0.05f) return null;
        float nx = v.x / v.w, ny = v.y / v.w;
        float sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        return new Vector3f((nx * 0.5f + 0.5f) * sw, (0.5f - ny * 0.5f) * sh, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
}
