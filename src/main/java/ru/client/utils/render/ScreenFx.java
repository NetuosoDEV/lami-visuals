package ru.client.utils.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.client.core.ClientMod;

import java.util.Optional;

public final class ScreenFx {
    private ScreenFx() {}

    public static final RenderPipeline PIPELINE = RenderPipelines.registerOptional(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "pipeline/hit_fx"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "core/hit_fx"))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .build());

    private static GpuTexture copy;
    private static GpuTextureView copyView;
    private static boolean failed;
    private static float strength;

    public static void init() {}

    public static void setStrength(float s) { strength = s; }

    public static void apply() {
        float s = strength;
        strength = 0f;
        if (s < 0.01f || failed) return;
        CompiledRenderPipeline compiled = RenderSystem.getCompiledPipelineNullable(PIPELINE);
        if (compiled == null) {
            failed = true;
            ClientMod.LOGGER.warn("HitFX shader не скомпилировался — RGB-сдвиг отключён");
            return;
        }
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTexture src = target.getColorTexture();
        if (src == null) return;
        int w = src.getWidth(0), h = src.getHeight(0);
        GpuDevice device = RenderSystem.getDevice();
        if (copy == null || copy.isClosed() || copy.getWidth(0) != w || copy.getHeight(0) != h) {
            if (copyView != null) copyView.close();
            if (copy != null) copy.close();
            copy = device.createTexture("LamiVisuals screen copy", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    src.getFormat(), w, h, 1, 1);
            copyView = device.createTextureView(copy);
        }
        CommandEncoder encoder = device.createCommandEncoder();
        encoder.copyTextureToTexture(src, copy, 0, 0, 0, 0, 0, w, h);
        GpuBufferSlice params = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(), new Vector4f(s, 0, 0, 0), new Vector3f(), new Matrix4f());
        try (RenderPass pass = encoder.createRenderPass(() -> "LamiVisuals hit fx", target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(compiled);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("InSampler", copyView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("DynamicTransforms", params);
            pass.draw(3, 1, 0, 0);
        }
    }
}
