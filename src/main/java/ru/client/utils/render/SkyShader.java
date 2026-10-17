package ru.client.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.client.core.ClientMod;
import ru.client.modules.Modules;
import ru.client.modules.impl.render.Ambience;

public final class SkyShader {
    private SkyShader() {}

    public static final RenderPipeline PIPELINE = RenderPipelines.registerOptional(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "pipeline/sky_shader"))
                    .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withVertexShader(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "core/sky_shader"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(ClientMod.MOD_ID, "core/sky_shader"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withCull(false)
                    .build());

    private static GpuBuffer cube;
    private static boolean failed;
    private static long lastNs = System.nanoTime();
    private static double time;

    public static void init() {}

    public static boolean active() {
        Ambience a = Modules.enabled(Ambience.class);
        return !failed && a != null && a.skyParams() != null;
    }

    public static void prepare() {
        if (cube != null || failed) return;
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(36 * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION);
            float s = 10f;
            float[][] faces = {
                    {-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s}, {-s, -s, s, s, -s, s, s, s, s, -s, s, s},
                    {-s, -s, -s, -s, s, -s, -s, s, s, -s, -s, s}, {s, -s, -s, s, s, -s, s, s, s, s, -s, s},
                    {-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s}, {-s, s, -s, s, s, -s, s, s, s, -s, s, s}};
            for (float[] f : faces) {
                int[] order = {0, 1, 2, 0, 2, 3};
                for (int i : order) b.addVertex(f[i * 3], f[i * 3 + 1], f[i * 3 + 2]);
            }
            try (MeshData mesh = b.buildOrThrow()) {
                cube = RenderSystem.getDevice().createBuffer(() -> "LamiVisuals sky cube", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    public static void draw(RenderPass pass) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNs) / 1e9, 0.1);
        lastNs = now;
        if (failed || cube == null) return;
        Ambience a = Modules.enabled(Ambience.class);
        Ambience.SkyParams p = a == null ? null : a.skyParams();
        if (p == null) return;
        CompiledRenderPipeline compiled = RenderSystem.getCompiledPipelineNullable(PIPELINE);
        if (compiled == null) {
            failed = true;
            ClientMod.LOGGER.warn("Sky Shader не скомпилировался — остаётся ванильное небо");
            return;
        }
        time = (time + dt * p.speed()) % 3600.0;

        int ca = p.colorA(), cb = p.colorB(), h = p.horizon();
        Matrix4f params = new Matrix4f(
                (float) time, p.mode(), p.opacity(), p.intensity(),
                ColorUtils.red(ca) / 255f, ColorUtils.green(ca) / 255f, ColorUtils.blue(ca) / 255f, 0,
                ColorUtils.red(cb) / 255f, ColorUtils.green(cb) / 255f, ColorUtils.blue(cb) / 255f, 0,
                ColorUtils.red(h) / 255f, ColorUtils.green(h) / 255f, ColorUtils.blue(h) / 255f, 0);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1, 1, 1, 1), new Vector3f(), params);
        pass.pushDebugGroup(() -> "LamiVisuals sky");
        pass.setPipeline(compiled);
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", transforms);
        pass.setVertexBuffer(0, cube.slice());
        pass.draw(36, 1, 0, 0);
        pass.popDebugGroup();
    }
}
