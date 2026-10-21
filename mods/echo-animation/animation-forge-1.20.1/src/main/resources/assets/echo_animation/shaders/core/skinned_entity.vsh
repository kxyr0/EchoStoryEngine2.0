#version 150

in vec3 Position;
in vec2 UV0;
in uvec4 BoneIndices;
in vec4 BoneWeights;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 BoneMatrices[48];

out vec2 texCoord;
out vec3 skinnedNormal;
flat out int dominantBone;

void main() {
    mat4 skin = BoneMatrices[int(BoneIndices.x)] * BoneWeights.x
        + BoneMatrices[int(BoneIndices.y)] * BoneWeights.y
        + BoneMatrices[int(BoneIndices.z)] * BoneWeights.z
        + BoneMatrices[int(BoneIndices.w)] * BoneWeights.w;
    gl_Position = ProjMat * ModelViewMat * skin * vec4(Position, 1.0);
    skinnedNormal = normalize(mat3(skin) * Normal);
    texCoord = UV0;
    dominantBone = int(BoneIndices.x);
    float strongest = BoneWeights.x;
    if (BoneWeights.y > strongest) {
        strongest = BoneWeights.y;
        dominantBone = int(BoneIndices.y);
    }
    if (BoneWeights.z > strongest) {
        strongest = BoneWeights.z;
        dominantBone = int(BoneIndices.z);
    }
    if (BoneWeights.w > strongest) {
        dominantBone = int(BoneIndices.w);
    }
}
