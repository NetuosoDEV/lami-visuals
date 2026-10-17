#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec3 viewDir;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265;

uvec3 pcg3(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    return v;
}

vec3 hash3(ivec3 c) {
    return vec3(pcg3(uvec3(c)) >> 8u) * (1.0 / 16777216.0);
}

float hash1(ivec3 c) {
    return float(pcg3(uvec3(c)).x >> 8u) * (1.0 / 16777216.0);
}

float hash2(ivec2 c) {
    return float(pcg3(uvec3(uvec2(c), 40503u)).x >> 8u) * (1.0 / 16777216.0);
}

float vnoise3(vec3 p) {
    vec3 fl = floor(p);
    ivec3 i = ivec3(fl);
    vec3 f = p - fl;
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash1(i);
    float n100 = hash1(i + ivec3(1, 0, 0));
    float n010 = hash1(i + ivec3(0, 1, 0));
    float n110 = hash1(i + ivec3(1, 1, 0));
    float n001 = hash1(i + ivec3(0, 0, 1));
    float n101 = hash1(i + ivec3(1, 0, 1));
    float n011 = hash1(i + ivec3(0, 1, 1));
    float n111 = hash1(i + ivec3(1, 1, 1));
    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
               mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}

float vnoise2(vec2 p) {
    vec2 fl = floor(p);
    ivec2 i = ivec2(fl);
    vec2 f = p - fl;
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash2(i);
    float b = hash2(i + ivec2(1, 0));
    float c = hash2(i + ivec2(0, 1));
    float d = hash2(i + ivec2(1, 1));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm3(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        s += a * vnoise3(p);
        p = p * 2.02 + vec3(3.1, 1.7, 4.3);
        a *= 0.5;
    }
    return s;
}

float fbm2(vec2 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        s += a * vnoise2(p);
        p = p * 2.03 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return s;
}

vec3 starLayer(vec3 d, float scale, float chance, float t, int salt) {
    vec3 p = d * scale;
    vec3 fl = floor(p);
    ivec3 cell = ivec3(fl);
    vec3 h = hash3(cell + ivec3(salt, 0, 0));
    if (h.x > chance) return vec3(0.0);
    vec3 sp = fl + 0.3 + 0.4 * hash3(cell + ivec3(17, 31 + salt, 47));
    float dist = length(p - sp);
    float radius = 0.035 + 0.05 * h.y * h.y;
    float core = max(0.0, 1.0 - dist / radius);
    core = core * core;
    float halo = max(0.0, 1.0 - dist / (radius * 2.4));
    halo = halo * halo * 0.3;
    float twinkle = 0.6 + 0.4 * sin(t * (0.8 + 2.4 * h.z) + h.y * 50.0);
    vec3 tint = mix(vec3(0.70, 0.80, 1.00), vec3(1.00, 0.86, 0.72), h.z);
    return tint * (core + halo) * twinkle * (0.9 + 2.0 * h.y);
}

vec3 stars(vec3 d, float density, float t) {
    return starLayer(d, 60.0, min(0.9, 0.45 * density), t, 0)
         + starLayer(d, 30.0, min(0.9, 0.10 * density), t * 0.7 + 3.0, 101) * 1.3;
}

vec3 skyBase(vec3 d, vec3 hor, vec3 zen) {
    if (d.y >= 0.0) return mix(hor, zen, smoothstep(0.0, 0.55, d.y));
    return mix(hor, hor * 0.45, smoothstep(0.0, 0.35, -d.y));
}

vec3 aurora(vec3 d, float t, vec3 ca, vec3 cb) {
    if (d.y < 0.02) return vec3(0.0);
    vec3 acc = vec3(0.0);
    vec2 base = d.xz / d.y;
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    for (int i = 0; i < 26; i++) {
        float fi = (float(i) + jitter) / 26.0;
        vec2 uv = base * (1.0 + fi * 1.3) * 0.35;
        float warp = fbm2(uv * 0.6 + vec2(t * 0.03, -t * 0.02)) * 3.0;
        float wave = uv.x * 0.9 + uv.y * 0.35 + warp + sin(uv.y * 1.3 + t * 0.12) * 0.6;
        float tri = 1.0 - abs(fract(wave * 0.5) * 2.0 - 1.0);
        float band = pow(tri, 9.0);
        float rays = 0.5 + 0.5 * vnoise2(vec2(wave * 9.0 + t * 0.4, fi * 3.0));
        float fall = 1.0 - fi * 0.8;
        vec3 col = mix(ca, cb, smoothstep(0.1, 0.8, fi));
        acc += col * band * rays * fall * fall;
    }
    return acc * 0.2 * smoothstep(0.02, 0.22, d.y);
}

vec3 galaxy(vec3 d, float t, vec3 ca, vec3 cb, out float band) {
    vec3 n = normalize(vec3(0.25, 0.85, 0.46));
    float lat = dot(d, n);
    band = exp(-lat * lat / 0.028);
    float clouds = fbm3(d * 3.0 + vec3(0.0, 0.0, t * 0.004));
    float dust = smoothstep(0.45, 0.75, fbm3(d * 7.0 + 11.0));
    vec3 core = normalize(vec3(0.6, 0.15, -0.78));
    core = normalize(core - n * dot(core, n));
    vec3 dc = d - core;
    float bulge = exp(-dot(dc, dc) / 0.12);
    vec3 col = mix(cb, ca, clouds) * band * (0.25 + 0.9 * clouds);
    col += mix(ca, vec3(1.0, 0.85, 0.7), 0.5) * bulge * band * 0.6;
    col *= 1.0 - dust * band * 0.75;
    col += mix(ca, cb, fbm3(d * 1.7 + 5.0)) * pow(fbm3(d * 2.2 - 7.0), 3.0) * 0.35;
    return col;
}

vec3 nebula(vec3 d, float t, vec3 ca, vec3 cb) {
    vec3 q = d * 1.8 + vec3(t * 0.006, 0.0, -t * 0.004);
    vec3 w = vec3(fbm3(q + 1.3), fbm3(q + 7.1), fbm3(q - 4.2));
    float f = fbm3(q + w * 1.6);
    float g = fbm3(q * 2.1 + w + 9.0);
    float shape = smoothstep(0.34, 0.82, f);
    vec3 col = mix(cb, ca, smoothstep(0.35, 0.7, g)) * shape * shape * 1.4;
    col += (ca + cb) * 0.5 * pow(shape, 6.0) * 0.8;
    return col;
}

vec3 sunset(vec3 d, float t, vec3 ca, vec3 cb, vec3 hor, float intensity) {
    vec3 zen = vec3(0.07, 0.05, 0.20);
    vec3 col;
    if (d.y >= 0.0) {
        col = mix(hor, cb, smoothstep(0.0, 0.22, d.y));
        col = mix(col, zen, smoothstep(0.18, 0.75, d.y));
    } else {
        col = mix(hor, hor * 0.35, smoothstep(0.0, 0.3, -d.y));
    }
    vec3 sunDir = normalize(vec3(-0.85, 0.05, -0.52));
    float g = max(dot(d, sunDir), 0.0);
    col += ca * (pow(g, 6.0) * 0.45 + pow(g, 40.0) * 0.6) * intensity;
    if (d.y > -0.02) {
        vec2 uv = d.xz / (max(d.y, 0.0) + 0.12);
        float c = smoothstep(0.4, 0.7, fbm2(uv * 0.9 + vec2(t * 0.015, t * 0.004)));
        vec3 dusk = mix(vec3(0.32, 0.14, 0.30), vec3(0.16, 0.08, 0.22), smoothstep(0.0, 0.4, d.y));
        vec3 lit = mix(dusk, ca * 1.3, clamp(pow(g, 3.0) * 0.9 + 0.15, 0.0, 1.0));
        col = mix(col, lit, c * 0.75 * intensity * (1.0 - smoothstep(0.06, 0.5, d.y)));
    }
    return col;
}

vec3 meteors(vec3 d, float t, vec3 ca, vec3 cb) {
    vec3 acc = vec3(0.0);
    for (int k = 0; k < 12; k++) {
        float fk = float(k);
        float period = 2.0 + fk * 0.41;
        float tt = t / period + fk * 0.37;
        float id = floor(tt);
        float ph = fract(tt) / 0.4;
        if (ph > 1.0) continue;
        vec3 r = hash3(ivec3(int(id), k, 7));
        float az = r.x * 2.0 * PI;
        float el = 0.3 + r.y * 0.6;
        vec3 s0 = vec3(cos(az) * cos(el), sin(el), sin(az) * cos(el));
        vec3 down = normalize(vec3(0.0, -1.0, 0.0) + s0 * s0.y);
        vec3 side = cross(s0, down);
        vec3 dir = normalize(down + side * (r.z - 0.5) * 1.6);
        vec3 nrm = cross(s0, dir);
        if (dot(d, s0) < 0.8) continue;
        float x = dot(d, dir);
        float y = dot(d, nrm);
        float head = ph * 0.45;
        float tailLen = 0.16;
        float k1 = (x - (head - tailLen)) / tailLen;
        float kc = clamp(k1, 0.0, 1.0);
        float w = 0.0012 + 0.0016 * kc;
        float mask = step(0.0, k1) * step(k1, 1.0);
        float streak = exp(-(y * y) / (w * w)) * mask * kc * kc;
        vec2 hd = vec2(x - head, y);
        float headGlow = exp(-dot(hd, hd) / 0.000012);
        float life = sin(ph * PI);
        acc += (mix(cb, ca, kc) * streak * 1.4 + vec3(1.0) * headGlow) * life;
    }
    return acc;
}

vec3 voidFx(vec3 d, float t, vec3 ca, vec3 cb) {
    float f = fbm3(d * 2.4 + vec3(0.0, t * 0.01, 0.0));
    return ca * pow(f, 4.0) * 0.5 + cb * 0.03 * (1.0 - abs(d.y));
}

vec3 emberLayer(vec3 d, float scale, float chance, float t, int salt, vec3 ca, vec3 cb) {
    vec3 p = d * scale - vec3(0.0, t * 0.9, 0.0);
    vec3 fl = floor(p);
    ivec3 cell = ivec3(fl);
    vec3 h = hash3(cell + ivec3(salt, 7, 0));
    if (h.x > chance) return vec3(0.0);
    vec3 sp = fl + 0.3 + 0.4 * hash3(cell + ivec3(5, 41 + salt, 13));
    float dist = length(p - sp);
    float radius = 0.05 + 0.06 * h.y;
    float core = max(0.0, 1.0 - dist / radius);
    float halo = max(0.0, 1.0 - dist / (radius * 2.4));
    float flicker = 0.55 + 0.45 * sin(t * (6.0 + 8.0 * h.z) + h.y * 30.0);
    return mix(cb, ca * 1.4, h.z) * (core * core + halo * halo * 0.35) * flicker * 2.6;
}

vec3 lava(vec3 d, float t, vec3 ca, vec3 cb, vec3 hor, float intensity) {
    float up = max(d.y, 0.0);
    vec3 col = skyBase(d, hor, hor * 0.15);
    vec3 q = d * 2.2 + vec3(t * 0.02, -t * 0.015, 0.0);
    float smoke = fbm3(q + fbm3(q * 1.7) * 0.8);
    float glow = exp(-up * 3.5);
    vec3 fx = mix(cb * 0.25, ca * 0.9, glow) * smoothstep(0.35, 0.85, smoke) * (0.4 + 0.8 * glow);
    fx += ca * exp(-up * 9.0) * 0.45;
    vec3 embers = emberLayer(d, 40.0, 0.32, t, 0, ca, cb) + emberLayer(d, 22.0, 0.12, t * 0.8, 57, ca, cb);
    fx += embers * (1.0 - smoothstep(0.0, 0.85, d.y));
    return col + fx * intensity * smoothstep(-0.05, 0.1, d.y);
}

vec3 synthwave(vec3 d, float t, vec3 ca, vec3 cb, vec3 hor, float intensity) {
    vec3 zen = vec3(0.04, 0.015, 0.10);
    vec3 col;
    if (d.y >= 0.0) {
        col = mix(hor, ca * 0.45, smoothstep(0.0, 0.12, d.y));
        col = mix(col, zen, smoothstep(0.08, 0.6, d.y));
        vec3 sunDir = normalize(vec3(0.0, 0.13, -1.0));
        float ang = acos(clamp(dot(d, sunDir), -1.0, 1.0));
        float r = 0.2;
        col += ca * exp(-ang * ang / 0.06) * 0.35 * intensity;
        if (ang < r) {
            float v = (d.y - sunDir.y) / r;
            vec3 sunCol = mix(ca * 1.1, vec3(1.0, 0.86, 0.35), smoothstep(-0.8, 0.7, v));
            float stripes = 1.0;
            if (v < 0.15) {
                float s = fract(v * 7.0 - t * 0.12);
                stripes = step(mix(0.12, 0.55, clamp(-v, 0.0, 1.0)), s);
            }
            float edge = 1.0 - smoothstep(r - 0.004, r, ang);
            col = mix(col, sunCol, edge * stripes * min(1.0, intensity));
        }
        col += stars(d, 0.8, t) * smoothstep(0.2, 0.7, d.y) * 0.6 * intensity;
    } else {
        float down = -d.y;
        col = mix(hor * 0.6, vec3(0.015, 0.0, 0.035), smoothstep(0.0, 0.35, down));
        vec2 uv = d.xz / max(down, 0.02) * 0.5;
        uv.y += t * 0.6;
        vec2 cellUv = abs(fract(uv) - 0.5);
        vec2 w = fwidth(uv) * 1.2;
        vec2 lines = 1.0 - smoothstep(vec2(0.0), w, 0.5 - cellUv);
        float grid = max(lines.x, lines.y);
        float fade = smoothstep(0.0, 0.12, down) * exp(-length(uv) * 0.03);
        col += mix(cb, ca, 0.35) * grid * fade * 1.3 * intensity;
        col += ca * exp(-down * 18.0) * 0.35 * intensity;
    }
    return col;
}

vec3 storm(vec3 d, float t, vec3 ca, vec3 cb, vec3 hor, float intensity) {
    float up = max(d.y, 0.0);
    vec3 col = skyBase(d, hor, hor * 0.55);
    vec2 uv = d.xz / (up + 0.15);
    float c1 = fbm2(uv * 0.6 + vec2(t * 0.05, t * 0.03));
    float c2 = fbm2(uv * 1.3 - vec2(t * 0.08, 0.0));
    float cloud = smoothstep(0.3, 0.75, c1 * 0.7 + c2 * 0.5) * smoothstep(-0.05, 0.2, d.y);
    col = mix(col, cb * (0.45 + 0.55 * c2), cloud);
    float period = 2.6;
    float id = floor(t / period);
    float ph = (t / period - id) * period;
    if (ph < 0.7) {
        vec3 r = hash3(ivec3(int(id), 99, 5));
        float az = r.x * 2.0 * PI;
        float el = 0.15 + r.y * 0.4;
        vec3 fdir = vec3(cos(az) * cos(el), sin(el), sin(az) * cos(el));
        float flick = exp(-ph * 9.0) + 0.7 * exp(-abs(ph - 0.2) * 22.0);
        vec3 dd = d - fdir;
        float near = exp(-dot(dd, dd) / 0.45);
        col += ca * flick * (near * (0.3 + cloud * 1.4) + 0.12 + cloud * 0.25) * intensity;
        if (r.z > 0.35 && dot(d, fdir) > 0.6) {
            vec3 side = normalize(cross(vec3(0.0, 1.0, 0.0), fdir));
            float x = dot(dd, side);
            float y = d.y - fdir.y;
            if (y < 0.0 && d.y > -0.02) {
                float jag = (fbm2(vec2(y * 18.0, id)) - 0.5) * 0.12 + (vnoise2(vec2(y * 70.0, id * 3.0)) - 0.5) * 0.02;
                float dist = (x - jag) / 0.0016;
                float bolt = exp(-dist * dist);
                float halo = exp(-dist * dist / 60.0) * 0.35;
                col += (vec3(1.0) * bolt + ca * halo) * flick * intensity;
            }
        }
    }
    return col;
}

void main() {
    vec3 d = normalize(viewDir);
    float t = TextureMat[0].x;
    int mode = int(TextureMat[0].y + 0.5);
    float opacity = TextureMat[0].z;
    float intensity = TextureMat[0].w;
    vec3 ca = TextureMat[1].xyz;
    vec3 cb = TextureMat[2].xyz;
    vec3 hor = TextureMat[3].xyz;
    float above = smoothstep(-0.02, 0.16, d.y);

    vec3 col;
    if (mode == 3) {
        col = sunset(d, t, ca, cb, hor, intensity);
        col += stars(d, 0.6, t) * smoothstep(0.45, 0.95, d.y) * 0.35 * intensity;
    } else if (mode == 6) {
        col = lava(d, t, ca, cb, hor, intensity);
    } else if (mode == 7) {
        col = synthwave(d, t, ca, cb, hor, intensity);
    } else if (mode == 8) {
        col = storm(d, t, ca, cb, hor, intensity);
    } else {
        col = skyBase(d, hor, mode == 5 ? vec3(0.0) : hor * 0.22 + cb * 0.02);
        vec3 fx = vec3(0.0);
        float density = 1.0;
        if (mode == 0) {
            fx = aurora(d, t, ca, cb);
        } else if (mode == 1) {
            float band;
            fx = galaxy(d, t, ca, cb, band);
            density = 1.0 + 1.6 * band;
        } else if (mode == 2) {
            fx = nebula(d, t, ca, cb);
            density = 0.8;
        } else if (mode == 4) {
            fx = meteors(d, t, ca, cb);
            density = 2.0;
        } else {
            fx = voidFx(d, t, ca, cb);
            density = 0.35;
        }
        col += (fx + stars(d, density, t)) * above * intensity;
    }

    col += (hash1(ivec3(ivec2(gl_FragCoord.xy), int(fract(t) * 64.0))) - 0.5) / 255.0;
    fragColor = vec4(max(col, vec3(0.0)), opacity);
}
