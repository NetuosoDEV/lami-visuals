package ru.client.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

public final class GuiMesh {

    private float[] xy = new float[64];
    private int[] col = new int[32];
    private int n;
    private float minX, minY, maxX, maxY;

    public GuiMesh() { reset(); }

    private void reset() {
        n = 0;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    private void v(float x, float y, int c) {
        if (n == col.length) {
            xy = java.util.Arrays.copyOf(xy, xy.length * 2);
            col = java.util.Arrays.copyOf(col, col.length * 2);
        }
        xy[n * 2] = x;
        xy[n * 2 + 1] = y;
        col[n] = c;
        n++;
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
    }

    public GuiMesh quad(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
        float area = (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y4 - x4 * y3) + (x4 * y1 - x1 * y4);
        if (area > 0) {
            v(x1, y1, c1); v(x4, y4, c4); v(x3, y3, c3); v(x2, y2, c2);
        } else {
            v(x1, y1, c1); v(x2, y2, c2); v(x3, y3, c3); v(x4, y4, c4);
        }
        return this;
    }

    public GuiMesh tri(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        return quad(x1, y1, c1, x2, y2, c2, x3, y3, c3, x3, y3, c3);
    }

    private boolean stripHas;
    private float sax, say, sbx, sby;
    private int sac, sbc;

    public GuiMesh stripStart() { stripHas = false; return this; }

    public GuiMesh strip(float ax, float ay, int ac, float bx, float by, int bc) {
        if (stripHas) quad(sax, say, sac, sbx, sby, sbc, bx, by, bc, ax, ay, ac);
        sax = ax; say = ay; sac = ac; sbx = bx; sby = by; sbc = bc;
        stripHas = true;
        return this;
    }

    public boolean isEmpty() { return n == 0; }

    public void submit(GuiGraphicsExtractor ctx) {
        if (n == 0) return;
        Matrix3x2f pose = new Matrix3x2f(ctx.pose());
        ScreenRectangle scissor = ctx.scissorStack.peek();
        int x0 = (int) Math.floor(minX), y0 = (int) Math.floor(minY);
        int x1 = (int) Math.ceil(maxX), y1 = (int) Math.ceil(maxY);
        ScreenRectangle b = new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0)).transformMaxBounds(pose);
        if (scissor != null) b = scissor.intersection(b);
        if (b != null) {
            ctx.guiRenderState.addGuiElement(new State(pose, java.util.Arrays.copyOf(xy, n * 2), java.util.Arrays.copyOf(col, n), n, scissor, b));
        }
        reset();
    }

    private record State(Matrix3x2f pose, float[] xy, int[] col, int count,
                         @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int i = 0; i < count; i++) {
                vc.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(col[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() { return RenderPipelines.GUI; }

        @Override
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
    }
}
