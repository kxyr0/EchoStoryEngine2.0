#include "portrait/FaceRig.h"
#include <fstream>
#include <sstream>
#include <iomanip>
#include <set>
#include <cctype>
namespace echo {
namespace {
struct Token {
    std::string text;
    size_t start, end;
};
std::vector<Token> lex(const std::string &s) {
    std::vector<Token> out;
    size_t i = 0;
    while (i < s.size()) {
        if (std::isspace(static_cast<unsigned char>(s[i]))) {
            ++i;
            continue;
        }
        if (s.compare(i, 2, "//") == 0) {
            auto e = s.find('\n', i);
            i = e == s.npos ? s.size() : e;
            continue;
        }
        if (s.compare(i, 2, "/*") == 0) {
            auto e = s.find("*/", i + 2);
            if (e == s.npos)
                throw std::runtime_error(tr8("error.facerig.unclosed_comment_in_face_rig"));
            i = e + 2;
            continue;
        }
        size_t start = i;
        std::string value;
        if (s[i] == '"') {
            ++i;
            bool closed = false;
            while (i < s.size()) {
                char c = s[i++];
                if (c == '"') {
                    closed = true;
                    break;
                }
                if (c == '\\' && i < s.size())
                    c = s[i++];
                value += c;
            }
            if (!closed)
                throw std::runtime_error(tr8("error.facerig.unclosed_rig_string"));
        } else if (std::isalnum(static_cast<unsigned char>(s[i])) || s[i] == '_' || s[i] == '-' ||
                   s[i] == '.' || s[i] == '+') {
            while (i < s.size() && (std::isalnum(static_cast<unsigned char>(s[i])) || s[i] == '_' ||
                                    s[i] == '-' || s[i] == '.' || s[i] == '+'))
                value += s[i++];
        } else
            value += s[i++];
        out.push_back({value, start, i});
    }
    return out;
}
size_t matching(const std::vector<Token> &t, size_t start, const char *open, const char *close) {
    int depth = 0;
    for (size_t i = start; i < t.size(); ++i) {
        if (t[i].text == open)
            ++depth;
        else if (t[i].text == close && --depth == 0)
            return i;
    }
    throw std::runtime_error(tr8("error.facerig.unbalanced_face_rig_block"));
}
double number(const std::string &v) {
    size_t end = 0;
    double n = std::stod(v, &end);
    if (end != v.size() || !std::isfinite(n) || std::abs(n) > 10000)
        throw std::runtime_error(tr8("error.facerig.invalid_rig_numeric_value"));
    return n;
}
std::vector<double> numbers(const std::vector<Token> &t, size_t a, size_t b) {
    std::vector<double> out;
    for (size_t i = a; i < b; ++i)
        if (t[i].text != ",")
            out.push_back(number(t[i].text));
    return out;
}
std::string readText(const fs::path &p) {
    std::ifstream in(p, std::ios::binary);
    if (!in)
        throw std::runtime_error(tr8("error.facerig.cannot_read_face_rig"));
    std::ostringstream s;
    s << in.rdbuf();
    return s.str();
}
std::string format(const std::vector<double> &v) {
    std::ostringstream out;
    out << std::setprecision(10);
    for (size_t i = 0; i < v.size(); ++i) {
        if (i)
            out << ", ";
        out << v[i];
    }
    return out.str();
}
void settings(FacePart &part, const std::vector<Token> &t, size_t a, size_t b) {
    for (size_t i = a; i + 1 < b; ++i) {
        if (t[i + 1].text != "(")
            continue;
        auto e = matching(t, i + 1, "(", ")");
        if (e >= b)
            throw std::runtime_error(tr8("error.facerig.call_outside_face_part"));
        auto name = t[i].text;
        part.calls[name] = {t[i + 1].end, t[e].start};
        if (name == "Position" || name == "Pivot") {
            auto v = numbers(t, i + 2, e);
            if (v.size() < (name == "Pivot" ? 2u : 3u) || v.size() > 3)
                throw std::runtime_error(tr8("error.facerig.invalid_position_pivot_arity"));
            auto &dst = name == "Position" ? part.position : part.pivot;
            for (size_t j = 0; j < v.size(); ++j)
                dst[j] = v[j];
        } else if (name == "TextureArea" || name == "Size") {
            auto v = numbers(t, i + 2, e);
            if (v.size() != (name == "Size" ? 2u : 4u))
                throw std::runtime_error(tr8("error.facerig.invalid_texture_dimensions"));
            for (auto n : v)
                if (n != std::floor(n))
                    throw std::runtime_error(tr8("error.facerig.texturearea_requires_integers"));
            if (name == "Size")
                part.area = {0, 0, int(v[0]), int(v[1])};
            else
                for (int j = 0; j < 4; ++j)
                    part.area[j] = int(v[j]);
        } else if (name == "Texture") {
            if (e != i + 3)
                throw std::runtime_error(tr8("error.facerig.invalid_texture_call"));
            part.textureId = t[i + 2].text;
        } else if (name == "Order") {
            auto v = numbers(t, i + 2, e);
            if (v.size() != 1)
                throw std::runtime_error(tr8("error.facerig.invalid_order_call"));
            part.order = int(v[0]);
        }
        i = e;
    }
}
} // namespace
std::map<std::string, std::string> scriptStringCalls(const std::string &source) {
    auto tokens = lex(source);
    std::map<std::string, std::string> calls;
    for (size_t i = 0; i + 3 < tokens.size(); ++i)
        if (tokens[i + 1].text == "(" && tokens[i + 3].text == ")" && source[tokens[i + 2].start] == '"')
            calls[tokens[i].text] = tokens[i + 2].text;
    return calls;
}
std::string patchScriptStrings(const std::string &source, const std::map<std::string, std::string> &values) {
    auto tokens = lex(source);
    if (std::count_if(tokens.begin(), tokens.end(), [&](const auto &token) {
            return token.text == "NpcResource" && source[token.start] != '"';
        }) != 1)
        throw std::runtime_error(tr8("error.export.npc_syntax"));
    struct Edit {
        size_t start, end;
        std::string text;
    };
    std::vector<Edit> edits;
    size_t body = tokens.size(), close = tokens.size();
    for (size_t i = 0; i < tokens.size(); ++i)
        if (tokens[i].text == "NpcResource" && source[tokens[i].start] != '"') {
            for (body = i + 1; body < tokens.size() && tokens[body].text != "{"; ++body) {
            }
            if (body < tokens.size())
                close = matching(tokens, body, "{", "}");
            break;
        }
    if (close == tokens.size())
        throw std::runtime_error(tr8("error.export.npc_syntax"));
    for (const auto &[name, value] : values) {
        std::ostringstream quoted;
        quoted << std::quoted(value);
        size_t matches = 0;
        int depth = 0;
        for (size_t i = body + 1; i < close; ++i) {
            if (tokens[i].text == "{")
                ++depth;
            if (tokens[i].text == "}")
                --depth;
            auto alias = name;
            alias[0] = char(std::tolower(static_cast<unsigned char>(alias[0])));
            if (depth == 0 && (tokens[i].text == name || tokens[i].text == alias) && i + 1 < close &&
                tokens[i + 1].text == "(") {
                if (i + 3 >= close || tokens[i + 3].text != ")" || source[tokens[i + 2].start] != '"')
                    throw std::runtime_error(tr8("error.export.npc_syntax"));
                edits.push_back({tokens[i + 2].start, tokens[i + 2].end, quoted.str()});
                ++matches;
            }
        }
        if (matches > 1)
            throw std::runtime_error(tr8("error.export.npc_syntax"));
        if (!matches)
            edits.push_back(
                {tokens[close].start, tokens[close].start, "    " + name + "(" + quoted.str() + ");\n"});
    }
    std::stable_sort(edits.begin(), edits.end(),
                     [](const auto &a, const auto &b) { return a.start > b.start; });
    auto result = source;
    for (const auto &edit : edits)
        result.replace(edit.start, edit.end - edit.start, edit.text);
    return result;
}
bool setDefaultFaceDepths(FaceRig &rig) {
    bool changed = false;
    for (auto &part : rig.parts) {
        auto z = part.kind == "Sclera"   ? 0.
                 : part.kind == "Pupil"  ? .01
                 : part.kind == "Eyelid" ? .02
                 : part.kind == "Brow"   ? .03
                                         : .04;
        if (part.position[2] != z) {
            part.position[2] = z;
            changed = true;
        }
    }
    rig.dirty = updatedFaceSource(rig) != rig.source;
    return changed;
}
fs::path findContentPack(fs::path p) {
    if (!fs::is_directory(p))
        p = p.parent_path();
    for (; !p.empty();) {
        if (fs::is_directory(p / L"assets") && fs::is_directory(p / L"data"))
            return p;
        auto parent = p.parent_path();
        if (parent == p)
            break;
        p = parent;
    }
    return {};
}
FaceRig parseFaceRig(std::string source, const fs::path &path, const fs::path &packRoot) {
    FaceRig rig;
    rig.source = std::move(source);
    rig.path = path;
    rig.packRoot = packRoot;
    auto t = lex(rig.source);
    size_t f = 0;
    while (f < t.size() && t[f].text != "face")
        ++f;
    if (f + 5 >= t.size() || t[f + 2].text != "(" || t[f + 4].text != ")" || t[f + 5].text != "{")
        throw std::runtime_error(
            tr8("error.facerig.unsupported_face_rig_syntax_expected_high_level_face_name_id"));
    rig.id = wide(t[f + 3].text);
    auto colon = t[f + 3].text.find(':');
    if (colon == std::string::npos)
        throw std::runtime_error(tr8("error.facerig.face_id_must_be_namespaced"));
    std::string root =
        t[f + 3].text.substr(0, colon) + ":echo_animation/faces/" + t[f + 3].text.substr(colon + 1);
    auto end = matching(t, f + 5, "{", "}");
    for (size_t i = f + 6; i < end;) {
        auto name = t[i].text;
        if (name == "Textures" && i + 3 < end && t[i + 1].text == "(") {
            root = t[i + 2].text;
            i = matching(t, i + 1, "(", ")") + 1;
            if (t[i].text == ";")
                ++i;
            continue;
        }
        if ((name == "Eyes" || name == "Brows") && t[i + 1].text == "{") {
            auto close = matching(t, i + 1, "{", "}");
            std::array<double, 3> groupPivot{};
            for (size_t p = i + 2; p < close;) {
                if (t[p].text == "Pivot" && t[p + 1].text == "(") {
                    auto e = matching(t, p + 1, "(", ")");
                    auto v = numbers(t, p + 2, e);
                    for (size_t j = 0; j < std::min(v.size(), size_t(3)); ++j)
                        groupPivot[j] = v[j];
                    p = e + 2;
                    continue;
                }
                FacePart part;
                part.kind = name == "Brows" ? "Brow" : t[p++].text;
                part.variant = t[p++].text;
                if ((part.kind != "Brow" && part.kind != "Sclera" && part.kind != "Pupil" &&
                     part.kind != "Eyelid") ||
                    (part.variant != "Left" && part.variant != "Right" && part.variant != "Upper" &&
                     part.variant != "Lower"))
                    throw std::runtime_error(tr8("error.facerig.unsupported_face_part"));
                std::string side = part.variant;
                std::transform(side.begin(), side.end(), side.begin(),
                               [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
                std::string kind = part.kind;
                std::transform(kind.begin(), kind.end(), kind.begin(),
                               [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
                part.id = part.kind == "Eyelid" ? side + "_eyelids" : kind + "_" + side;
                part.pivot = groupPivot;
                part.area = {0, 0,
                             part.kind == "Pupil"    ? 1
                             : part.kind == "Eyelid" ? 8
                                                     : 2,
                             part.kind == "Brow" ? 1 : 2};
                part.order = part.kind == "Sclera"   ? 10
                             : part.kind == "Pupil"  ? 15
                             : part.kind == "Eyelid" ? (side == "upper" ? 20 : 22)
                                                     : 30;
                if (side == "right")
                    ++part.order;
                if (t[p].text == "{") {
                    auto e = matching(t, p, "{", "}");
                    part.insert = t[e].start;
                    settings(part, t, p + 1, e);
                    p = e + 1;
                } else
                    throw std::runtime_error(
                        tr8("error.facerig.face_part_requires_an_editable_settings_block"));
                if (part.textureId.empty())
                    part.textureId = root + "/" + part.id;
                else if (part.textureId.find(':') == std::string::npos)
                    part.textureId = root + "/" + part.textureId;
                rig.parts.push_back(std::move(part));
            }
            i = close + 1;
            continue;
        }
        if (name == "Bone" && i + 2 < end && t[i + 2].text == "{") {
            auto e = matching(t, i + 2, "{", "}");
            if (t[i + 1].text == "Glasses") {
                FacePart part;
                part.kind = "Glasses";
                part.id = "glasses";
                part.order = 40;
                part.area = {0, 0, 8, 8};
                part.insert = t[e].start;
                settings(part, t, i + 3, e);
                if (part.textureId.empty())
                    part.textureId = root + "/glasses";
                else if (part.textureId.find(':') == std::string::npos)
                    part.textureId = root + "/" + part.textureId;
                rig.parts.push_back(std::move(part));
            }
            i = e + 1;
            continue;
        }
        if (name == "Blink" && i + 2 < end && t[i + 2].text == "{") {
            rig.blinkId = t[i + 1].text;
            auto e = matching(t, i + 2, "{", "}");
            for (size_t j = i + 3; j + 1 < e; ++j) {
                if (t[j + 1].text != "(")
                    continue;
                auto ce = matching(t, j + 1, "(", ")");
                if (t[j].text != "Idle") {
                    auto v = numbers(t, j + 2, ce);
                    if (t[j].text == "Travel" && v.size() == 2) {
                        rig.upperTravel = v[0];
                        rig.lowerTravel = v[1];
                    } else if (v.size() == 1) {
                        int n = int(v[0]);
                        if (t[j].text == "PeriodTicks")
                            rig.period = n;
                        else if (t[j].text == "CloseTicks")
                            rig.close = n;
                        else if (t[j].text == "HoldTicks")
                            rig.hold = n;
                        else if (t[j].text == "OpenTicks")
                            rig.open = n;
                    }
                }
                j = ce;
            }
            i = e + 1;
            continue;
        }
        throw std::runtime_error(tr8("error.facerig.unsupported_face_member") + name);
    }
    if (rig.parts.empty())
        throw std::runtime_error(tr8("error.facerig.face_rig_has_no_layers"));
    if (rig.period <= 0 || rig.close <= 0 || rig.open <= 0 || rig.hold < 0)
        throw std::runtime_error(tr8("error.facerig.invalid_blink_timing"));
    return rig;
}
void loadFaceTextures(FaceRig &rig) {
    for (auto &part : rig.parts) {
        auto c = part.textureId.find(':');
        if (c == std::string::npos)
            throw std::runtime_error(tr8("error.facerig.invalid_face_texture_id"));
        fs::path resource(wide(part.textureId.substr(c + 1)));
        if (resource.is_absolute() || part.textureId.find("..") != std::string::npos)
            throw std::runtime_error(tr8("error.facerig.unsafe_face_texture_path"));
        if (resource.extension() != L".png")
            resource += L".png";
        part.texturePath =
            rig.packRoot / L"assets" / wide(part.textureId.substr(0, c)) / L"textures" / resource;
        part.texture = loadPng(part.texturePath, false);
        auto a = part.area;
        if (a[0] < 0 || a[1] < 0 || a[2] <= 0 || a[3] <= 0 || a[0] + a[2] > int(part.texture.width) ||
            a[1] + a[3] > int(part.texture.height))
            throw std::runtime_error(tr8("error.facerig.texturearea_outside_png") + part.id);
    }
}
std::map<std::wstring, std::shared_ptr<FaceRig>> loadFaceCatalog(const fs::path &root,
                                                                 std::vector<std::wstring> &diagnostics) {
    std::map<std::wstring, std::shared_ptr<FaceRig>> out;
    auto pack = findContentPack(root);
    if (pack.empty())
        return out;
    for (auto &ns : fs::directory_iterator(pack / L"data")) {
        if (!ns.is_directory())
            continue;
        auto data = ns.path() / L"echo_animation";
        auto rigs = data / L"face_rigs";
        if (!fs::is_directory(rigs))
            continue;
        std::vector<fs::path> related;
        for (auto folder : {L"face_poses", L"face_animations"})
            if (fs::is_directory(data / folder))
                for (auto &entry : fs::recursive_directory_iterator(data / folder))
                    if (entry.is_regular_file() && entry.path().extension() == L".es")
                        related.push_back(entry.path());
        for (auto &entry : fs::recursive_directory_iterator(rigs)) {
            if (!entry.is_regular_file() || entry.path().extension() != L".es")
                continue;
            try {
                auto rig =
                    std::make_shared<FaceRig>(parseFaceRig(readText(entry.path()), entry.path(), pack));
                loadFaceTextures(*rig);
                auto id = utf8(rig->id);
                for (auto &r : related) {
                    auto s = readText(r);
                    if (s.find("\"" + id + "\"") != s.npos)
                        rig->related.push_back(r);
                }
                if (out.contains(rig->id))
                    diagnostics.push_back(tr("ui.facerig.duplicate_face_rig_id") + rig->id);
                else
                    out[rig->id] = rig;
            } catch (const std::exception &e) {
                diagnostics.push_back(entry.path().wstring() + L": " + wide(e.what()));
            }
        }
    }
    // Minecraft loads dialogue projection overrides outside the content-pack.
    fs::path configs;
    for (auto parent = pack.parent_path(); !parent.empty();) {
        if (fs::is_directory(parent / L"configs")) {
            configs = parent / L"configs";
            break;
        }
        auto next = parent.parent_path();
        if (next == parent)
            break;
        parent = next;
    }
    if (!configs.empty()) {
        std::vector<fs::path> files;
        for (auto &entry : fs::recursive_directory_iterator(configs))
            if (entry.is_regular_file() &&
                entry.path().filename().wstring().ends_with(L".portrait-overlay.es"))
                files.push_back(entry.path());
        // Stable precedence: the normal echo_animation folder wins over its copy.
        std::sort(files.begin(), files.end());
        for (auto &file : files) {
            try {
                auto profile = parsePortraitProfile(readText(file), file);
                if (auto it = out.find(profile.id); it != out.end())
                    it->second->profile = std::move(profile);
            } catch (const std::exception &e) {
                diagnostics.push_back(file.wstring() + L": " + wide(e.what()));
            }
        }
    }
    return out;
}
std::string updatedFaceSource(const FaceRig &rig) {
    struct Edit {
        size_t a, b;
        std::string text;
    };
    std::vector<Edit> edits;
    for (auto &p : rig.parts) {
        std::string inserts;
        auto update = [&](const std::string &key, std::vector<double> values) {
            auto value = format(values);
            auto it = p.calls.find(key);
            if (it != p.calls.end()) {
                auto old = lex(rig.source.substr(it->second.start, it->second.end - it->second.start));
                std::vector<double> nums;
                try {
                    nums = numbers(old, 0, old.size());
                } catch (...) {
                    throw std::runtime_error(tr8("error.facerig.cannot_update_source_call"));
                }
                if (nums.size() != values.size())
                    throw std::runtime_error(tr8("error.facerig.cannot_change_source_call_arity"));
                size_t n = 0;
                for (auto &token : old) {
                    if (token.text == ",")
                        continue;
                    if (nums[n] != values[n])
                        edits.push_back({it->second.start + token.start, it->second.start + token.end,
                                         format({values[n]})});
                    ++n;
                }
            } else if (key == "Position" && values != std::vector<double>{0, 0, 0})
                inserts += "\n        " + key + "(" + value + ");\n    ";
            else if (key == "TextureArea" &&
                     p.area != std::array<int, 4>{0, 0, int(p.texture.width), int(p.texture.height)} &&
                     !p.calls.contains("Size"))
                inserts += "\n        " + key + "(" + value + ");\n    ";
        };
        update("Position", {p.position[0], p.position[1], p.position[2]});
        if (p.calls.contains("TextureArea"))
            update("TextureArea",
                   {double(p.area[0]), double(p.area[1]), double(p.area[2]), double(p.area[3])});
        else if (p.calls.contains("Size"))
            update("Size", {double(p.area[2]), double(p.area[3])});
        else
            update("TextureArea",
                   {double(p.area[0]), double(p.area[1]), double(p.area[2]), double(p.area[3])});
        if (!inserts.empty())
            edits.push_back({p.insert, p.insert, inserts});
    }
    auto out = rig.source;
    std::sort(edits.begin(), edits.end(), [](auto &a, auto &b) { return a.a > b.a; });
    for (auto &e : edits)
        out.replace(e.a, e.b - e.a, e.text);
    return out;
}
void saveFaceRig(FaceRig &rig, const fs::path &dest) {
    if (!dest.filename().wstring().ends_with(L".face-rig.es"))
        throw std::runtime_error(tr8("error.facerig.face_rig_destination_must_end_with_face_rig_es"));
    auto source = updatedFaceSource(rig);
    auto fresh = parseFaceRig(source, dest, rig.packRoot);
    loadFaceTextures(fresh);
    if (dest == rig.path && fs::exists(dest) && readText(dest) != rig.source)
        throw std::runtime_error(tr8("error.facerig.face_rig_changed_externally_reload_before_saving"));
    auto tmp = dest;
    tmp += L".tmp-" + std::to_wstring(GetCurrentProcessId());
    HANDLE h =
        CreateFileW(tmp.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (h == INVALID_HANDLE_VALUE)
        throw std::runtime_error(tr8("error.facerig.cannot_create_face_rig_temporary_file"));
    DWORD count = 0;
    bool ok = WriteFile(h, source.data(), DWORD(source.size()), &count, nullptr) && count == source.size() &&
              FlushFileBuffers(h);
    CloseHandle(h);
    if (!ok || !MoveFileExW(tmp.c_str(), dest.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
        DeleteFileW(tmp.c_str());
        throw std::runtime_error(tr8("error.facerig.cannot_save_face_rig"));
    }
    fresh.related = rig.related;
    fresh.profile = std::move(rig.profile);
    rig = std::move(fresh);
}

PortraitProfile parsePortraitProfile(std::string source, const fs::path &path) {
    PortraitProfile profile;
    profile.source = std::move(source);
    profile.path = path;
    auto t = lex(profile.source);
    size_t start = 0;
    while (start < t.size() && t[start].text != "PortraitOverlay")
        ++start;
    if (start + 2 >= t.size() || t[start + 1].text != "(")
        throw std::runtime_error(tr8("error.facerig.missing_portraitoverlay_call"));
    profile.id = wide(t[start + 2].text);
    auto end = matching(t, start + 1, "(", ")");
    for (size_t i = start + 3; i < end; ++i) {
        if (t[i].text == "Layer" && t[i + 1].text == "(") {
            auto e = matching(t, i + 1, "(", ")");
            PortraitLayer layer;
            layer.insert = t[e].start;
            for (size_t j = i + 3; j + 2 < e; ++j) {
                if (t[j + 1].text != ":")
                    continue;
                auto key = t[j].text;
                double v = key == "visible" ? (t[j + 2].text == "false" ? 0 : 1) : number(t[j + 2].text);
                if ((key == "scaleX" || key == "scaleY" || key == "w" || key == "h") && v <= 0)
                    throw std::runtime_error(tr8("error.facerig.portrait_layer_dimensions_must_be_positive"));
                layer.values[key] = v;
                auto span = SourceCall{t[j + 2].start, t[j + 2].end};
                if (profile.source[span.start] == '"') {
                    ++span.start;
                    --span.end;
                }
                layer.spans[key] = span;
                j += 2;
            }
            profile.layers[t[i + 2].text] = std::move(layer);
            i = e;
        } else if (t[i].text == "Blink" && i + 2 < end && t[i + 2].text == "{") {
            auto e = matching(t, i + 2, "{", "}");
            auto prefix = t[i + 1].text + "/";
            for (size_t j = i + 3; j + 1 < e; ++j) {
                if (t[j + 1].text != "(")
                    continue;
                auto ce = matching(t, j + 1, "(", ")");
                if (t[j].text == "Idle")
                    profile.blink[prefix + "Idle"] = t[j + 2].text == "false" ? 0 : 1;
                else {
                    auto v = numbers(t, j + 2, ce);
                    if (t[j].text == "Travel" && v.size() == 2) {
                        profile.blink[prefix + "upperTravel"] = v[0];
                        profile.blink[prefix + "lowerTravel"] = v[1];
                    } else if (v.size() == 1) {
                        if ((t[j].text == "PeriodTicks" || t[j].text == "CloseTicks" ||
                             t[j].text == "OpenTicks") &&
                            v[0] <= 0)
                            throw std::runtime_error(tr8("error.facerig.invalid_profile_blink_timing"));
                        if (t[j].text == "HoldTicks" && v[0] < 0)
                            throw std::runtime_error(tr8("error.facerig.invalid_profile_blink_hold"));
                        profile.blink[prefix + t[j].text] = v[0];
                    }
                }
                j = ce;
            }
            i = e;
        } else if (i + 2 < end && t[i + 1].text == ":") {
            auto key = t[i].text;
            double *value = key == "centerX"      ? &profile.centerX
                            : key == "centerY"    ? &profile.centerY
                            : key == "offsetX"    ? &profile.offsetX
                            : key == "offsetY"    ? &profile.offsetY
                            : key == "facePixels" ? &profile.facePixels
                                                  : nullptr;
            if (value) {
                *value = number(t[i + 2].text);
                i += 2;
            }
        }
    }
    if (profile.facePixels <= 0)
        throw std::runtime_error(tr8("error.facerig.facepixels_must_be_positive"));
    return profile;
}
std::string updatedPortraitSource(const PortraitProfile &profile) {
    struct Edit {
        size_t start, end;
        std::string text;
    };
    std::vector<Edit> edits;
    for (auto &[id, layer] : profile.layers) {
        std::string insert;
        for (auto &[key, value] : layer.values) {
            auto text = key == "visible" ? (value ? "true" : "false") : format({value});
            if (auto it = layer.spans.find(key); it != layer.spans.end()) {
                auto old = profile.source.substr(it->second.start, it->second.end - it->second.start);
                auto oldValue = key == "visible" ? (old == "false" ? 0. : 1.) : number(old);
                if (oldValue != value)
                    edits.push_back({it->second.start, it->second.end, text});
            } else
                insert += ", " + key + ": " + text;
        }
        if (!insert.empty()) {
            if (!layer.insert)
                throw std::runtime_error(tr8("error.facerig.cannot_edit_undeclared_profile_layer"));
            edits.push_back({layer.insert, layer.insert, insert});
        }
    }
    auto out = profile.source;
    std::sort(edits.begin(), edits.end(), [](auto &a, auto &b) { return a.start > b.start; });
    for (auto &e : edits)
        out.replace(e.start, e.end - e.start, e.text);
    return out;
}
void savePortraitProfile(PortraitProfile &profile, const fs::path &dest) {
    if (!dest.filename().wstring().ends_with(L".portrait-overlay.es"))
        throw std::runtime_error(tr8("error.facerig.profile_filename_must_end_with_portrait_overlay_es"));
    auto source = updatedPortraitSource(profile);
    auto fresh = parsePortraitProfile(source, dest);
    if (dest == profile.path && fs::exists(dest) && readText(dest) != profile.source)
        throw std::runtime_error(
            tr8("error.facerig.portrait_profile_changed_externally_reload_before_saving"));
    auto tmp = dest;
    tmp += L".tmp-" + std::to_wstring(GetCurrentProcessId());
    HANDLE file =
        CreateFileW(tmp.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_NEW, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE)
        throw std::runtime_error(tr8("error.facerig.cannot_create_profile_temporary_file"));
    DWORD written = 0;
    bool ok = WriteFile(file, source.data(), DWORD(source.size()), &written, nullptr) &&
              written == source.size() && FlushFileBuffers(file);
    CloseHandle(file);
    if (!ok || !MoveFileExW(tmp.c_str(), dest.c_str(), MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
        DeleteFileW(tmp.c_str());
        throw std::runtime_error(tr8("error.facerig.cannot_save_portrait_profile"));
    }
    profile = std::move(fresh);
}
static double profileBlink(const FaceRig &r, const std::string &key, double fallback) {
    if (r.profile) {
        auto it = r.profile->blink.find(r.blinkId + "/" + key);
        if (it != r.profile->blink.end())
            return it->second;
    }
    return fallback;
}
double blinkClosure(const FaceRig &r, double seconds) {
    double period = profileBlink(r, "PeriodTicks", r.period), close = profileBlink(r, "CloseTicks", r.close),
           hold = profileBlink(r, "HoldTicks", r.hold), open = profileBlink(r, "OpenTicks", r.open);
    if (!profileBlink(r, "Idle", 1))
        return 0;
    double t = std::fmod(seconds * 20, std::max(1., period)), active = close + hold + open;
    t -= std::max(0., period - active);
    if (t < 0)
        return 0;
    if (t < close)
        return t / std::max(1., close);
    t -= close;
    if (t < hold)
        return 1;
    t -= hold;
    return std::max(0., 1 - t / std::max(1., open));
}
std::vector<FaceQuad> faceQuads(const FaceRig &rig, double closure, unsigned size, bool useProfile) {
    std::vector<FaceQuad> out;
    auto profile = useProfile && rig.profile ? &*rig.profile : nullptr;
    double pixels = profile ? profile->facePixels : 8;
    double centerX = profile ? profile->centerX + profile->offsetX / size : .5;
    double centerY = profile ? profile->centerY + profile->offsetY / size : .5;
    for (int i = 0; i < int(rig.parts.size()); ++i) {
        auto &p = rig.parts[i];
        bool eyelid = p.kind == "Eyelid";
        int pieces = eyelid ? 2 : 1;
        for (int piece = 0; piece < pieces; ++piece) {
            FaceQuad q;
            q.part = i;
            q.piece = piece;
            q.order = p.order + piece;
            q.id = eyelid ? (p.variant == "Upper" ? "upper_eyelid_" : "lower_eyelid_") +
                                std::string(piece ? "right" : "left")
                          : p.id;
            q.area = p.area;
            q.area[2] /= pieces;
            q.area[0] += piece * q.area[2];
            q.placeX = p.position[0] + (eyelid ? (piece ? .125 : -.125) : 0);
            q.placeY = p.position[1];
            const PortraitLayer *layer = nullptr;
            if (profile) {
                auto it = profile->layers.find(q.id);
                if (it != profile->layers.end())
                    layer = &it->second;
            }
            auto value = [&](const char *key, double fallback) {
                if (layer) {
                    auto it = layer->values.find(key);
                    if (it != layer->values.end())
                        return it->second;
                }
                return fallback;
            };
            q.placeX = value("placeX", q.placeX);
            q.placeY = value("placeY", q.placeY);
            double travel = p.variant == "Upper" ? rig.upperTravel : rig.lowerTravel;
            if (profile)
                travel = profileBlink(rig, p.variant == "Upper" ? "upperTravel" : "lowerTravel", travel);
            q.x = centerX + q.placeX * 16 / pixels + value("offsetX", 0) / size;
            q.y = centerY + (q.placeY + (eyelid ? closure * travel / 1000 : 0)) * 16 / pixels +
                  value("offsetY", 0) / size;
            // Minecraft GuiGraphics.blit uses integer -drawWidth/2 before scaling (important for 1px pupils).
            double w = std::max(1., std::floor(value("w", q.area[2]) + .5));
            double h = std::max(1., std::floor(value("h", q.area[3]) + .5));
            double sx = value("scaleX", 1) / pixels, sy = value("scaleY", 1) / pixels;
            q.x -= std::floor(w / 2) * sx;
            q.y -= std::floor(h / 2) * sy;
            q.w = w * sx;
            q.h = h * sy;
            if (!profile) {
                q.x -= (w / 2 - std::floor(w / 2)) * sx;
                q.y -= (h / 2 - std::floor(h / 2)) * sy;
            }
            q.visible = p.visible && value("visible", 1) != 0;
            out.push_back(q);
        }
    }
    return out;
}
Image composeFace(const Image &base, const FaceRig &rig, double closure, unsigned size, bool useProfile) {
    Image out{size, size, std::vector<uint8_t>(size_t(size) * size * 4)};
    for (unsigned y = 0; y < size; ++y)
        for (unsigned x = 0; x < size; ++x)
            std::copy_n(&base.rgba[(size_t(y * base.height / size) * base.width + x * base.width / size) * 4],
                        4, &out.rgba[(size_t(y) * size + x) * 4]);
    auto quads = faceQuads(rig, closure, size, useProfile);
    std::stable_sort(quads.begin(), quads.end(), [](auto &a, auto &b) { return a.order < b.order; });
    for (auto &q : quads) {
        auto &p = rig.parts[q.part];
        auto a = q.area;
        if (!q.visible || a[2] <= 0 || a[3] <= 0 || a[0] < 0 || a[1] < 0 ||
            a[0] + a[2] > int(p.texture.width) || a[1] + a[3] > int(p.texture.height))
            continue;
        int x0 = int(std::floor(q.x * size)), y0 = int(std::floor(q.y * size)),
            x1 = int(std::ceil((q.x + q.w) * size)), y1 = int(std::ceil((q.y + q.h) * size));
        for (int y = std::max(0, y0); y < std::min(int(size), y1); ++y)
            for (int x = std::max(0, x0); x < std::min(int(size), x1); ++x) {
                int tx = int(std::floor(((x + .5) / size - q.x) / q.w * a[2])),
                    ty = int(std::floor(((y + .5) / size - q.y) / q.h * a[3]));
                if (tx < 0 || ty < 0 || tx >= a[2] || ty >= a[3])
                    continue;
                auto src = &p.texture.rgba[(size_t(a[1] + ty) * p.texture.width + a[0] + tx) * 4];
                auto dst = &out.rgba[(size_t(y) * size + x) * 4];
                float alphaSrc = src[3] / 255.f, alphaDst = dst[3] / 255.f,
                      alpha = alphaSrc + alphaDst * (1 - alphaSrc);
                for (int c = 0; c < 3; ++c)
                    dst[c] = alpha > 0
                                 ? uint8_t(std::lround(
                                       (src[c] * alphaSrc + dst[c] * alphaDst * (1 - alphaSrc)) / alpha))
                                 : 0;
                dst[3] = uint8_t(std::lround(alpha * 255));
            }
    }
    return out;
}
} // namespace echo
