package ru.client.utils.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class LateRenderer {
    private LateRenderer() {}

    private static final class Batch {
        final Identifier texture;
        float[] p = new float[4 * 3 * 32];
        float[] uv = new float[4 * 2 * 32];
        int[] c = new int[4 * 32];
        int n;
        int firstQuad;
        AbstractTexture gpuTexture;

        Batch(Identifier texture) { this.texture = texture; }

        void append(Render3D.TexBuf b) {
            int add = b.count();
            while (n + add > c.length) {
                p = java.util.Arrays.copyOf(p, p.length * 2);
                uv = java.util.Arrays.copyOf(uv, uv.length * 2);
                c = java.util.Arrays.copyOf(c, c.length * 2);
            }
            b.copyTo(p, uv, c, n);
            n += add;
        }
    }

    private static final Map<Identifier, Batch> pending = new LinkedHashMap<>();
    private static final List<Batch> ready = new ArrayList<>();
    private static GpuBuffer vertexBuffer;
    private static int readyQuads;

    public static void beginFrame() {
        for (Batch b : pending.values()) b.n = 0;
    }

    public static void add(Identifier texture, Render3D.TexBuf buf) {
        if (buf.count() < 4) return;
        pending.computeIfAbsent(texture, Batch::new).append(buf);
    }

    public static void upload() {
        ready.clear();
        readyQuads = 0;
        int quads = 0;
        for (Batch b : pending.values()) quads += b.n / 4;
        if (quads == 0) return;

        int vertexSize = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * vertexSize)) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (Batch b : pending.values()) {
                int bq = b.n / 4;
                if (bq == 0) continue;
                b.firstQuad = readyQuads;
                for (int i = 0; i < bq * 4; i++) {
                    builder.addVertex(b.p[i * 3], b.p[i * 3 + 1], b.p[i * 3 + 2]).setUv(b.uv[i * 2], b.uv[i * 2 + 1]).setColor(b.c[i]);
                }
                b.gpuTexture = Minecraft.getInstance().getTextureManager().getTexture(b.texture);
                readyQuads += bq;
                ready.add(b);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                uploadVertices(mesh.vertexBuffer());
            }
        }
        RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(readyQuads * 6);
    }

    private static void uploadVertices(ByteBuffer data) {
        GpuDevice device = RenderSystem.getDevice();
        if (vertexBuffer == null || vertexBuffer.size() < data.remaining()) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = device.createBuffer(() -> "LamiVisuals particles", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    Math.max(data.remaining(), 64 * 1024));
        }
        device.createCommandEncoder().writeToBuffer(vertexBuffer.slice(0, data.remaining()), data);
    }

    public static void draw(RenderPass pass) {
        if (readyQuads == 0 || vertexBuffer == null) return;
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        pass.pushDebugGroup(() -> "LamiVisuals particles");
        pass.setPipeline(RenderSystem.getCompiledPipeline(Render3D.ADDITIVE_TEXTURED));
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", transforms);
        pass.setIndexBuffer(indices.getBuffer(), indices.type());
        pass.setVertexBuffer(0, vertexBuffer.slice());
        for (Batch b : ready) {
            pass.setUniform("Sampler0", b.gpuTexture.getTextureView(), sampler);
            pass.drawIndexed(b.n / 4 * 6, 1, b.firstQuad * 6, 0, 0);
        }
        pass.popDebugGroup();
        readyQuads = 0;
        ready.clear();
    }

    public static void drawOwnPass() {
        if (readyQuads == 0) return;
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "LamiVisuals particles",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            draw(pass);
        }
    }
}
