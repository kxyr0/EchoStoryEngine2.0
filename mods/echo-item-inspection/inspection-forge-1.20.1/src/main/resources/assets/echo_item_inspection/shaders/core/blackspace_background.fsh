#version 150

in vec2 texcoord;
out vec4 fragColor;

// Echo Blackspace bundled final.fsh default palette and smoothstep gradient.
// Darkening is local to this background, leaving item and text lighting intact.
void main() {
    float horizontal = smoothstep(0.0, 1.0, clamp(texcoord.x, 0.0, 1.0));
    float y = clamp(texcoord.y, 0.0, 1.0);
    vec3 top = mix(vec3(0.435, 0.275, 0.490), vec3(0.624, 0.412, 0.675), horizontal);
    vec3 middle = mix(vec3(0.600, 0.388, 0.651), vec3(0.800, 0.561, 0.839), horizontal);
    vec3 bottom = mix(vec3(0.506, 0.322, 0.553), vec3(0.729, 0.498, 0.773), horizontal);
    vec3 lower = mix(bottom, middle, smoothstep(0.0, 0.58, y));
    vec3 upper = mix(middle, top, smoothstep(0.58, 1.0, y));
    fragColor = vec4((y < 0.58 ? lower : upper) * 0.32, 1.0);
}
