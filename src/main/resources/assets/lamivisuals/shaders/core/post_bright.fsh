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
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec3 c = texture(InSampler, texCoord + texel * vec2(-1.0, -1.0)).rgb;
    c += texture(InSampler, texCoord + texel * vec2(1.0, -1.0)).rgb;
    c += texture(InSampler, texCoord + texel * vec2(-1.0, 1.0)).rgb;
    c += texture(InSampler, texCoord + texel * vec2(1.0, 1.0)).rgb;
    c *= 0.25;
    float brightness = max(c.r, max(c.g, c.b));
    float knee = max(ColorModulator.y, 0.001);
    float w = smoothstep(ColorModulator.x - knee, ColorModulator.x + knee, brightness);
    fragColor = vec4(c * w, 1.0);
}
