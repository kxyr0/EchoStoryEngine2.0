#include "npc/Templates.h"
#include "TemplateDefaults.h"
#include <fstream>
#include <iomanip>
#include <sstream>
namespace echo {
NpcTemplate loadBackgroundTemplate(const fs::path &file) {
    std::string json = defaultBackgroundTemplate;
    if (fs::exists(file)) {
        std::ifstream input(file, std::ios::binary);
        if (!input || fs::file_size(file) > 64 * 1024)
            throw std::runtime_error(tr8("error.template.read"));
        json.assign(std::istreambuf_iterator<char>(input), {});
    }
    TranslationMap values;
    try {
        values = parseTranslations(json);
    } catch (const std::exception &) {
        throw std::runtime_error(tr8("error.template.json"));
    }
    if (!values.contains("id_prefix") || !values.contains("display_name") || !values.contains("name_color"))
        throw std::runtime_error(tr8("error.template.fields"));
    NpcTemplate result{values.at("id_prefix"), values.at("display_name"), values.at("name_color")};
    // A flat resource prefix makes the generated path safe in both metadata and content-pack modes.
    if (!validIdentifier(result.prefix + L"00001") || result.prefix.find(L'/') != result.prefix.npos ||
        result.prefix.find(L"..") != result.prefix.npos || result.name.empty() ||
        result.name.find(L'\0') != result.name.npos || !validColor(result.color))
        throw std::runtime_error(tr8("error.template.values"));
    return result;
}
std::wstring nextTemplateId(const NpcTemplate &settings, const std::vector<Npc> &npcs, const fs::path &pack,
                            const fs::path &metadataFolder) {
    auto colon = settings.prefix.find(L':');
    auto ns = settings.prefix.substr(0, colon);
    for (unsigned number = 1; number <= 99999; ++number) {
        std::wostringstream suffix;
        suffix << std::setw(5) << std::setfill(L'0') << number;
        auto id = settings.prefix + suffix.str();
        if (std::any_of(npcs.begin(), npcs.end(), [&](const Npc &n) { return n.id == id; }))
            continue;
        auto slug = id.substr(colon + 1);
        bool used = !metadataFolder.empty() && fs::exists(metadataFolder / (slug + L".echo-npc"));
        if (!pack.empty()) {
            auto skin = pack / L"assets" / ns / L"textures" / L"echo_npc" / L"skins";
            auto animation = pack / L"data" / ns / L"echo_animation" / L"face_rigs";
            auto definitions = pack / L"data" / ns / L"echo_npc" / L"npcs";
            used |= fs::exists(skin / (slug + L".png")) || fs::exists(skin / (slug + L".echo-npc")) ||
                    fs::exists(pack / L"assets" / ns / L"textures" / L"echo_animation" / L"faces" / slug) ||
                    fs::exists(animation / L"generated" / (slug + L".face-rig.es")) ||
                    fs::exists(animation / (slug + L".face-rig.es")) ||
                    fs::exists(definitions / L"generated" / (slug + L".npc.es")) ||
                    fs::exists(definitions / (slug + L".npc.es"));
            auto profiles =
                pack.parent_path().parent_path() / L"configs" / ns / L"echo_animation" / L"portrait_overlays";
            used |= fs::exists(profiles / L"generated" / (slug + L".portrait-overlay.es")) ||
                    fs::exists(profiles / (slug + L".portrait-overlay.es"));
        }
        if (!used)
            return id;
    }
    throw std::runtime_error(tr8("error.template.no_free_id"));
}
} // namespace echo
