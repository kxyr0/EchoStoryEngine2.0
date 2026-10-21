#pragma once
#include "Common.h"
#include <DirectXMath.h>
#include "portrait/FaceRig.h"
#include <array>
namespace echo {
struct Vertex {
    DirectX::XMFLOAT3 position, normal;
    DirectX::XMFLOAT2 uv;
};
struct ModelSettings {
    bool slim = false, hat = true, jacket = true, sleeves = true, pants = true;
    float animation = 0;
};
std::vector<Vertex> buildModel(const ModelSettings &settings, bool overlay);
struct FaceMeshLayer {
    const Image *texture = nullptr;
    std::array<Vertex, 6> vertices;
    std::string id;
};
std::vector<FaceQuad> worldFaceQuads(const FaceRig &rig);
std::vector<FaceMeshLayer> buildFaceLayers(const FaceRig &rig);
} // namespace echo
