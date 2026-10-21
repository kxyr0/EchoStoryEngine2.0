#version 150

uniform sampler2D DiffuseSampler;
uniform float Amount;
uniform vec2 Center;
uniform vec2 OutSize;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float luma = dot(source.rgb, vec3(0.3, 0.59, 0.11));
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 scale = vec2(aspect, 1.0);
    float distanceFromPlayer = length((texCoord - Center) * scale);
    float maximumRadius = length(max(Center, vec2(1.0) - Center) * scale);
    float feather = 0.065;
    float progress = clamp(Amount, 0.0, 1.0);
    float radius = progress * (maximumRadius + feather) - feather;
    float wave = 1.0 - smoothstep(radius, radius + feather, distanceFromPlayer);
    fragColor = vec4(mix(source.rgb, vec3(luma), wave), source.a);
}
