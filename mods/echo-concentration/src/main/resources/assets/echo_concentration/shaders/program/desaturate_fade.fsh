#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Direction;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float luma = dot(source.rgb, vec3(0.3, 0.59, 0.11));
    float curve = smoothstep(0.0, 0.35, Time);
    float amount = mix(curve, 1.0 - curve, Direction);
    fragColor = vec4(mix(source.rgb, vec3(luma), amount), source.a);
}
