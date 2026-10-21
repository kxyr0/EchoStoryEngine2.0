#pragma once
#include "skin/Skin.h"
#include "portrait/FaceRig.h"
#include <map>
#include <atomic>
namespace echo {
struct Npc {
    std::wstring id, name, color = L"#DEC6B2";
    bool slim = false, hat = true, jacket = true, sleeves = true, pants = true, dirty = false;
    fs::path metadata;
    fs::path gameDefinition;
    std::string gameSource;
    std::shared_ptr<Skin> skin;
    std::shared_ptr<FaceRig> faceRig;
    Image animationPortrait;
    std::map<std::string, std::string> extra;
};
struct ScanResult {
    std::vector<Npc> npcs;
    std::vector<std::wstring> diagnostics;
};
bool validIdentifier(const std::wstring &id);
bool validColor(const std::wstring &color);
Npc npcFromSkin(std::shared_ptr<Skin> skin);
std::shared_ptr<Skin> skinForPreview(const Npc &npc);
void readMetadata(Npc &npc);
void saveMetadata(Npc &npc, const fs::path &destination);
std::wstring valuePreview(const Npc &npc);
ScanResult scanProject(const fs::path &root, const std::atomic_bool &stop);
// Echo Script 2 deliberately has no guessed grammar. A future adapter must preserve source spans.
class ScriptAdapter {
  public:
    virtual ~ScriptAdapter() = default;
    virtual bool available() const = 0;
    virtual Npc parse(const std::string &source) = 0;
    virtual std::string update(const std::string &original, const Npc &npc) = 0;
};
} // namespace echo
