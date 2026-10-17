package ru.client.utils.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.client.core.ClientMod;
import ru.client.modules.impl.render.Shaders;

import java.util.Optional;

public final class PostFx {
    private PostFx() {}

    private static RenderPipeline pipeline(String name, BindGroupLayout samplers) {
        return RenderPipelines.registerOptional(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "pipeline/" + name))
                .withVertexShader("core/screenquad")
                .withFragmentShader(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "core/" + name))
                .withBindGroupLayout(samplers)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .build());
    }

    public static final RenderPipeline MOTION = pipeline("post_motion", BindGroupLayouts.SAMPLER0_SAMPLER1);
    public static final RenderPipeline BRIGHT = pipeline("post_bright", BindGroupLayouts.IN_SAMPLER);
    public static final RenderPipeline COMPOSITE = pipeline("post_composite", BindGroupLayouts.SAMPLER0_SAMPLER1);

    private static GpuTexture scene, work, history, bloomA, bloomB;
    private static GpuTextureView sceneView, workView, historyView, bloomAView, bloomBView;
    private static DynamicTexture black;
    private static boolean historyValid, failed;
    private static long lastNs;

    public static void init() {}

    public static void apply() {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 1f / 60f : Math.min((now - lastNs) / 1e9f, 0.1f);
        lastNs = now;
        Shaders s = Shaders.active();
        if (s == null || failed) {
            historyValid = false;
            return;
        }
        boolean motion = s.motionBlur(), bloom = s.bloom();
        float grade = s.gradeStrength(), vignette = s.vignette();
        if (!motion) historyValid = false;
        if (!motion && !bloom && grade <= 0.001f && vignette <= 0.001f) return;

        CompiledRenderPipeline motionP = RenderSystem.getCompiledPipelineNullable(MOTION);
        CompiledRenderPipeline brightP = RenderSystem.getCompiledPipelineNullable(BRIGHT);
        CompiledRenderPipeline blurP = RenderSystem.getCompiledPipelineNullable(UiBlur.PIPELINE);
        CompiledRenderPipeline compositeP = RenderSystem.getCompiledPipelineNullable(COMPOSITE);
        if (motionP == null || brightP == null || blurP == null || compositeP == null) {
            failed = true;
            ClientMod.LOGGER.warn("Shaders: пост-обработка не скомпилировалась — модуль ничего не делает");
            return;
        }

        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTexture main = target.getColorTexture();
        GpuTextureView mainView = target.getColorTextureView();
        if (main == null || mainView == null) return;
        int w = main.getWidth(0), h = main.getHeight(0);
        ensure(main, w, h);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        encoder.copyTextureToTexture(main, scene, 0, 0, 0, 0, 0, w, h);
        GpuTextureView current = sceneView;

        if (motion) {
            if (!historyValid) {
                encoder.copyTextureToTexture(main, history, 0, 0, 0, 0, 0, w, h);
                historyValid = true;
            }
            float keep = (float) Math.pow(s.blurAmount(), dt * 60f);
            pass(encoder, motionP, workView, params(new Matrix4f(), keep, 0, 0, 0),
                    "Sampler0", sceneView, "Sampler1", historyView, linear);
            encoder.copyTextureToTexture(work, history, 0, 0, 0, 0, 0, w, h);
            current = workView;
        }

        GpuTextureView glow = blackView();
        if (bloom) {
            float knee = 0.12f;
            pass(encoder, brightP, bloomAView, params(new Matrix4f(), s.bloomThreshold(), knee, 0, 0), "InSampler", current, null, null, linear);
            float r = s.bloomRadius();
            for (int i = 1; i <= 2; i++) {
                pass(encoder, blurP, bloomBView, params(new Matrix4f(), r * i, 0, 0, 0), "InSampler", bloomAView, null, null, linear);
                pass(encoder, blurP, bloomAView, params(new Matrix4f(), 0, r * i, 0, 0), "InSampler", bloomBView, null, null, linear);
            }
            glow = bloomAView;
        }

        Shaders.Grade g = s.grade();
        Matrix4f gradeMat = new Matrix4f(
                g.lift()[0], g.lift()[1], g.lift()[2], g.saturation(),
                g.gamma()[0], g.gamma()[1], g.gamma()[2], g.contrast(),
                g.gain()[0], g.gain()[1], g.gain()[2], g.exposure(),
                0, 0, 0, 1);
        pass(encoder, compositeP, mainView, params(gradeMat, bloom ? s.bloomStrength() : 0f, grade, vignette, 0),
                "Sampler0", current, "Sampler1", glow, linear);
    }

    private static GpuBufferSlice params(Matrix4f m, float x, float y, float z, float wv) {
        return RenderSystem.getDynamicUniforms().writeTransform(m, new Vector4f(x, y, z, wv), new Vector3f(), new Matrix4f());
    }

    private static void pass(CommandEncoder encoder, CompiledRenderPipeline pipeline, GpuTextureView out, GpuBufferSlice params,
                             String name0, GpuTextureView in0, String name1, GpuTextureView in1, GpuSampler sampler) {
        try (RenderPass pass = encoder.createRenderPass(() -> "LamiVisuals shaders", out, Optional.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(name0, in0, sampler);
            if (name1 != null) pass.setUniform(name1, in1, sampler);
            pass.setUniform("DynamicTransforms", params);
            pass.draw(3, 1, 0, 0);
        }
    }

    private static void ensure(GpuTexture main, int w, int h) {
        if (scene != null && !scene.isClosed() && scene.getWidth(0) == w && scene.getHeight(0) == h) return;
        release();
        GpuDevice device = RenderSystem.getDevice();
        var format = main.getFormat();
        int bw = Math.max(1, w / 4), bh = Math.max(1, h / 4);
        scene = device.createTexture("LamiVisuals shaders scene", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, format, w, h, 1, 1);
        work = device.createTexture("LamiVisuals shaders work",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC, format, w, h, 1, 1);
        history = device.createTexture("LamiVisuals shaders history", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, format, w, h, 1, 1);
        int bloomUsage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING;
        bloomA = device.createTexture("LamiVisuals shaders bloom A", bloomUsage, format, bw, bh, 1, 1);
        bloomB = device.createTexture("LamiVisuals shaders bloom B", bloomUsage, format, bw, bh, 1, 1);
        sceneView = device.createTextureView(scene);
        workView = device.createTextureView(work);
        historyView = device.createTextureView(history);
        bloomAView = device.createTextureView(bloomA);
        bloomBView = device.createTextureView(bloomB);
        historyValid = false;
    }

    private static void release() {
        for (GpuTextureView v : new GpuTextureView[]{sceneView, workView, historyView, bloomAView, bloomBView}) if (v != null) v.close();
        for (GpuTexture t : new GpuTexture[]{scene, work, history, bloomA, bloomB}) if (t != null) t.close();
        sceneView = workView = historyView = bloomAView = bloomBView = null;
        scene = work = history = bloomA = bloomB = null;
    }

    private static GpuTextureView blackView() {
        if (black == null) {
            black = new DynamicTexture("LamiVisuals shaders (no bloom)", 1, 1, true);
            black.getPixels().setPixel(0, 0, 0xFF000000);
            black.upload();
        }
        return black.getTextureView();
    }
}
