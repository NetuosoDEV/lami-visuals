package ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.Modules;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.KeySetting;
import ru.client.modules.settings.ModeSetting;

public final class FreeLook extends Module {

    private final KeySetting lookKey = register(new KeySetting("Look Key", InputConstants.KEY_LALT));
    private final ModeSetting mode = register(new ModeSetting("Mode", "Hold", "Hold", "Toggle"));
    private final BooleanSetting thirdPerson = register(new BooleanSetting("Third Person", true));
    private final BooleanSetting smoothReturn = register(new BooleanSetting("Smooth Return", true));

    private static final long RETURN_MS = 260;

    private boolean active, returning;
    private float yaw, pitch;
    private float fromYaw, fromPitch;
    private long returnStart;
    private CameraType savedType, appliedType;

    public FreeLook() {
        super("FreeLook", "Осмотреться вокруг, не поворачивая персонажа", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        active = false;
        finish();
    }

    @Override
    public void onKeyPress(int key) {
        if (!mode.is("Toggle") || key == -1 || key != lookKey.get()) return;
        if (active) stop(); else start();
    }

    private void start() {
        if (mc.player == null || mc.getCameraEntity() != mc.player) return;
        yaw = returning ? shownYaw(mc.player.getYRot()) : mc.player.getYRot();
        pitch = returning ? shownPitch(mc.player.getXRot()) : mc.player.getXRot();
        returning = false;
        active = true;
        CameraType type = mc.options.getCameraType();
        if (thirdPerson.isEnabled() && savedType == null && type != CameraType.THIRD_PERSON_BACK) {
            savedType = type;
            appliedType = CameraType.THIRD_PERSON_BACK;
            mc.options.setCameraType(appliedType);
        }
    }

    private void stop() {
        active = false;
        if (smoothReturn.isEnabled() && mc.player != null) {
            returning = true;
            fromYaw = yaw;
            fromPitch = pitch;
            returnStart = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    private void finish() {
        returning = false;
        if (savedType != null && mc.options.getCameraType() == appliedType) mc.options.setCameraType(savedType);
        savedType = appliedType = null;
    }

    private float progress() {
        float t = Math.min(1f, (System.currentTimeMillis() - returnStart) / (float) RETURN_MS);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private float shownYaw(float playerYaw) {
        return fromYaw + Mth.wrapDegrees(playerYaw - fromYaw) * progress();
    }

    private float shownPitch(float playerPitch) {
        return Mth.lerp(progress(), fromPitch, playerPitch);
    }

    @Override
    public void onTick() {
        if (mc.player == null && (active || returning)) {
            active = false;
            finish();
        }
    }

    private void frame() {
        if (mode.is("Hold")) {
            boolean down = lookKey.get() != -1 && InputConstants.isKeyDown(lookKey.get());
            if (!active && down && mc.gui.screen() == null) start();
            else if (active && !down) stop();
        }
        if (returning && System.currentTimeMillis() - returnStart >= RETURN_MS) finish();
    }

    public static void onCameraFrame() {
        FreeLook m = Modules.enabled(FreeLook.class);
        if (m != null) m.frame();
    }

    public static boolean turn(double xo, double yo) {
        FreeLook m = Modules.enabled(FreeLook.class);
        if (m == null || !m.active) return false;
        m.yaw += (float) xo * 0.15f;
        m.pitch = Mth.clamp(m.pitch + (float) yo * 0.15f, -90f, 90f);
        return true;
    }

    public static float cameraYaw(Entity entity, float vanilla) {
        FreeLook m = Modules.enabled(FreeLook.class);
        if (m == null || entity != mc.player) return vanilla;
        if (m.active) return m.yaw;
        return m.returning ? m.shownYaw(vanilla) : vanilla;
    }

    public static float cameraPitch(Entity entity, float vanilla) {
        FreeLook m = Modules.enabled(FreeLook.class);
        if (m == null || entity != mc.player) return vanilla;
        if (m.active) return m.pitch;
        return m.returning ? m.shownPitch(vanilla) : vanilla;
    }
}
