package ru.client.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import ru.client.core.ClientMod;
import ru.client.modules.impl.render.Cosmetics;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CosmeticWings {
    private CosmeticWings() {}

    public enum Type {
        ANGEL("angel", -0.44f, 0.10f),
        BONE("bone", -0.43f, 0.48f),
        DEMON("demon", -0.48f, 0.30f);

        final String file;
        final float rootX, rootY;

        Type(String file, float rootX, float rootY) {
            this.file = file;
            this.rootX = rootX;
            this.rootY = rootY;
        }
    }

    public record Pose(Type type, float spread, float lift, float size, float height) {}

    private record Baked(float[] pos, float[] uv, float[] normal, int count, Identifier texture) {
        void emit(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
            for (int i = 0; i + 2 < count; i += 3) {
                for (int k = 0; k < 4; k++) {
                    int j = i + Math.min(k, 2);
                    vc.addVertex(pose, pos[j * 3], pos[j * 3 + 1], pos[j * 3 + 2])
                            .setColor(-1)
                            .setUv(uv[j * 2], uv[j * 2 + 1])
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(pose, normal[j * 3], normal[j * 3 + 1], normal[j * 3 + 2]);
                }
            }
        }
    }

    private static final Map<Type, Baked> BAKED = new EnumMap<>(Type.class);
    private static final Map<Type, Boolean> FAILED = new EnumMap<>(Type.class);

    public static void preload(Type type) {
        if (BAKED.containsKey(type) || FAILED.containsKey(type)) return;
        Identifier file = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "cosmetics/wings/" + type.file + ".glb");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(file);
        if (res.isEmpty()) {
            FAILED.put(type, true);
            ClientMod.LOGGER.warn("Cosmetics: нет модели крыльев {}", file);
            return;
        }
        try (InputStream in = res.get().open()) {
            GlbModel model = GlbModel.read(in);
            BAKED.put(type, bake(type, model));
        } catch (Exception e) {
            FAILED.put(type, true);
            ClientMod.LOGGER.warn("Cosmetics: не удалось загрузить {}", file, e);
        }
    }

    private static Baked bake(Type type, GlbModel m) {
        float rootZ = (m.min.z + m.max.z) / 2f;
        float[] pos = new float[m.vertexCount * 3], nrm = new float[m.vertexCount * 3];
        for (int i = 0; i < m.vertexCount; i++) {
            pos[i * 3] = -(m.pos[i * 3] - type.rootX);
            pos[i * 3 + 1] = -(m.pos[i * 3 + 1] - type.rootY);
            pos[i * 3 + 2] = m.pos[i * 3 + 2] - rootZ;
            nrm[i * 3] = -m.normal[i * 3];
            nrm[i * 3 + 1] = -m.normal[i * 3 + 1];
            nrm[i * 3 + 2] = m.normal[i * 3 + 2];
        }
        Identifier tex = Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "cosmetics/wings/" + type.file);
        Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(() -> "LamiVisuals wings " + type.file, m.texture));
        return new Baked(pos, m.uv.clone(), nrm, m.vertexCount, tex);
    }

    public static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {

        public Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
            Pose pose = Cosmetics.wingPose(state);
            if (pose == null) return;
            Baked model = BAKED.get(pose.type());
            if (model == null) return;
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0f);
            RenderType type = RenderTypes.entityCutout(model.texture());
            float back = state.chestEquipment.isEmpty() ? 0.16f : 0.22f;

            poseStack.pushPose();
            getParentModel().root().translateAndRotate(poseStack);
            getParentModel().body.translateAndRotate(poseStack);
            for (int side = 0; side < 2; side++) {
                poseStack.pushPose();
                if (side == 1) poseStack.scale(-1f, 1f, 1f);
                poseStack.translate(-0.06f, 0.2f - pose.height(), back);
                poseStack.rotate(Axis.YP.rotation(pose.spread()));
                poseStack.rotate(Axis.ZP.rotation(pose.lift()));
                poseStack.scale(pose.size(), pose.size(), pose.size());
                collector.submitCustomGeometry(poseStack, type, (p, vc) -> model.emit(p, vc, lightCoords, overlay));
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
}
