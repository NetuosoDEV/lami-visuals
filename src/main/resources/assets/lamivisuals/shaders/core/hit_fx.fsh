#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    float s = clamp(ColorModulator.x, 0.0, 2.0);
    vec2 c = texCoord - 0.5;
    vec2 uv = 0.5 + c * (1.0 - 0.008 * s);
    vec2 dir = c * (0.016 * s) * (0.4 + 1.6 * dot(c, c) * 2.0);
    float r = texture(InSampler, uv + dir).r;
    float g = texture(InSampler, uv).g;
    float b = texture(InSampler, uv - dir).b;
    fragColor = vec4(r, g, b, 1.0);
}
