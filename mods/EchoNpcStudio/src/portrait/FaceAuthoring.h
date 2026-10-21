#pragma once
#include "npc/Repository.h"
namespace echo {
struct FaceCrop {
    int x = 0, y = 0, w = 1, h = 1;
    bool operator==(const FaceCrop &) const = default;
};
struct EyelidDraft {
    double offset = 0, travel = 0; // Logical pixel offset; 0 travel uses aperture default.
    int height = 1;
    bool operator==(const EyelidDraft &) const = default;
};
struct FaceDraft {
    std::shared_ptr<Skin> source;
    Image face;                    // Logical 8x8 Minecraft face; coordinates below use this grid.
    std::array<FaceCrop, 6> crops; // Sclera L/R, pupil L/R, brow L/R.
    std::array<EyelidDraft, 2> lids{};
    bool brows = true;
    int aperture = 2;
    int fillX = 3, fillY = 7;
    float confidence = 0;
};
struct FacePackage {
    Image skin, portrait;
    std::map<std::wstring, Image> textures;
    std::string rig, profile, npc;
};
FaceDraft detectFaceDraft(std::shared_ptr<Skin> source);
void setEyeHeight(FaceDraft &draft, int height);
void validateFaceDraft(const FaceDraft &draft);
FacePackage prepareFacePackage(const FaceDraft &draft, const std::wstring &id, const std::wstring &name,
                               const std::wstring &color, double closure = 0);
Npc writeFacePackage(const FaceDraft &draft, const fs::path &pack, const std::wstring &id,
                     const std::wstring &name, const std::wstring &color, bool slim,
                     const Npc *current = nullptr);
} // namespace echo
