package ru.client.utils.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class GlbModel implements AutoCloseable {

    public final float[] pos, uv, normal;
    public final int vertexCount;
    public final NativeImage texture;
    public final Vector3f min, max;

    private GlbModel(float[] pos, float[] uv, float[] normal, int vertexCount, NativeImage texture) {
        this.pos = pos;
        this.uv = uv;
        this.normal = normal;
        this.vertexCount = vertexCount;
        this.texture = texture;
        min = new Vector3f(Float.MAX_VALUE);
        max = new Vector3f(-Float.MAX_VALUE);
        for (int i = 0; i < vertexCount; i++) {
            min.min(new Vector3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]));
            max.max(new Vector3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]));
        }
    }

    @Override
    public void close() { texture.close(); }

    public static GlbModel read(InputStream in) throws IOException {
        ByteBuffer file = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        if (file.remaining() < 20 || file.getInt(0) != 0x46546C67) throw new IOException("не .glb (нет заголовка glTF)");
        if (file.getInt(4) != 2) throw new IOException("нужен glTF 2.0");
        JsonObject json = null;
        ByteBuffer bin = null;
        int off = 12;
        while (off + 8 <= file.limit()) {
            int len = file.getInt(off), type = file.getInt(off + 4);
            ByteBuffer chunk = file.slice(off + 8, len).order(ByteOrder.LITTLE_ENDIAN);
            if (type == 0x4E4F534A) {
                byte[] b = new byte[len];
                chunk.get(b);
                json = JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
            } else if (type == 0x004E4942) {
                bin = chunk;
            }
            off += 8 + ((len + 3) & ~3);
        }
        if (json == null || bin == null) throw new IOException("в .glb нет JSON или BIN");
        return new Parser(json, bin).parse();
    }

    private static final class Parser {
        private final JsonObject json;
        private final ByteBuffer bin;
        private float[] pos = new float[3 * 1024], uv = new float[2 * 1024], nrm = new float[3 * 1024];
        private int n;

        Parser(JsonObject json, ByteBuffer bin) {
            this.json = json;
            this.bin = bin;
        }

        GlbModel parse() throws IOException {
            JsonArray scenes = json.getAsJsonArray("scenes");
            int scene = json.has("scene") ? json.get("scene").getAsInt() : 0;
            for (JsonElement root : scenes.get(scene).getAsJsonObject().getAsJsonArray("nodes")) {
                node(root.getAsInt(), new Matrix4f());
            }
            if (n == 0) throw new IOException("в модели нет треугольников");
            return new GlbModel(java.util.Arrays.copyOf(pos, n * 3), java.util.Arrays.copyOf(uv, n * 2),
                    java.util.Arrays.copyOf(nrm, n * 3), n, image());
        }

        private void node(int index, Matrix4f parent) throws IOException {
            JsonObject node = json.getAsJsonArray("nodes").get(index).getAsJsonObject();
            Matrix4f local = new Matrix4f();
            if (node.has("matrix")) {
                float[] m = floats(node.getAsJsonArray("matrix"));
                local.set(m);
            } else {
                if (node.has("translation")) {
                    float[] t = floats(node.getAsJsonArray("translation"));
                    local.translate(t[0], t[1], t[2]);
                }
                if (node.has("rotation")) {
                    float[] q = floats(node.getAsJsonArray("rotation"));
                    local.rotate(new Quaternionf(q[0], q[1], q[2], q[3]));
                }
                if (node.has("scale")) {
                    float[] s = floats(node.getAsJsonArray("scale"));
                    local.scale(s[0], s[1], s[2]);
                }
            }
            Matrix4f world = new Matrix4f(parent).mul(local);
            if (node.has("mesh")) mesh(node.get("mesh").getAsInt(), world);
            if (node.has("children")) {
                for (JsonElement c : node.getAsJsonArray("children")) node(c.getAsInt(), world);
            }
        }

        private void mesh(int index, Matrix4f world) throws IOException {
            Matrix3f normalMat = new Matrix3f(world).invert().transpose();
            JsonObject mesh = json.getAsJsonArray("meshes").get(index).getAsJsonObject();
            for (JsonElement pe : mesh.getAsJsonArray("primitives")) {
                JsonObject prim = pe.getAsJsonObject();
                if (prim.has("mode") && prim.get("mode").getAsInt() != 4) continue;
                JsonObject attrs = prim.getAsJsonObject("attributes");
                float[] p = accessorFloats(attrs.get("POSITION").getAsInt(), 3);
                int count = p.length / 3;
                float[] t = attrs.has("TEXCOORD_0") ? accessorFloats(attrs.get("TEXCOORD_0").getAsInt(), 2) : new float[count * 2];
                float[] nn = attrs.has("NORMAL") ? accessorFloats(attrs.get("NORMAL").getAsInt(), 3) : null;
                int[] idx = prim.has("indices") ? accessorInts(prim.get("indices").getAsInt()) : sequence(count);
                for (int i = 0; i + 2 < idx.length; i += 3) {
                    Vector3f[] v = new Vector3f[3];
                    for (int k = 0; k < 3; k++) {
                        int j = idx[i + k];
                        v[k] = world.transformPosition(new Vector3f(p[j * 3], p[j * 3 + 1], p[j * 3 + 2]));
                    }
                    Vector3f face = new Vector3f(v[1]).sub(v[0]).cross(new Vector3f(v[2]).sub(v[0]));
                    if (face.lengthSquared() > 1e-12f) face.normalize();
                    for (int k = 0; k < 3; k++) {
                        int j = idx[i + k];
                        Vector3f nv = nn != null ? normalMat.transform(new Vector3f(nn[j * 3], nn[j * 3 + 1], nn[j * 3 + 2])) : new Vector3f(face);
                        if (nv.lengthSquared() > 1e-12f) nv.normalize();
                        put(v[k], t[j * 2], t[j * 2 + 1], nv);
                    }
                }
            }
        }

        private void put(Vector3f p, float u, float v, Vector3f nv) {
            if (n * 3 + 3 > pos.length) {
                pos = java.util.Arrays.copyOf(pos, pos.length * 2);
                nrm = java.util.Arrays.copyOf(nrm, nrm.length * 2);
                uv = java.util.Arrays.copyOf(uv, uv.length * 2);
            }
            pos[n * 3] = p.x; pos[n * 3 + 1] = p.y; pos[n * 3 + 2] = p.z;
            uv[n * 2] = u; uv[n * 2 + 1] = v;
            nrm[n * 3] = nv.x; nrm[n * 3 + 1] = nv.y; nrm[n * 3 + 2] = nv.z;
            n++;
        }

        private NativeImage image() throws IOException {
            if (!json.has("images")) throw new IOException("в модели нет встроенной текстуры (Embed Textures при экспорте)");
            JsonObject img = json.getAsJsonArray("images").get(0).getAsJsonObject();
            if (!img.has("bufferView")) throw new IOException("текстура не встроена в .glb");
            ByteBuffer view = bufferView(img.get("bufferView").getAsInt());
            byte[] png = new byte[view.remaining()];
            view.get(png);
            NativeImage image = NativeImage.read(png);
            if (image.format() != NativeImage.Format.RGBA) {
                NativeImage rgba = new NativeImage(image.getWidth(), image.getHeight(), false);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) rgba.setPixel(x, y, image.getPixel(x, y));
                }
                image.close();
                image = rgba;
            }
            return image;
        }

        private ByteBuffer bufferView(int index) {
            JsonObject bv = json.getAsJsonArray("bufferViews").get(index).getAsJsonObject();
            int off = bv.has("byteOffset") ? bv.get("byteOffset").getAsInt() : 0;
            return bin.slice(off, bv.get("byteLength").getAsInt()).order(ByteOrder.LITTLE_ENDIAN);
        }

        private float[] accessorFloats(int index, int comps) throws IOException {
            JsonObject acc = json.getAsJsonArray("accessors").get(index).getAsJsonObject();
            int type = acc.get("componentType").getAsInt(), count = acc.get("count").getAsInt();
            boolean normalized = acc.has("normalized") && acc.get("normalized").getAsBoolean();
            int size = switch (type) {
                case 5126 -> 4;
                case 5123 -> 2;
                case 5121 -> 1;
                default -> throw new IOException("неподдерживаемый тип вершинных данных " + type);
            };
            if (type != 5126 && !normalized) throw new IOException("ненормализованные целые в вершинах не поддерживаются");
            JsonObject bv = json.getAsJsonArray("bufferViews").get(acc.get("bufferView").getAsInt()).getAsJsonObject();
            int stride = bv.has("byteStride") ? bv.get("byteStride").getAsInt() : size * comps;
            ByteBuffer buf = bufferView(acc.get("bufferView").getAsInt());
            int base = acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0;
            float[] out = new float[count * comps];
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < comps; c++) {
                    int at = base + i * stride + c * size;
                    out[i * comps + c] = switch (type) {
                        case 5126 -> buf.getFloat(at);
                        case 5123 -> (buf.getShort(at) & 0xFFFF) / 65535f;
                        default -> (buf.get(at) & 0xFF) / 255f;
                    };
                }
            }
            return out;
        }

        private int[] accessorInts(int index) throws IOException {
            JsonObject acc = json.getAsJsonArray("accessors").get(index).getAsJsonObject();
            int type = acc.get("componentType").getAsInt(), count = acc.get("count").getAsInt();
            ByteBuffer buf = bufferView(acc.get("bufferView").getAsInt());
            int base = acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0;
            int[] out = new int[count];
            for (int i = 0; i < count; i++) {
                out[i] = switch (type) {
                    case 5125 -> buf.getInt(base + i * 4);
                    case 5123 -> buf.getShort(base + i * 2) & 0xFFFF;
                    case 5121 -> buf.get(base + i) & 0xFF;
                    default -> throw new IOException("неподдерживаемый тип индексов " + type);
                };
            }
            return out;
        }

        private static int[] sequence(int count) {
            int[] out = new int[count];
            for (int i = 0; i < count; i++) out[i] = i;
            return out;
        }

        private static float[] floats(JsonArray a) {
            float[] out = new float[a.size()];
            for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
            return out;
        }
    }
}
