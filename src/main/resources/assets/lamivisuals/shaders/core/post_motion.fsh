#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 current = texture(Sampler0, texCoord).rgb;
    vec3 history = texture(Sampler1, texCoord).rgb;
    fragColor = vec4(mix(current, history, clamp(ColorModulator.x, 0.0, 0.97)), 1.0);
}
