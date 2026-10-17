package ru.client.modules.impl.render;

import ru.client.modules.Category;
import ru.client.modules.Module;
import ru.client.modules.settings.BooleanSetting;
import ru.client.modules.settings.NumberSetting;
import ru.client.ui.theme.Theme;
import ru.client.utils.render.ColorUtils;
import ru.client.utils.render.Render3D;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Trajectories extends Module {

    private final NumberSetting lineWidth = register(new NumberSetting("Line Width", 2.5, 1, 6, 0.5));
    private final BooleanSetting landing = register(new BooleanSetting("Landing Point", true));

    private record Params(double speed, double gravity, double drag, float roll) {}

    private final List<Vec3> points = new ArrayList<>();

    public Trajectories() {
        super("Trajectories", "Траектория снарядов", Category.RENDER);
    }

    @Override
    public void onRender3D(PoseStack ms, Camera camera, float td) {
        if (nullCheck()) return;
        LocalPlayer p = mc.player;
        Params params = null;
        for (InteractionHand hand : InteractionHand.values()) {
            params = params(p, p.getItemInHand(hand));
            if (params != null) break;
        }
        if (params == null) return;

        float yaw = (float) Math.toRadians(p.getViewYRot(td));
        float pitch = (float) Math.toRadians(p.getViewXRot(td));
        float roll = (float) Math.toRadians(params.roll());
        Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch),
                -Mth.sin(pitch + roll),
                Mth.cos(yaw) * Mth.cos(pitch)).normalize().scale(params.speed());
        Vec3 mv = p.getDeltaMovement();
        Vec3 vel = dir.add(mv.x, p.onGround() ? 0 : mv.y, mv.z);
        Vec3 pos = p.getPosition(td).add(0, p.getEyeHeight() - 0.1, 0);

        points.clear();
        points.add(pos);
        HitResult.Type hitType = HitResult.Type.MISS;
        Vec3 hitPos = null;
        Direction hitSide = Direction.UP;
        Entity hitEntity = null;
        int bottom = mc.level.getMinY() - 16;

        for (int tick = 0; tick < 300; tick++) {
            Vec3 next = pos.add(vel);
            BlockHitResult bhr = mc.level.clip(new ClipContext(pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Entity ent = null;
            Vec3 entHit = null;
            AABB sweep = new AABB(pos, next).inflate(1.0);
            for (Entity e : mc.level.getEntities(p, sweep, e -> !e.isSpectator() && e.isPickable())) {
                Optional<Vec3> h = e.getBoundingBox().inflate(0.3).clip(pos, next);
                if (h.isPresent() && (entHit == null || pos.distanceToSqr(h.get()) < pos.distanceToSqr(entHit))) {
                    entHit = h.get();
                    ent = e;
                }
            }
            if (ent != null && (bhr.getType() == HitResult.Type.MISS || pos.distanceToSqr(entHit) < pos.distanceToSqr(bhr.getLocation()))) {
                points.add(entHit);
                hitType = HitResult.Type.ENTITY;
                hitPos = entHit;
                hitEntity = ent;
                break;
            }
            if (bhr.getType() != HitResult.Type.MISS) {
                points.add(bhr.getLocation());
                hitType = HitResult.Type.BLOCK;
                hitPos = bhr.getLocation();
                hitSide = bhr.getDirection();
                break;
            }
            points.add(next);
            pos = next;
            boolean water = mc.level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
            vel = vel.scale(water ? 0.8 : params.drag()).add(0, -params.gravity(), 0);
            if (pos.y < bottom) break;
        }

        Theme t = Theme.get();
        int cA = t.accentA(), cB = t.accentB();
        int n = points.size();
        Render3D.begin();
        Render3D.polyline(ms, points, i -> ColorUtils.lerp(cA, cB, i / (float) Math.max(1, n - 1)), lineWidth.getFloat());

        if (landing.isEnabled() && hitPos != null) {
            if (hitType == HitResult.Type.ENTITY && hitEntity != null) {
                AABB box = hitEntity.getBoundingBox();
                Render3D.filledBox(ms, box, ColorUtils.withAlpha(0xFFFF4040, 50));
                Render3D.outlineBox(ms, box, 0xFFFF4040, 2f);
            } else if (hitSide.getAxis() == Direction.Axis.Y) {
                double y = hitPos.y + (hitSide == Direction.UP ? 0.01 : -0.01);
                Render3D.disc(ms, hitPos.x, y, hitPos.z, 0.45f, ColorUtils.withAlpha(cB, 30), ColorUtils.withAlpha(cB, 110));
                Render3D.ring(ms, hitPos.x, y, hitPos.z, 0.45f, 0.05f, tt -> ColorUtils.lerp(cA, cB, tt));
            } else {
                AABB box = new AABB(hitPos, hitPos).inflate(0.15);
                Render3D.filledBox(ms, box, ColorUtils.withAlpha(cB, 80));
                Render3D.outlineBox(ms, box, cB, 2f);
            }
        }
        Render3D.end();
    }

    private static Params params(LocalPlayer p, ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof EnderpearlItem || item instanceof SnowballItem || item instanceof EggItem)
            return new Params(1.5, 0.03, 0.99, 0);
        if (item instanceof ThrowablePotionItem) return new Params(0.5, 0.05, 0.99, -20);
        if (item instanceof ExperienceBottleItem) return new Params(0.7, 0.07, 0.99, -20);
        if (item instanceof BowItem) {
            if (!p.isUsingItem() || p.getUseItem() != stack) return null;
            int used = item.getUseDuration(stack, p) - p.getUseItemRemainingTicks();
            float pull = BowItem.getPowerForTime(used);
            return pull < 0.1f ? null : new Params(pull * 3.0, 0.05, 0.99, 0);
        }
        if (item instanceof CrossbowItem) return CrossbowItem.isCharged(stack) ? new Params(3.15, 0.05, 0.99, 0) : null;
        if (item instanceof TridentItem) return p.isUsingItem() && p.getUseItem() == stack ? new Params(2.5, 0.05, 0.99, 0) : null;
        return null;
    }
}
