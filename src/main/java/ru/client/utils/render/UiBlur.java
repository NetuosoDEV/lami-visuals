package ru.client.utils.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import ru.client.core.ClientMod;

import java.util.Optional;

public final class UiBlur {
    private UiBlur() {}

    public static final RenderPipeline PIPELINE = RenderPipelines.registerOptional(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "pipeline/ui_blur"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "core/ui_blur"))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .build());

    private static GpuTexture texA, texB;
    private static GpuTextureView viewA, viewB;
    private static DynamicTexture empty;
    private static boolean failed, requested, captured;
    private static float strength = 1f;
    private static final float[] PERIM = new float[RenderUtils.PERIMETER_CAPACITY];

    public static void init() {}

    public static boolean available() { return !failed; }

    public static void panel(GuiGraphicsExtractor ctx, float x, float y, float w, float h,
                             float rTL, float rTR, float rBR, float rBL, int alpha, float strength) {
        if (failed || w <= 0 || h <= 0 || alpha <= 0) return;
        requested = true;
        UiBlur.strength = strength;
        Minecraft mc = Minecraft.getInstance();
        float scale = mc.getWindow().getGuiScale();
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        float sw = Math.max(1, main.width), sh = Math.max(1, main.height);
        int n = RenderUtils.perimeter(PERIM, x, y, w, h, rTL, rTR, rBR, rBL);
        Matrix3x2f pose = new Matrix3x2f(ctx.pose());
        float cx = x + w / 2f, cy = y + h / 2f;
        float[] xy = new float[n * 8], uv = new float[n * 8];
        int v = 0;
        Vector2f tmp = new Vector2f();
        for (int i = 0; i < n; i++) {
            int j = i * 2, k = ((i + 1) % n) * 2;
            float[] q = {cx, cy, PERIM[j], PERIM[j + 1], PERIM[k], PERIM[k + 1], PERIM[k], PERIM[k + 1]};
            float area = (q[0] * q[3] - q[2] * q[1]) + (q[2] * q[5] - q[4] * q[3]) + (q[4] * q[7] - q[6] * q[5]) + (q[6] * q[1] - q[0] * q[7]);
            int[] order = area > 0 ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};
            for (int o : order) {
                float px = q[o * 2], py = q[o * 2 + 1];
                xy[v * 2] = px;
                xy[v * 2 + 1] = py;
                pose.transformPosition(px, py, tmp);
                uv[v * 2] = tmp.x * scale / sw;
                uv[v * 2 + 1] = 1f - tmp.y * scale / sh;
                v++;
            }
        }
        ScreenRectangle scissor = ctx.scissorStack.peek();
        ScreenRectangle b = new ScreenRectangle((int) Math.floor(x), (int) Math.floor(y),
                Math.max(1, (int) Math.ceil(w) + 1), Math.max(1, (int) Math.ceil(h) + 1)).transformMaxBounds(pose);
        if (scissor != null) b = scissor.intersection(b);
        if (b == null) return;
        ctx.guiRenderState.addGuiElement(new State(pose, xy, uv, v, (Math.min(255, alpha) << 24) | 0xFFFFFF, scissor, b));
    }

    public static void panel(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float r, int alpha, float strength) {
        panel(ctx, x, y, w, h, r, r, r, r, alpha, strength);
    }

    public static void capture() {
        if (!requested || failed) return;
        requested = false;
        CompiledRenderPipeline compiled = RenderSystem.getCompiledPipelineNullable(PIPELINE);
        if (compiled == null) {
            failed = true;
            ClientMod.LOGGER.warn("UiBlur shader не скомпилировался — блюр под чатом и Tab отключён");
            return;
        }
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView src = target.getColorTextureView();
        GpuTexture srcTex = target.getColorTexture();
        if (src == null || srcTex == null) return;
        int w = Math.max(1, srcTex.getWidth(0) / 4), h = Math.max(1, srcTex.getHeight(0) / 4);
        GpuDevice device = RenderSystem.getDevice();
        if (texA == null || texA.isClosed() || texA.getWidth(0) != w || texA.getHeight(0) != h) {
            release();
            int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING;
            texA = device.createTexture("LamiVisuals UI blur A", usage, srcTex.getFormat(), w, h, 1, 1);
            texB = device.createTexture("LamiVisuals UI blur B", usage, srcTex.getFormat(), w, h, 1, 1);
            viewA = device.createTextureView(texA);
            viewB = device.createTextureView(texB);
        }
        CommandEncoder encoder = device.createCommandEncoder();
        GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        float s = strength;
        pass(encoder, compiled, linear, src, viewA, 0, 0, true);
        pass(encoder, compiled, linear, viewA, viewB, s, 0, false);
        pass(encoder, compiled, linear, viewB, viewA, 0, s, false);
        pass(encoder, compiled, linear, viewA, viewB, 2 * s, 0, false);
        pass(encoder, compiled, linear, viewB, viewA, 0, 2 * s, false);
        captured = true;
    }

    private static void pass(CommandEncoder encoder, CompiledRenderPipeline compiled, GpuSampler sampler,
                             GpuTextureView in, GpuTextureView out, float dx, float dy, boolean downsample) {
        GpuBufferSlice params = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(), new Vector4f(dx, dy, downsample ? 1f : 0f, 0f), new Vector3f(), new Matrix4f());
        try (RenderPass pass = encoder.createRenderPass(() -> "LamiVisuals UI blur", out, Optional.empty())) {
            pass.setPipeline(compiled);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("InSampler", in, sampler);
            pass.setUniform("DynamicTransforms", params);
            pass.draw(3, 1, 0, 0);
        }
    }

    private static void release() {
        if (viewA != null) viewA.close();
        if (viewB != null) viewB.close();
        if (texA != null) texA.close();
        if (texB != null) texB.close();
        viewA = viewB = null;
        texA = texB = null;
        captured = false;
    }

    private static TextureSetup textureSetup() {
        GpuTextureView view = captured && viewA != null && !viewA.isClosed() ? viewA : emptyView();
        return TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    private static GpuTextureView emptyView() {
        if (empty == null) {
            empty = new DynamicTexture("LamiVisuals UI blur (empty)", 1, 1, true);
            empty.upload();
        }
        return empty.getTextureView();
    }

    private record State(Matrix3x2f pose, float[] xy, float[] uv, int count, int color,
                         @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int i = 0; i < count; i++) {
                vc.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setUv(uv[i * 2], uv[i * 2 + 1]).setColor(color);
            }
        }

        @Override
        public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }

        @Override
        public TextureSetup textureSetup() { return UiBlur.textureSetup(); }
    }
}
