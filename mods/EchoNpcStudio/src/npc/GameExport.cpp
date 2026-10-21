#include "npc/GameExport.h"
#include <fstream>
#include <iomanip>
namespace echo {
namespace {
std::string read(const fs::path &path) {
    std::ifstream in(path, std::ios::binary);
    if (!in)
        throw std::runtime_error(tr8("error.export.read"));
    return {std::istreambuf_iterator<char>(in), {}};
}
void write(const fs::path &path, const std::string &text) {
    std::ofstream out(path, std::ios::binary);
    out << text;
    if (!out)
        throw std::runtime_error(tr8("error.export.write"));
}
} // namespace
void saveGameBundle(Npc &npc, const fs::path &pack) {
    if (!npc.skin || !npc.faceRig || npc.faceRig->id != npc.id || !validIdentifier(npc.id) ||
        !validColor(npc.color) || npc.name.empty())
        throw std::runtime_error(tr8("error.export.identity"));
    if (pack.empty() || fs::weakly_canonical(findContentPack(pack)) != fs::weakly_canonical(pack))
        throw std::runtime_error(tr8("error.faceauthoring.select_content_pack_root_with_assets_and_data"));
    auto &rig = *npc.faceRig;
    if (loadSkin(npc.skin->path)->image.rgba != npc.skin->image.rgba)
        throw std::runtime_error(tr8("error.export.external"));
    if (read(rig.path) != rig.source || (rig.profile && read(rig.profile->path) != rig.profile->source))
        throw std::runtime_error(tr8("error.export.external"));
    auto colon = npc.id.find(L':');
    auto ns = npc.id.substr(0, colon);
    auto slug = npc.id.substr(colon + 1);
    if (slug.find(L"..") != slug.npos || fs::path(slug).is_absolute())
        throw std::runtime_error(tr8("error.faceauthoring.unsafe_npc_resource_path"));
    auto skinPath = npc.skin->path;
    auto relative = fs::weakly_canonical(skinPath).lexically_relative(fs::weakly_canonical(pack / L"assets"));
    std::wstring skinId;
    if (!relative.empty() && *relative.begin() != L"..") {
        auto it = relative.begin();
        auto skinNs = *it++;
        if (it != relative.end() && *it++ == L"textures" && it != relative.end()) {
            fs::path texture;
            for (; it != relative.end(); ++it)
                texture /= *it;
            texture.replace_extension();
            skinId = skinNs.wstring() + L":" + texture.generic_wstring();
        }
    }
    // Echo NPC Skin refers to its skin folder, rather than the full textures-relative resource ID.
    auto skinColon = skinId.find(L':');
    auto prefix = skinId.substr(0, skinColon + 1) + L"echo_npc/skins/";
    if (skinId.starts_with(prefix))
        skinId = skinId.substr(0, skinColon + 1) + skinId.substr(prefix.size());
    else
        skinId.clear();
    if (skinId.empty()) {
        skinPath = pack / L"assets" / ns / L"textures" / L"echo_npc" / L"skins" / (slug + L".png");
        skinId = npc.id;
        if (fs::exists(skinPath))
            throw std::runtime_error(tr8("error.export.skin_collision"));
    }
    auto npcPath = npc.gameDefinition.empty()
                       ? pack / L"data" / ns / L"echo_npc" / L"npcs" / L"generated" / (slug + L".npc.es")
                       : npc.gameDefinition;
    std::string source;
    if (!npc.gameDefinition.empty()) {
        source = read(npcPath);
        if (!npc.gameSource.empty() && source != npc.gameSource)
            throw std::runtime_error(tr8("error.export.external"));
    } else {
        if (fs::exists(npcPath))
            throw std::runtime_error(tr8("error.export.npc_collision"));
        auto className = utf8(npc.id);
        for (auto &c : className)
            if (c == ':' || c == '/' || c == '.' || c == '-')
                c = '_';
        source = "namespace JDH.Generated;\nclass GeneratedNpc_" + className + " : NpcResource\n{\n}\n";
    }
    auto calls = scriptStringCalls(source);
    auto color = utf8(npc.color);
    auto colorCall = calls.contains("NameColor") ? "NameColor" : "nameColor";
    if (calls.contains(colorCall) && calls.at(colorCall).size() == 9)
        color += calls.at(colorCall).substr(7);
    source = patchScriptStrings(
        source,
        {{"Id", utf8(npc.id)}, {"Display", utf8(npc.name)}, {"Skin", utf8(skinId)}, {"NameColor", color}});
    auto rigSource = updatedFaceSource(rig);
    auto fresh = parseFaceRig(rigSource, rig.path, rig.packRoot);
    loadFaceTextures(fresh);
    fresh.related = rig.related;
    if (rig.profile)
        fresh.profile = parsePortraitProfile(updatedPortraitSource(*rig.profile), rig.profile->path);
    auto stage = fs::temp_directory_path() / (L"Echo-game-export-" + std::to_wstring(GetCurrentProcessId()) +
                                              L"-" + std::to_wstring(GetTickCount64()));
    if (!fs::create_directory(stage))
        throw std::runtime_error(tr8("error.export.write"));
    auto cleanup = [&] {
        std::error_code ec;
        for (const auto &entry : fs::directory_iterator(stage))
            fs::remove(entry.path(), ec);
        fs::remove(stage, ec);
    };
    std::vector<std::pair<fs::path, fs::path>> files;
    std::map<fs::path, fs::path> backups;
    std::vector<fs::path> committed;
    try {
        auto addText = [&](const fs::path &target, const std::string &text) {
            auto staged = stage / (std::to_wstring(files.size()) + L".es");
            write(staged, text);
            files.emplace_back(target, staged);
        };
        addText(npcPath, source);
        addText(rig.path, rigSource);
        if (fresh.profile)
            addText(fresh.profile->path, fresh.profile->source);
        fs::copy_file(npc.skin->path, stage / L"skin.png");
        files.emplace_back(skinPath, stage / L"skin.png");
        Npc saved = npc;
        saved.skin = std::make_shared<Skin>(*npc.skin);
        saved.skin->path = skinPath;
        auto metadata =
            npc.metadata.empty() ? fs::path(skinPath).replace_extension(L".echo-npc") : npc.metadata;
        saveMetadata(saved, stage / L"metadata.echo-npc");
        saved.extra["skin"] = utf8(skinPath.wstring());
        {
            std::ofstream out(stage / L"metadata.echo-npc", std::ios::binary | std::ios::trunc);
            out << "ECHO_NPC_STUDIO 1\n";
            for (auto &[key, value] : saved.extra)
                out << key << ' ' << std::quoted(value) << '\n';
            if (!out)
                throw std::runtime_error(tr8("error.export.write"));
        }
        files.emplace_back(metadata, stage / L"metadata.echo-npc");
        // The layer PNGs are already materialized by authoring; retain every texture and validate its
        // presence.
        for (const auto &part : fresh.parts)
            if (!fs::is_regular_file(part.texturePath))
                throw std::runtime_error(tr8("error.export.read"));
        for (size_t i = 0; i < files.size(); ++i) {
            const auto &[target, staged] = files[i];
            fs::create_directories(target.parent_path());
            if (fs::exists(target)) {
                auto backup = stage / (L"backup-" + std::to_wstring(i));
                fs::copy_file(target, backup);
                backups[target] = backup;
            }
            committed.push_back(target);
            fs::copy_file(staged, target,
                          backups.contains(target) ? fs::copy_options::overwrite_existing
                                                   : fs::copy_options::none);
        }
        saved.skin = loadSkin(skinPath);
        saved.metadata = metadata;
        saved.gameDefinition = npcPath;
        saved.gameSource = source;
        saved.faceRig = std::make_shared<FaceRig>(std::move(fresh));
        saved.animationPortrait = composeFace(saved.skin->portrait, *saved.faceRig);
        saved.dirty = false;
        npc = std::move(saved);
    } catch (...) {
        for (const auto &[target, backup] : backups) {
            std::error_code ec;
            fs::copy_file(backup, target, fs::copy_options::overwrite_existing, ec);
        }
        for (const auto &target : committed)
            if (!backups.contains(target)) {
                std::error_code ec;
                fs::remove(target, ec);
            }
        cleanup();
        throw;
    }
    cleanup();
}
std::wstring gameBundlePreview(const Npc &npc) {
    if (!npc.faceRig)
        return tr("ui.export.needs_face") + L"\n\n" + wide(npc.gameSource);
    std::wstring files = npc.gameDefinition.wstring() + L"\n" + npc.faceRig->path.wstring();
    if (npc.faceRig->profile)
        files += L"\n" + npc.faceRig->profile->path.wstring();
    for (const auto &part : npc.faceRig->parts)
        files += L"\n" + part.texturePath.wstring();
    return wide(npc.gameSource) + L"\n\n" + tr("ui.export.bundle_files") + L"\n" + files;
}
} // namespace echo
