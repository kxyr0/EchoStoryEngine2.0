#pragma once
#include "npc/Repository.h"
namespace echo {
struct RemovedNpcFiles {
    fs::path folder;
    std::vector<std::pair<fs::path, fs::path>> files;
};
std::vector<fs::path> npcRemovalFiles(const std::vector<Npc> &npcs, size_t index, bool assets);
RemovedNpcFiles archiveNpcFiles(const std::vector<fs::path> &files, const fs::path &archiveRoot);
void restoreNpcFiles(const RemovedNpcFiles &archive);
} // namespace echo
