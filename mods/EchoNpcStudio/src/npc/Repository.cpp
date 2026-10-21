#include "npc/Repository.h"
#include <fstream>
#include <sstream>
#include <iomanip>
#include <set>
namespace echo {
bool validIdentifier(const std::wstring &id) {
    auto p = id.find(L':');
    if (p == 0 || p == std::wstring::npos || p + 1 == id.size())
        return false;
    for (size_t i = 0; i < id.size(); ++i) {
        auto c = id[i];
        if (i == p)
            continue;
        if (!((c >= L'a' && c <= L'z') || (c >= L'0' && c <= L'9') || c == L'_' || c == L'-' || c == L'.' ||
              (i > p && c == L'/')))
            return false;
    }
    return true;
}
bool validColor(const std::wstring &c) {
    return c.size() == 7 && c[0] == L'#' && std::all_of(c.begin() + 1, c.end(), [](wchar_t a) {
               return (a >= L'0' && a <= L'9') || (a >= L'a' && a <= L'f') || (a >= L'A' && a <= L'F');
           });
}
std::shared_ptr<Skin> skinForPreview(const Npc &npc) {
    return npc.skin; // Face layers are rendered as geometry; never bake them into the source skin.
}
Npc npcFromSkin(std::shared_ptr<Skin> skin) {
    Npc n;
    n.skin = std::move(skin);
    n.name = n.skin->path.stem().wstring();
    std::wstring slug = n.name;
    for (auto &c : slug)
        if (!((c >= L'a' && c <= L'z') || (c >= L'0' && c <= L'9') || c == L'_' || c == L'-'))
            c = L'_';
    n.id = L"jdh:" + slug;
    n.metadata = n.skin->path;
    n.metadata.replace_extension(L".echo-npc");
    return n;
}
void readMetadata(Npc &npc) {
    if (!fs::exists(npc.metadata))
        return;
    std::ifstream input(npc.metadata, std::ios::binary);
    if (!input)
        throw std::runtime_error(tr8("error.repository.cannot_read_npc_metadata"));
    std::string line;
    if (!std::getline(input, line) || line != "ECHO_NPC_STUDIO 1")
        throw std::runtime_error(tr8("error.repository.unsupported_npc_metadata_version"));
    std::map<std::string, std::string> values;
    while (std::getline(input, line)) {
        std::istringstream in(line);
        std::string key, value;
        if (!(in >> key >> std::quoted(value)))
            throw std::runtime_error(tr8("error.repository.malformed_npc_metadata"));
        values[key] = value;
    }
    npc.extra = values;
    auto text = [&](const char *k, std::wstring &dest) {
        if (values.contains(k))
            dest = wide(values[k]);
    };
    text("identifier", npc.id);
    text("display", npc.name);
    text("color", npc.color);
    auto flag = [&](const char *k, bool &dest) {
        if (values.contains(k)) {
            if (values[k] != "0" && values[k] != "1")
                throw std::runtime_error(tr8("error.repository.invalid_metadata_flag"));
            dest = values[k] == "1";
        }
    };
    flag("slim", npc.slim);
    flag("hat", npc.hat);
    flag("jacket", npc.jacket);
    flag("sleeves", npc.sleeves);
    flag("pants", npc.pants);
    if (values.contains("skin")) {
        fs::path p(wide(values["skin"]));
        if (p.empty())
            throw std::runtime_error(tr8("error.repository.empty_skin_mapping"));
        if (p.is_relative())
            p = npc.metadata.parent_path() / p;
        if (!npc.skin->image.width)
            npc.skin->path = p;
        else if (p.lexically_normal() != npc.skin->path.lexically_normal())
            npc.skin = loadSkin(p);
    }
}
void saveMetadata(Npc &npc, const fs::path &destination) {
    auto extension = destination.extension().wstring();
    std::transform(extension.begin(), extension.end(), extension.begin(), towlower);
    if (extension != L".echo-npc")
        throw std::runtime_error(
            tr8("error.repository.npc_metadata_must_use_the_echo_npc_extension_source_assets"));
    if (!validIdentifier(npc.id))
        throw std::runtime_error(tr8("error.repository.invalid_npc_identifier"));
    if (!validColor(npc.color))
        throw std::runtime_error(tr8("error.repository.invalid_hex_color"));
    if (npc.name.empty())
        throw std::runtime_error(tr8("error.repository.display_name_cannot_be_empty"));
    auto values = npc.extra;
    values["identifier"] = utf8(npc.id);
    values["display"] = utf8(npc.name);
    values["color"] = utf8(npc.color);
    std::error_code ec;
    auto relative = fs::relative(npc.skin->path, destination.parent_path(), ec);
    values["skin"] = utf8((ec || relative.empty() ? fs::absolute(npc.skin->path) : relative).wstring());
    values["slim"] = npc.slim ? "1" : "0";
    values["hat"] = npc.hat ? "1" : "0";
    values["jacket"] = npc.jacket ? "1" : "0";
    values["sleeves"] = npc.sleeves ? "1" : "0";
    values["pants"] = npc.pants ? "1" : "0";
    std::string bytes = "ECHO_NPC_STUDIO 1\n";
    for (auto &[key, value] : values) {
        std::ostringstream out;
        out << key << ' ' << std::quoted(value) << '\n';
        bytes += out.str();
    }
    auto temp = destination;
    temp += L".tmp-" + std::to_wstring(GetCurrentProcessId()) + L"-" + std::to_wstring(GetTickCount64());
    HANDLE file =
        CreateFileW(temp.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE)
        throw std::runtime_error(tr8("error.repository.cannot_create_metadata_temporary_file"));
    DWORD written = 0;
    bool ok = WriteFile(file, bytes.data(), static_cast<DWORD>(bytes.size()), &written, nullptr) &&
              written == bytes.size() && FlushFileBuffers(file);
    CloseHandle(file);
    if (!ok ||
        !MoveFileExW(temp.c_str(), destination.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
        DeleteFileW(temp.c_str());
        throw std::runtime_error(tr8("error.repository.cannot_atomically_save_npc_metadata"));
    }
    npc.metadata = destination;
    npc.extra = values;
    npc.dirty = false;
}
std::wstring valuePreview(const Npc &n) {
    return trf("ui.repository.values_preview",
               {{L"id", n.id},
                {L"name", n.name},
                {L"skin", n.skin->path.wstring()},
                {L"color", n.color},
                {L"model", n.slim ? tr("ui.repository.slim") : tr("ui.repository.classic")}});
}
ScanResult scanProject(const fs::path &root, const std::atomic_bool &stop) {
    ScanResult result;
    std::error_code ec;
    std::set<std::wstring> ids;
    std::vector<fs::path> skins, metadata;
    fs::recursive_directory_iterator it(root, fs::directory_options::skip_permission_denied, ec), end;
    if (ec)
        result.diagnostics.push_back(tr("ui.repository.cannot_open_folder") + wide(ec.message()));
    while (it != end && !stop) {
        auto entry = *it;
        it.increment(ec);
        if (ec) {
            result.diagnostics.push_back(wide(ec.message()));
            ec.clear();
        }
        if (!entry.is_regular_file(ec))
            continue;
        auto ext = entry.path().extension().wstring();
        std::transform(ext.begin(), ext.end(), ext.begin(), towlower);
        if (ext == L".png") {
            if (entry.path().generic_wstring().find(L"/echo_animation/faces/") == std::wstring::npos)
                skins.push_back(entry.path());
        } else if (ext == L".echo-npc")
            metadata.push_back(entry.path());
    }
    std::set<fs::path> referenced;
    std::map<fs::path, std::shared_ptr<Skin>> cache;
    size_t bytes = 0;
    auto cached = [&](const fs::path &path) {
        auto key = fs::weakly_canonical(path);
        auto it = cache.find(key);
        if (it != cache.end())
            return it->second;
        auto loaded = loadSkin(path);
        bytes += loaded->image.rgba.size() + loaded->portrait.rgba.size();
        if (bytes > 512ull * 1024 * 1024)
            throw std::runtime_error(
                tr8("error.repository.cpu_skin_cache_exceeds_512_mib_open_a_smaller_project"));
        cache[key] = loaded;
        return loaded;
    };

    struct GameNpc {
        std::map<std::string, std::string> fields;
        fs::path path;
        std::string source;
    };
    std::map<std::wstring, GameNpc> definitions;
    auto pack = findContentPack(root);
    if (!pack.empty()) {
        for (auto &ns : fs::directory_iterator(pack / L"data")) {
            auto folder = ns.path() / L"echo_npc" / L"npcs";
            if (!fs::is_directory(folder))
                continue;
            for (auto &entry : fs::recursive_directory_iterator(folder)) {
                if (stop)
                    break;
                if (!entry.is_regular_file() || !entry.path().filename().wstring().ends_with(L".npc.es"))
                    continue;
                try {
                    std::ifstream in(entry.path(), std::ios::binary);
                    if (!in)
                        throw std::runtime_error(tr8("error.repository.cannot_read_npc_definition"));
                    std::string source((std::istreambuf_iterator<char>(in)), {});
                    auto fields = scriptStringCalls(source);
                    if (fields.contains("Id") && fields.contains("Display")) {
                        auto id = wide(fields.at("Id"));
                        definitions[id] = {std::move(fields), entry.path(), source};
                    }
                } catch (const std::exception &e) {
                    result.diagnostics.push_back(entry.path().wstring() + L": " + wide(e.what()));
                }
            }
        }
    }
    auto add = [&](Npc npc) {
        if (auto def = definitions.find(npc.id); def != definitions.end()) {
            npc.gameDefinition = def->second.path;
            npc.gameSource = def->second.source;
            auto &fields = def->second.fields;
            if (!npc.extra.contains("display"))
                npc.name = wide(fields.at("Display"));
            if (!npc.extra.contains("color") && fields.contains("NameColor")) {
                auto color = wide(fields.at("NameColor"));
                if (color.size() == 9)
                    color.resize(7); // Editor stores RGB; leave game alpha untouched.
                if (validColor(color))
                    npc.color = color;
            }
        }
        if (!validIdentifier(npc.id))
            result.diagnostics.push_back(tr("ui.repository.invalid_id") + npc.id);
        if (!validColor(npc.color))
            result.diagnostics.push_back(tr("ui.repository.invalid_color") + npc.id);
        if (!ids.insert(npc.id).second)
            result.diagnostics.push_back(tr("ui.repository.duplicate_id") + npc.id);
        result.npcs.push_back(std::move(npc));
    };
    for (auto &path : metadata) {
        if (stop)
            break;
        try {
            Npc npc;
            npc.metadata = path;
            npc.skin = std::make_shared<Skin>();
            readMetadata(npc);
            if (npc.skin->path.empty())
                throw std::runtime_error(tr8("error.repository.metadata_has_no_skin_mapping"));
            npc.skin = cached(npc.skin->path);
            referenced.insert(fs::weakly_canonical(npc.skin->path));
            add(std::move(npc));
        } catch (const std::exception &e) {
            result.diagnostics.push_back(path.wstring() + L": " + wide(e.what()));
        }
    }
    for (auto &path : skins) {
        if (stop)
            break;
        try {
            if (referenced.contains(fs::weakly_canonical(path)))
                continue;
            auto npc = npcFromSkin(cached(path));
            if (fs::exists(npc.metadata)) {
                result.diagnostics.push_back(tr("ui.repository.skipped_skin_with_invalid_metadata") +
                                             path.wstring());
                continue;
            }
            add(std::move(npc));
        } catch (const std::exception &e) {
            result.diagnostics.push_back(path.wstring() + L": " + wide(e.what()));
        }
    }
    auto faces = loadFaceCatalog(root, result.diagnostics);
    for (auto &n : result.npcs) {
        auto faceEntry = faces.find(n.id);
        if (faceEntry != faces.end())
            n.faceRig = faceEntry->second;
        else if (n.skin->path.stem() == L"player") {
            auto player = faces.find(L"echo_content_player:player/base");
            if (player != faces.end())
                n.faceRig = player->second;
        }
        if (n.faceRig)
            n.animationPortrait = composeFace(n.skin->portrait, *n.faceRig);
    }
    std::sort(result.npcs.begin(), result.npcs.end(), [](const Npc &a, const Npc &b) {
        if (a.metadata.parent_path() != b.metadata.parent_path())
            return a.metadata.parent_path() < b.metadata.parent_path();
        if (a.skin->path != b.skin->path)
            return a.skin->path < b.skin->path;
        return a.id < b.id;
    });
    return result;
}
} // namespace echo
