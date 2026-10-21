#pragma once
#include "skin/Skin.h"
#include <map>
#include <array>
#include <optional>
namespace echo {
struct SourceCall {
    size_t start = 0, end = 0;
};
std::map<std::string, std::string> scriptStringCalls(const std::string &source);
std::string patchScriptStrings(const std::string &source, const std::map<std::string, std::string> &values);
struct FacePart {
    std::string id, kind, variant, textureId;
    std::array<double, 3> position{}, pivot{};
    std::array<int, 4> area{};
    int order = 0;
    bool visible = true;
    Image texture;
    fs::path texturePath;
    size_t insert = 0;
    std::map<std::string, SourceCall> calls;
};
struct PortraitLayer {
    std::map<std::string, double> values;
    std::map<std::string, SourceCall> spans;
    size_t insert = 0;
};
struct PortraitProfile {
    fs::path path;
    std::string source;
    std::wstring id;
    double centerX = .5, centerY = .5, offsetX = 0, offsetY = 0, facePixels = 8;
    std::map<std::string, PortraitLayer> layers;
    std::map<std::string, double> blink;
    bool dirty = false;
};
struct FaceRig {
    std::wstring id;
    fs::path path, packRoot;
    std::string source;
    std::vector<FacePart> parts;
    double upperTravel = 1000, lowerTravel = 1000;
    int period = 80, close = 3, hold = 2, open = 4;
    bool dirty = false;
    std::vector<fs::path> related;
    std::optional<PortraitProfile> profile;
    std::string blinkId = "eye_closure";
};
struct FaceQuad {
    int part = 0, piece = 0, order = 0;
    std::string id;
    std::array<int, 4> area;
    double placeX = 0, placeY = 0, x = 0, y = 0, w = 0, h = 0;
    bool visible = true;
};
PortraitProfile parsePortraitProfile(std::string source, const fs::path &path);
std::string updatedPortraitSource(const PortraitProfile &profile);
void savePortraitProfile(PortraitProfile &profile, const fs::path &destination);
std::vector<FaceQuad> faceQuads(const FaceRig &rig, double closure = 0, unsigned size = 256,
                                bool useProfile = true);
fs::path findContentPack(fs::path path);
FaceRig parseFaceRig(std::string source, const fs::path &sourcePath, const fs::path &packRoot);
void loadFaceTextures(FaceRig &rig);
std::map<std::wstring, std::shared_ptr<FaceRig>> loadFaceCatalog(const fs::path &root,
                                                                 std::vector<std::wstring> &diagnostics);
std::string updatedFaceSource(const FaceRig &rig);
bool setDefaultFaceDepths(FaceRig &rig);
void saveFaceRig(FaceRig &rig, const fs::path &destination);
Image composeFace(const Image &base, const FaceRig &rig, double closure = 0, unsigned size = 256,
                  bool useProfile = true);
double blinkClosure(const FaceRig &rig, double seconds);
} // namespace echo
