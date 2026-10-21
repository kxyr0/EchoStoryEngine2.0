#version 150

in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texcoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texcoord = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
}
