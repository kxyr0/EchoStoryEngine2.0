#pragma once
#include <filesystem>
#include <map>
#include <string>
#include <string_view>
#include <vector>
namespace echo {
using TranslationMap = std::map<std::string, std::wstring, std::less<>>;
// A flat UTF-8 JSON object with string keys/values. Rejects malformed input atomically.
TranslationMap parseTranslations(std::string_view json);
const std::wstring &tr(std::string_view key);
std::string tr8(std::string_view key);
std::wstring trf(std::string_view key,
                 std::initializer_list<std::pair<std::wstring_view, std::wstring>> arguments);
// Directory is resolved relative to the executable, never the working directory.
std::vector<std::wstring> loadLocalization(const std::filesystem::path &directory,
                                           const std::wstring &languageOverride = {});
const std::wstring &activeLanguage();
} // namespace echo
