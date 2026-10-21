#include "npc/Removal.h"
#include <set>
#include <fstream>
#include <iomanip>
namespace echo {
namespace {
std::vector<fs::path> paths(const Npc &n, bool assets) {
    std::vector<fs::path> result{n.metadata, n.gameDefinition};
    if (assets) {
        if (n.skin)
            result.push_back(n.skin->path);
        if (n.faceRig) {
            result.push_back(n.faceRig->path);
            if (n.faceRig->profile)
                result.push_back(n.faceRig->profile->path);
            for (const auto &part : n.faceRig->parts)
                result.push_back(part.texturePath);
        }
    }
    return result;
}
} // namespace
std::vector<fs::path> npcRemovalFiles(const std::vector<Npc> &npcs, size_t index, bool assets) {
    std::set<fs::path> shared, selected;
    for (size_t i = 0; i < npcs.size(); ++i)
        if (i != index)
            for (const auto &path : paths(npcs[i], true))
                if (!path.empty())
                    shared.insert(fs::weakly_canonical(path));
    for (const auto &path : paths(npcs.at(index), assets))
        if (!path.empty() && fs::is_regular_file(path)) {
            auto canonical = fs::canonical(path);
            if (!shared.contains(canonical))
                selected.insert(canonical);
        }
    return {selected.begin(), selected.end()};
}
RemovedNpcFiles archiveNpcFiles(const std::vector<fs::path> &files, const fs::path &archiveRoot) {
    RemovedNpcFiles archive;
    archive.folder =
        archiveRoot / (std::to_wstring(GetCurrentProcessId()) + L"-" + std::to_wstring(GetTickCount64()));
    fs::create_directories(archiveRoot);
    if (!fs::create_directory(archive.folder))
        throw std::runtime_error(tr8("error.removal.archive"));
    std::ofstream manifest(archive.folder / L"restore.txt", std::ios::binary);
    for (const auto &file : files) {
        auto backup =
            archive.folder / (std::to_wstring(archive.files.size()) + L"-" + file.filename().wstring());
        fs::copy_file(file, backup);
        archive.files.emplace_back(file, backup);
        manifest << std::quoted(utf8(file.wstring())) << ' ' << std::quoted(utf8(backup.filename().wstring()))
                 << '\n';
    }
    manifest.close();
    if (!manifest)
        throw std::runtime_error(tr8("error.removal.archive"));
    std::vector<fs::path> removed;
    try {
        for (const auto &[file, backup] : archive.files) {
            if (!fs::remove(file))
                throw std::runtime_error(tr8("error.removal.archive"));
            removed.push_back(file);
        }
    } catch (...) {
        for (const auto &[file, backup] : archive.files)
            if (std::find(removed.begin(), removed.end(), file) != removed.end())
                fs::copy_file(backup, file);
        throw;
    }
    return archive;
}
void restoreNpcFiles(const RemovedNpcFiles &archive) {
    for (const auto &[file, backup] : archive.files)
        if (fs::exists(file) || !fs::is_regular_file(backup))
            throw std::runtime_error(tr8("error.removal.conflict"));
    std::vector<fs::path> restored;
    try {
        for (const auto &[file, backup] : archive.files) {
            fs::create_directories(file.parent_path());
            fs::copy_file(backup, file);
            restored.push_back(file);
        }
    } catch (...) {
        for (const auto &file : restored) {
            std::error_code ec;
            fs::remove(file, ec);
        }
        throw;
    }
}
} // namespace echo
