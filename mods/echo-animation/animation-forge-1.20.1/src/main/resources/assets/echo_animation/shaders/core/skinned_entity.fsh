#version 150

uniform sampler2D Sampler0;
uniform float Light;
uniform int DebugMode;

in vec2 texCoord;
in vec3 skinnedNormal;
flat in int dominantBone;
out vec4 fragColor;

vec3 boneColor(int index) {
    float value = float(index);
    return 0.45 + 0.45 * cos(value * vec3(2.0, 2.7, 3.4) + vec3(0.0, 2.0, 4.0));
}

void main() {
    vec4 color = texture(Sampler0, texCoord);
    if (color.a < 0.1) {
        discard;
    }
    vec3 debug = boneColor(dominantBone);
    float shade = Light * (0.75 + 0.25 * abs(normalize(skinnedNormal).z));
    fragColor = vec4((DebugMode == 1 ? debug : color.rgb) * shade, color.a);
}
