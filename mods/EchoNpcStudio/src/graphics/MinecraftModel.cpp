#include "graphics/MinecraftModel.h"
#include <array>
namespace echo {
using namespace DirectX;
static void box(std::vector<Vertex> &vertices, float cx, float cy, float cz, float w, float h, float d,
                float u, float v, float inflate = 0, float swing = 0, float pivot = 0) {
    float x0 = cx - w / 2 - inflate, x1 = cx + w / 2 + inflate, y0 = cy - h / 2 - inflate,
          y1 = cy + h / 2 + inflate, z0 = cz - d / 2 - inflate, z1 = cz + d / 2 + inflate;
    auto face = [&](std::array<XMFLOAT3, 4> p, XMFLOAT3 n, float a, float b, float uw, float vh) {
        std::array<XMFLOAT2, 4> uv = {XMFLOAT2{a / 64, b / 64}, XMFLOAT2{(a + uw) / 64, b / 64},
                                      XMFLOAT2{(a + uw) / 64, (b + vh) / 64},
                                      XMFLOAT2{a / 64, (b + vh) / 64}};
        float c = std::cos(swing), s = std::sin(swing);
        for (auto &q : p) {
            float y = q.y - pivot, z = q.z;
            q.y = y * c - z * s + pivot;
            q.z = y * s + z * c;
        }
        float ny = n.y, nz = n.z;
        n.y = ny * c - nz * s;
        n.z = ny * s + nz * c;
        for (int i : {0, 1, 2, 0, 2, 3})
            vertices.push_back({p[i], n, uv[i]});
    };
    // Each quad starts at the top-left of its texture when viewed from outside.
    face({XMFLOAT3{x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}}, {0, 0, -1}, u + d, v + d, w, h);
    face({XMFLOAT3{x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}}, {0, 0, 1}, u + 2 * d + w, v + d, w,
         h);
    face({XMFLOAT3{x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}}, {-1, 0, 0}, u, v + d, d, h);
    face({XMFLOAT3{x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}}, {1, 0, 0}, u + d + w, v + d, d, h);
    face({XMFLOAT3{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}}, {0, 1, 0}, u + d, v, w, d);
    face({XMFLOAT3{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}}, {0, -1, 0}, u + d + w, v, w, d);
}
std::vector<Vertex> buildModel(const ModelSettings &s, bool overlay) {
    std::vector<Vertex> out;
    out.reserve(216);
    float a = std::sin(s.animation) * .065f, w = s.slim ? 3.f : 4.f, arm = 4 + w / 2;
    if (!overlay) {
        box(out, 0, 28, 0, 8, 8, 8, 0, 0);
        box(out, 0, 18, 0, 8, 12, 4, 16, 16);
        box(out, -arm, 18, 0, w, 12, 4, 40, 16, 0, a, 24);
        box(out, arm, 18, 0, w, 12, 4, 32, 48, 0, -a, 24);
        box(out, -2, 6, 0, 4, 12, 4, 0, 16, 0, -a, 12);
        box(out, 2, 6, 0, 4, 12, 4, 16, 48, 0, a, 12);
    } else {
        if (s.hat)
            box(out, 0, 28, 0, 8, 8, 8, 32, 0, .5f);
        if (s.jacket)
            box(out, 0, 18, 0, 8, 12, 4, 16, 32, .25f);
        if (s.sleeves) {
            box(out, -arm, 18, 0, w, 12, 4, 40, 32, .25f, a, 24);
            box(out, arm, 18, 0, w, 12, 4, 48, 48, .25f, -a, 24);
        }
        if (s.pants) {
            box(out, -2, 6, 0, 4, 12, 4, 0, 32, .25f, -a, 12);
            box(out, 2, 6, 0, 4, 12, 4, 0, 48, .25f, a, 12);
        }
    }
    return out;
}

std::vector<FaceQuad> worldFaceQuads(const FaceRig &rig) {
    auto quads = faceQuads(rig, 0, 256, false);
    for (auto &q : quads) {
        if (!q.id.starts_with("pupil_"))
            continue;
        auto id = std::string("sclera_") + q.id.substr(6);
        auto it = std::find_if(quads.begin(), quads.end(), [&](auto &surface) { return surface.id == id; });
        if (it == quads.end())
            continue;
        auto &surface = *it;
        // Echo Animation FaceLayerPoseProjector clamps a pupil that fits in its sclera.
        if (q.w <= surface.w && q.h <= surface.h) {
            q.x = std::clamp(q.x, surface.x, surface.x + surface.w - q.w);
            q.y = std::clamp(q.y, surface.y, surface.y + surface.h - q.h);
        } else {
            double x0 = std::max(q.x, surface.x), y0 = std::max(q.y, surface.y),
                   x1 = std::min(q.x + q.w, surface.x + surface.w),
                   y1 = std::min(q.y + q.h, surface.y + surface.h);
            if (x1 <= x0 || y1 <= y0) {
                q.visible = false;
                continue;
            }
            int tx0 = int(std::floor((x0 - q.x) / q.w * q.area[2])),
                ty0 = int(std::floor((y0 - q.y) / q.h * q.area[3])),
                tx1 = int(std::ceil((x1 - q.x) / q.w * q.area[2])),
                ty1 = int(std::ceil((y1 - q.y) / q.h * q.area[3]));
            q.area = {q.area[0] + tx0, q.area[1] + ty0, tx1 - tx0, ty1 - ty0};
            q.w = q.area[2] / 8.;
            q.h = q.area[3] / 8.;
            q.x = (x0 + x1 - q.w) / 2;
            q.y = (y0 + y1 - q.h) / 2;
        }
    }
    return quads;
}
std::vector<FaceMeshLayer> buildFaceLayers(const FaceRig &rig) {
    auto quads = worldFaceQuads(rig);
    std::stable_sort(quads.begin(), quads.end(), [](auto &a, auto &b) { return a.order < b.order; });
    std::vector<FaceMeshLayer> layers;
    for (auto &q : quads) {
        if (!q.visible)
            continue;
        auto &part = rig.parts[q.part];
        int plane = part.kind == "Pupil"     ? 1
                    : part.kind == "Eyelid"  ? (q.id.starts_with("lower_eyelid") ? 3 : 2)
                    : part.kind == "Brow"    ? 4
                    : part.kind == "Glasses" ? 5
                                             : 0;
        float x0 = float(q.x * 8 - 4), x1 = float((q.x + q.w) * 8 - 4), y0 = float(32 - q.y * 8),
              y1 = float(32 - (q.y + q.h) * 8),
              z = -4.03f - float(plane * .016 + std::max(0., part.position[2]) * .016);
        float u0 = float(q.area[0]) / part.texture.width,
              u1 = float(q.area[0] + q.area[2]) / part.texture.width,
              v0 = float(q.area[1]) / part.texture.height,
              v1 = float(q.area[1] + q.area[3]) / part.texture.height;
        std::array<Vertex, 4> corners{{{{x0, y0, z}, {0, 0, -1}, {u0, v0}},
                                       {{x1, y0, z}, {0, 0, -1}, {u1, v0}},
                                       {{x1, y1, z}, {0, 0, -1}, {u1, v1}},
                                       {{x0, y1, z}, {0, 0, -1}, {u0, v1}}}};
        FaceMeshLayer layer;
        layer.id = q.id;
        layer.texture = &part.texture;
        const int indices[] = {0, 1, 2, 0, 2, 3};
        for (int i = 0; i < 6; ++i)
            layer.vertices[i] = corners[indices[i]];
        layers.push_back(layer);
    }
    return layers;
}
} // namespace echo
