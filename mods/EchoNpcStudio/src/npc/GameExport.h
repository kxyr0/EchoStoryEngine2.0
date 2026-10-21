#pragma once
#include "npc/Repository.h"
namespace echo {
void saveGameBundle(Npc &npc, const fs::path &pack);
std::wstring gameBundlePreview(const Npc &npc);
} // namespace echo
