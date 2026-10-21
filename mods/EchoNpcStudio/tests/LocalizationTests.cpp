#include "Common.h"
#include "npc/Templates.h"
#include <fstream>
#include <iostream>
#include <regex>
using namespace echo;
namespace {
void require(bool value, const char *message) {
    if (!value)
        throw std::runtime_error(message);
}
std::string read(const fs::path &path) {
    std::ifstream file(path, std::ios::binary);
    require(bool(file), "Cannot read translation test file");
    return {std::istreambuf_iterator<char>(file), {}};
}
void write(const fs::path &path, const std::string &data) {
    std::ofstream(path, std::ios::binary) << data;
}
bool rejected(const std::string &json) {
    try {
        parseTranslations(json);
        return false;
    } catch (const std::exception &) {
        return true;
    }
}
} // namespace
int wmain(int argc, wchar_t **argv) {
    fs::path temporary;
    try {
        require(argc == 2, "Pass the source directory");
        const fs::path source = argv[1];
        auto russian = parseTranslations(read(source / L"locales/ru.json"));
        auto english = parseTranslations(read(source / L"locales/en.json"));
        require(russian.size() == english.size() && russian.size() > 400, "Language catalog coverage");
        for (const auto &[key, value] : russian) {
            require(english.contains(key), "Language key mismatch");
            require(!value.empty() && !english.at(key).empty(), "Empty translation");
        }
        std::regex keys(R"KEY(\btr(?:8|f)?\(\s*((?:"[^"]*"\s*)+))KEY");
        std::regex literal(R"KEY("([^"]*)")KEY");
        for (const auto &entry : fs::recursive_directory_iterator(source / L"src")) {
            if (!entry.is_regular_file() ||
                (entry.path().extension() != L".cpp" && entry.path().extension() != L".h"))
                continue;
            const auto text = read(entry.path());
            for (std::sregex_iterator it(text.begin(), text.end(), keys), end; it != end; ++it) {
                const auto pieces = (*it)[1].str();
                std::string key;
                for (std::sregex_iterator part(pieces.begin(), pieces.end(), literal); part != end; ++part)
                    key += (*part)[1].str();
                if (!russian.contains(key))
                    throw std::runtime_error("Missing translation key: " + key + " in " +
                                             entry.path().string());
            }
        }
        auto unicode = parseTranslations(
            "\xEF\xBB\xBF{\"key\":\"\\u0412\\u0435\\u043a\\u0438 \\uD83D\\uDE00\\n\\t\\\"\\\\\\u0000\"}");
        require(unicode.at("key") == std::wstring(L"Веки 😀\n\t\"\\\0", 12),
                "Unicode, escapes and embedded NUL parsing");
        for (const auto &bad :
             {"[]", "{\"key\":1}", "{\"key\":\"a\",\"key\":\"b\"}", "{\"key\":\"a\",}", "{} trailing",
              "{\"key\":\"\\uD800\"}", "{\"key\":\"\\uDC00\"}", "{\"key\":\"\\q\"}", "{\"key\":\"\n\"}"})
            require(rejected(bad), "Malformed JSON accepted");
        require(rejected(std::string("{\"key\":\"") + "\xFF" + "\"}"), "Invalid UTF-8 accepted");
        temporary =
            fs::temp_directory_path() / (L"EchoLocalizationTests-" + std::to_wstring(GetCurrentProcessId()) +
                                         L"-" + std::to_wstring(GetTickCount64()));
        fs::create_directories(temporary);
        auto background = loadBackgroundTemplate(temporary / L"missing-template.json");
        require(background.prefix == L"jdh:background_" && background.name == L"Незнакомец" &&
                    background.color == L"#DEC6B2",
                "Default background NPC template");
        std::vector<Npc> occupied(1);
        occupied[0].id = L"jdh:background_00001";
        write(temporary / L"background_00002.echo-npc", "reserved");
        require(nextTemplateId(background, occupied, {}, temporary) == L"jdh:background_00003",
                "Template ID must skip library and files");
        write(temporary / L"background.json",
              "{\"id_prefix\":\"jdh:extra_\",\"display_name\":\"Custom NPC\",\"name_color\":\"#123456\"}");
        auto custom = loadBackgroundTemplate(temporary / L"background.json");
        require(custom.name == L"Custom NPC" && custom.color == L"#123456" &&
                    nextTemplateId(custom, occupied, {}) == L"jdh:extra_00001",
                "External JSON template overrides");
        write(temporary / L"background.json",
              "{\"id_prefix\":\"jdh:../escape_\",\"display_name\":\"NPC\",\"name_color\":\"#123456\"}");
        bool unsafeRejected = false;
        try {
            loadBackgroundTemplate(temporary / L"background.json");
        } catch (...) {
            unsafeRejected = true;
        }
        require(unsafeRejected, "Unsafe template prefix accepted");
        write(temporary / L"settings.json", "{\"language\":\"en\"}");
        write(temporary / L"ru.json", "{\"ui.common.save\":\"Russian override\"}");
        write(temporary / L"en.json",
              "{\"ui.common.save\":\"External save\",\"test.format\":\"{second} / {first} / {second}\"}");
        require(loadLocalization(temporary).empty() && activeLanguage() == L"en",
                "Configured language loading");
        require(tr("ui.common.save") == L"External save" && tr8("ui.common.save") == "External save",
                "External JSON override");
        require(trf("test.format", {{L"first", L"{second}"}, {L"second", L"B"}}) == L"B / {second} / B",
                "Named arguments must not reinterpret inserted text");
        write(temporary / L"en.json", "{\"ui.common.save\":\"Partial must not apply\",}");
        require(loadLocalization(temporary).size() == 1 && tr("ui.common.save") == L"Save",
                "Malformed override must fall back atomically");
        require(loadLocalization(temporary, L"ru").empty() && tr("ui.common.save") == L"Russian override",
                "Command line overrides configured language");
        write(temporary / L"ru.json", "{}");
        require(loadLocalization(temporary, L"../escape").size() == 1 && activeLanguage() == L"ru",
                "Unsafe locale identifier accepted");
        require(tr("ui.common.save") == L"Сохранить", "Missing keys must use embedded fallback");
        fs::remove(temporary / L"ru.json");
        write(temporary / L"settings.json", "broken");
        require(loadLocalization(temporary).size() == 2 && tr("ui.common.save") == L"Сохранить",
                "Broken settings and missing file recovery");
        require(tr("missing.test.key") == L"[missing.test.key]", "Missing key diagnostic");
        fs::remove_all(temporary);
        std::cout
            << "Localization catalogs, key coverage, Unicode JSON, external overrides and fallback passed.\n";
        return 0;
    } catch (const std::exception &e) {
        std::cerr << e.what() << '\n';
        if (!temporary.empty())
            fs::remove_all(temporary);
        return 1;
    }
}
