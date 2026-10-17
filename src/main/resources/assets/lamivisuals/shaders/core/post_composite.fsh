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

vec3 grade(vec3 c) {
    vec3 lift = ModelViewMat[0].rgb;
    vec3 gamma = ModelViewMat[1].rgb;
    vec3 gain = ModelViewMat[2].rgb;
    float saturation = ModelViewMat[0].a;
    float contrast = ModelViewMat[1].a;
    float exposure = ModelViewMat[2].a;
    c *= exp2(exposure);
    c = gain * (c + lift * (1.0 - c));
    c = pow(max(c, vec3(0.0)), 1.0 / max(gamma, vec3(0.01)));
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(luma), c, saturation);
    c = (c - 0.5) * contrast + 0.5;
    return clamp(c, 0.0, 1.0);
}

void main() {
    vec3 c = texture(Sampler0, texCoord).rgb;
    c += texture(Sampler1, texCoord).rgb * ColorModulator.x;
    c = mix(clamp(c, 0.0, 1.0), grade(c), clamp(ColorModulator.y, 0.0, 1.0));
    float v = smoothstep(0.3, 0.95, length(texCoord - 0.5) * 1.3);
    c *= 1.0 - clamp(ColorModulator.z, 0.0, 1.0) * v;
    fragColor = vec4(c, 1.0);
}
