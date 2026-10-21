#pragma once
#include "npc/Repository.h"
namespace echo {
struct NpcTemplate {
    std::wstring prefix, name, color;
};
NpcTemplate loadBackgroundTemplate(const fs::path &file);
std::wstring nextTemplateId(const NpcTemplate &settings, const std::vector<Npc> &npcs, const fs::path &pack,
                            const fs::path &metadataFolder = {});
} // namespace echo
