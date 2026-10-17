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
    if (ColorModulator.z > 0.5) {
        vec3 c = texture(InSampler, texCoord + texel * vec2(-1.0, -1.0)).rgb;
        c += texture(InSampler, texCoord + texel * vec2(1.0, -1.0)).rgb;
        c += texture(InSampler, texCoord + texel * vec2(-1.0, 1.0)).rgb;
        c += texture(InSampler, texCoord + texel * vec2(1.0, 1.0)).rgb;
        fragColor = vec4(c * 0.25, 1.0);
        return;
    }
    vec2 dir = ColorModulator.xy * texel;
    vec3 c = texture(InSampler, texCoord).rgb * 0.2270270270;
    c += texture(InSampler, texCoord + dir * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord - dir * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord + dir * 3.2307692308).rgb * 0.0702702703;
    c += texture(InSampler, texCoord - dir * 3.2307692308).rgb * 0.0702702703;
    fragColor = vec4(c, 1.0);
}
