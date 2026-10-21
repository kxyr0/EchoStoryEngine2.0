#include "localization/Localization.h"
#include "LocalizationDefaults.h"
#include <windows.h>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <algorithm>
#include <mutex>
namespace echo {
namespace {
std::wstring decode(std::string_view text) {
    if (text.empty())
        return {};
    int count = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text.data(), int(text.size()), nullptr, 0);
    if (!count)
        throw std::runtime_error("Invalid UTF-8 in localization JSON");
    std::wstring value(count, L' ');
    MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text.data(), int(text.size()), value.data(), count);
    return value;
}
class JsonReader {
    std::string_view input;
    size_t at = 0;
    [[noreturn]] void fail() const {
        throw std::runtime_error("Invalid localization JSON at byte " + std::to_string(at));
    }
    void whitespace() {
        while (at < input.size() &&
               (input[at] == ' ' || input[at] == '\n' || input[at] == '\r' || input[at] == '\t'))
            ++at;
    }
    bool take(char c) {
        whitespace();
        if (at < input.size() && input[at] == c) {
            ++at;
            return true;
        }
        return false;
    }
    unsigned hex() {
        unsigned value = 0;
        for (int i = 0; i < 4; ++i) {
            if (at == input.size())
                fail();
            char c = input[at++];
            unsigned digit = c >= '0' && c <= '9'   ? c - '0'
                             : c >= 'a' && c <= 'f' ? c - 'a' + 10
                             : c >= 'A' && c <= 'F' ? c - 'A' + 10
                                                    : 16;
            if (digit == 16)
                fail();
            value = value * 16 + digit;
        }
        return value;
    }
    std::wstring string() {
        if (!take('"'))
            fail();
        std::wstring value;
        std::string bytes;
        auto flush = [&] {
            value += decode(bytes);
            bytes.clear();
        };
        while (at < input.size()) {
            unsigned char c = static_cast<unsigned char>(input[at++]);
            if (c == '"') {
                flush();
                return value;
            }
            if (c < 32)
                fail();
            if (c != '\\') {
                bytes += char(c);
                continue;
            }
            flush();
            if (at == input.size())
                fail();
            switch (input[at++]) {
            case '"':
                value += L'"';
                break;
            case '\\':
                value += L'\\';
                break;
            case '/':
                value += L'/';
                break;
            case 'b':
                value += L'\b';
                break;
            case 'f':
                value += L'\f';
                break;
            case 'n':
                value += L'\n';
                break;
            case 'r':
                value += L'\r';
                break;
            case 't':
                value += L'\t';
                break;
            case 'u': {
                unsigned code = hex();
                if (code >= 0xD800 && code <= 0xDBFF) {
                    if (at + 2 > input.size() || input.substr(at, 2) != "\\u")
                        fail();
                    at += 2;
                    unsigned low = hex();
                    if (low < 0xDC00 || low > 0xDFFF)
                        fail();
                    value += wchar_t(code);
                    value += wchar_t(low);
                } else {
                    if (code >= 0xDC00 && code <= 0xDFFF)
                        fail();
                    value += wchar_t(code);
                }
                break;
            }
            default:
                fail();
            }
        }
        fail();
    }

  public:
    explicit JsonReader(std::string_view text) : input(text) {
        if (input.starts_with("\xEF\xBB\xBF"))
            at = 3;
    }
    TranslationMap read() {
        TranslationMap result;
        if (!take('{'))
            fail();
        if (!take('}')) {
            do {
                auto wideKey = string();
                std::string key;
                for (wchar_t c : wideKey) {
                    if (c < 33 || c > 126)
                        fail();
                    key += char(c);
                }
                if (key.empty() || !take(':'))
                    fail();
                auto value = string();
                if (!result.emplace(key, value).second)
                    fail();
                if (take('}'))
                    break;
                if (!take(','))
                    fail();
            } while (true);
        }
        whitespace();
        if (at != input.size())
            fail();
        return result;
    }
};
TranslationMap readFile(const std::filesystem::path &path) {
    std::ifstream file(path, std::ios::binary);
    if (!file)
        throw std::runtime_error("Cannot read localization JSON");
    std::string data;
    char buffer[8192];
    while (file.read(buffer, sizeof(buffer)) || file.gcount()) {
        data.append(buffer, size_t(file.gcount()));
        if (data.size() > 2 * 1024 * 1024)
            throw std::runtime_error("Localization JSON exceeds 2 MiB");
    }
    if (file.bad())
        throw std::runtime_error("Cannot read localization JSON");
    return parseTranslations(data);
}
struct Catalog {
    TranslationMap values = parseTranslations(defaultRussian);
    std::wstring language = L"ru";
};
Catalog &catalog() {
    static Catalog value;
    return value;
}
bool validLanguage(const std::wstring &language) {
    return !language.empty() && language.size() <= 32 &&
           std::all_of(language.begin(), language.end(), [](wchar_t c) {
               return (c >= L'a' && c <= L'z') || (c >= L'A' && c <= L'Z') || (c >= L'0' && c <= L'9') ||
                      c == L'-' || c == L'_';
           });
}
} // namespace
TranslationMap parseTranslations(std::string_view json) {
    return JsonReader(json).read();
}
const std::wstring &tr(std::string_view key) {
    auto &values = catalog().values;
    auto found = values.find(key);
    if (found != values.end())
        return found->second;
    // Visible key makes omissions discoverable without dangling references or exceptions in paint.
    static TranslationMap missing;
    static std::mutex mutex;
    std::lock_guard lock(mutex);
    return missing.emplace(std::string(key), L"[" + decode(key) + L"]").first->second;
}
std::string tr8(std::string_view key) {
    const auto &value = tr(key);
    if (value.empty())
        return {};
    int count = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, value.data(), int(value.size()), nullptr,
                                    0, nullptr, nullptr);
    if (!count)
        throw std::runtime_error("Invalid Unicode in localization JSON");
    std::string out(count, ' ');
    WideCharToMultiByte(CP_UTF8, 0, value.data(), int(value.size()), out.data(), count, nullptr, nullptr);
    return out;
}
std::wstring trf(std::string_view key,
                 std::initializer_list<std::pair<std::wstring_view, std::wstring>> arguments) {
    const auto &pattern = tr(key);
    std::wstring out;
    for (size_t i = 0; i < pattern.size();) {
        if (pattern[i] == L'{') {
            auto end = pattern.find(L'}', i + 1);
            if (end != pattern.npos) {
                auto name = std::wstring_view(pattern).substr(i + 1, end - i - 1);
                auto found = std::find_if(arguments.begin(), arguments.end(),
                                          [&](const auto &arg) { return arg.first == name; });
                if (found != arguments.end()) {
                    out += found->second;
                    i = end + 1;
                    continue;
                }
            }
        }
        out += pattern[i++];
    }
    return out;
}
std::vector<std::wstring> loadLocalization(const std::filesystem::path &directory,
                                           const std::wstring &languageOverride) {
    auto &state = catalog();
    state.values = parseTranslations(defaultRussian);
    state.language = L"ru";
    std::vector<std::wstring> warnings;
    std::wstring language = languageOverride;
    if (language.empty() && std::filesystem::exists(directory / L"settings.json")) {
        try {
            auto settings = readFile(directory / L"settings.json");
            auto found = settings.find("language");
            if (found != settings.end())
                language = found->second;
        } catch (const std::exception &) {
            warnings.push_back((directory / L"settings.json").wstring());
        }
    }
    if (language.empty())
        language = L"ru";
    if (!validLanguage(language)) {
        warnings.push_back(language);
        language = L"ru";
    }
    auto overlay = [&](const std::filesystem::path &path, bool required) {
        if (!std::filesystem::exists(path)) {
            if (required)
                warnings.push_back(path.wstring());
            return;
        }
        try {
            auto entries = readFile(path);
            for (auto &[key, value] : entries)
                state.values.insert_or_assign(key, std::move(value));
        } catch (const std::exception &) {
            warnings.push_back(path.wstring());
        }
    };
    overlay(directory / L"ru.json", true);
    if (language == L"en") {
        auto english = parseTranslations(defaultEnglish);
        for (auto &[key, value] : english)
            state.values.insert_or_assign(key, std::move(value));
    }
    if (language != L"ru")
        overlay(directory / (language + L".json"), true);
    state.language = language;
    for (auto &warning : warnings)
        warning = trf("localization.load_warning", {{L"file", warning}});
    return warnings;
}
const std::wstring &activeLanguage() {
    return catalog().language;
}
} // namespace echo
