#pragma once
#include "Common.h"
#include <memory>
namespace echo {
struct Image {
    unsigned width = 0, height = 0;
    std::vector<uint8_t> rgba;
};
struct Skin {
    fs::path path;
    Image image, portrait, basePortrait;
};
bool validSkinSize(unsigned width, unsigned height);
Image loadPng(const fs::path &path, bool skinLayout = true);
void savePng(const fs::path &path, const Image &image);
Image makePortrait(const Image &image, bool hat = true);
std::shared_ptr<Skin> loadSkin(const fs::path &path);
struct EyeMask {
    std::vector<uint8_t> left, right;
    float confidence = 0;
};
EyeMask detectEyes(const Image &face);
Image maskImage(const Image &face, const std::vector<uint8_t> &mask);
} // namespace echo
