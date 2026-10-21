#include "portrait/FaceAuthoring.h"
#include <fstream>
#include <sstream>
#include <iomanip>
#include <set>
namespace echo {
namespace {
const uint8_t *pixel(const Image &im, int x, int y) {
    return &im.rgba[(size_t(y) * im.width + x) * 4];
}
double luma(const uint8_t *p) {
    return (p[0] + p[1] + p[2]) / 3.;
}
double distance(const uint8_t *a, const uint8_t *b) {
    double v = 0;
    for (int i = 0; i < 3; ++i)
        v += std::abs(int(a[i]) - int(b[i]));
    return v / 3;
}
bool inside(FaceCrop r, int x, int y) {
    return x >= r.x && y >= r.y && x < r.x + r.w && y < r.y + r.h;
}
Image crop(const Image &face, FaceCrop r) {
    Image out{unsigned(r.w), unsigned(r.h), std::vector<uint8_t>(size_t(r.w) * r.h * 4)};
    for (int y = 0; y < r.h; ++y)
        for (int x = 0; x < r.w; ++x)
            std::copy_n(pixel(face, r.x + x, r.y + y), 4, &out.rgba[(size_t(y) * r.w + x) * 4]);
    return out;
}
std::string quote(const std::wstring &text) {
    std::string out = "\"";
    for (char c : utf8(text)) {
        if (c == '\\' || c == '"')
            out += '\\';
        if (c == '\n')
            out += "\\n";
        else if (c == '\r')
            out += "\\r";
        else
            out += c;
    }
    return out + '"';
}
std::string pos(double x, double y, double z) {
    std::ostringstream out;
    out << std::setprecision(10) << x << ", " << y << ", " << z;
    return out.str();
}
Image logicalFace(const Skin &skin) {
    Image out{8, 8, std::vector<uint8_t>(256)};
    for (int y = 0; y < 8; ++y)
        for (int x = 0; x < 8; ++x)
            std::copy_n(
                pixel(skin.basePortrait, x * skin.basePortrait.width / 8, y * skin.basePortrait.height / 8),
                4, &out.rgba[(y * 8 + x) * 4]);
    return out;
}
} // namespace
FaceDraft detectFaceDraft(std::shared_ptr<Skin> source) {
    FaceDraft d;
    d.source = std::move(source);
    d.face = logicalFace(*d.source);
    const auto *skin = pixel(d.face, 3, 7);
    double best = -1e9;
    int bestY = 4, bestH = 2;
    for (int y = 3; y <= 6; ++y)
        for (int h = 1; h <= 2 && y + h <= 8; ++h) {
            double rowScores[2]{};
            for (int row = 0; row < h; ++row)
                for (int side = 0; side < 2; ++side) {
                    int whiteX = side ? 6 : 1, pupilX = side ? 5 : 2;
                    auto *white = pixel(d.face, whiteX, y + row);
                    auto *pupil = pixel(d.face, pupilX, y + row);
                    rowScores[row] +=
                        std::max(0., luma(white) - luma(skin)) * .5 + distance(white, pupil) * 1.8;
                    rowScores[row] -= distance(white, skin) < 15 ? 45 : 0;
                    if (white[3] < 128 || pupil[3] < 128)
                        rowScores[row] -= 200;
                }
            double score = rowScores[0];
            if (h == 2) {
                double low = std::min(rowScores[0], rowScores[1]),
                       high = std::max(rowScores[0], rowScores[1]);
                // Both rows must contain an eye. Different pupil shading is normal.
                score = (rowScores[0] + rowScores[1]) / 2 + std::max(0., low) * .5;
                if (low < high * .35)
                    score -= 80;
            }
            score -= std::abs(y - 4) * 2;
            if (score > best) {
                best = score;
                bestY = y;
                bestH = h;
            }
        }
    d.crops = {FaceCrop{1, bestY, 2, bestH},
               FaceCrop{5, bestY, 2, bestH},
               FaceCrop{2, bestY, 1, bestH},
               FaceCrop{5, bestY, 1, bestH},
               FaceCrop{1, std::max(0, bestY - 1), 2, 1},
               FaceCrop{5, std::max(0, bestY - 1), 2, 1}};
    d.confidence = float(std::clamp(best / 240., 0., 1.));
    d.aperture = bestH;
    d.brows = bestY > 0 && luma(pixel(d.face, 1, bestY - 1)) < luma(skin) - 30 &&
              luma(pixel(d.face, 5, bestY - 1)) < luma(skin) - 30;
    return d;
}
void setEyeHeight(FaceDraft &d, int height) {
    if (height < 1 || height > 2)
        throw std::runtime_error(tr8("error.faceauthoring.eye_height_must_be_one_or_two_pixels"));
    auto next = d;
    if (height > next.crops[0].h) {
        int oldY = next.crops[0].y, oldH = next.crops[0].h;
        auto evidence = [&](int row) {
            if (row < 0 || row >= 8)
                return -1.;
            double contrast = 0;
            for (int side = 0; side < 2; ++side) {
                auto eye = next.crops[side], pupil = next.crops[2 + side];
                int whiteX = pupil.x == eye.x ? eye.x + eye.w - 1 : eye.x;
                contrast += distance(pixel(next.face, whiteX, row), pixel(next.face, pupil.x, row));
            }
            return contrast;
        };
        int newY = oldY;
        if (oldY + height > 8 || (oldY > 0 && evidence(oldY - 1) > evidence(oldY + oldH)))
            newY--;
        for (int side = 0; side < 2; ++side) {
            next.crops[side].y = newY;
            next.crops[side].h = height;
            auto &pupil = next.crops[2 + side];
            if (pupil.y == oldY && pupil.h == oldH) {
                pupil.y = newY;
                pupil.h = height;
            }
            if (next.brows && next.crops[4 + side].y == oldY - 1)
                next.crops[4 + side].y = std::max(0, newY - 1);
        }
    }
    next.aperture = height;
    validateFaceDraft(next);
    d = std::move(next);
}
void validateFaceDraft(const FaceDraft &d) {
    if (!d.source || d.face.width != 8 || d.face.height != 8)
        throw std::runtime_error(tr8("error.faceauthoring.missing_extraction_source"));
    for (int i = 0; i < 6; ++i) {
        if (i >= 4 && !d.brows)
            continue;
        auto r = d.crops[i];
        if (r.x < 0 || r.y < 0 || r.w < 1 || r.h < 1 || r.x + r.w > 8 || r.y + r.h > 8)
            throw std::runtime_error(tr8("error.faceauthoring.crop_outside_8x8_face"));
    }
    for (int side = 0; side < 2; ++side) {
        auto e = d.crops[side], p = d.crops[2 + side];
        if (e.h > 2 || e.x < side * 4 || e.x + e.w > side * 4 + 4)
            throw std::runtime_error(
                tr8("error.faceauthoring.each_eye_must_fit_its_half_of_the_face_height"));
        if (!inside(e, p.x, p.y) || !inside(e, p.x + p.w - 1, p.y + p.h - 1))
            throw std::runtime_error(tr8("error.faceauthoring.pupil_crop_must_be_inside_its_eye"));
    }
    if (d.crops[0].y != d.crops[1].y || d.crops[0].h != d.crops[1].h)
        throw std::runtime_error(tr8("error.faceauthoring.paired_eyelids_require_the_same_eye_y_and_height"));
    if (d.aperture < 1 || d.aperture > d.crops[0].h)
        throw std::runtime_error(tr8("error.faceauthoring.invalid_eye_aperture"));
    for (auto lid : d.lids)
        if (!std::isfinite(lid.offset) || std::abs(lid.offset) > 4 || lid.height < 1 || lid.height > 4 ||
            !std::isfinite(lid.travel) || lid.travel < 0 || lid.travel > 250)
            throw std::runtime_error(tr8("error.faceauthoring.invalid_eyelid_offset_height_or_blink_travel"));
    if (d.fillX < 0 || d.fillY < 0 || d.fillX >= 8 || d.fillY >= 8 ||
        pixel(d.face, d.fillX, d.fillY)[3] < 128)
        throw std::runtime_error(tr8("error.faceauthoring.choose_an_opaque_skin_fill_sample"));
}
FacePackage prepareFacePackage(const FaceDraft &d, const std::wstring &id, const std::wstring &name,
                               const std::wstring &color, double closure) {
    validateFaceDraft(d);
    if (!validIdentifier(id) || name.empty() || !validColor(color))
        throw std::runtime_error(tr8("error.faceauthoring.valid_npc_id_name_and_rgb_color_required"));
    FacePackage p;
    p.skin = d.source->image;
    const auto *fill = pixel(d.face, d.fillX, d.fillY);
    unsigned factor = p.skin.width / 64;
    for (int y = 0; y < 8; ++y)
        for (int x = 0; x < 8; ++x) {
            bool remove = inside(d.crops[0], x, y) || inside(d.crops[1], x, y) ||
                          (d.brows && (inside(d.crops[4], x, y) || inside(d.crops[5], x, y)));
            if (!remove)
                continue;
            const uint8_t *replacement = fill;
            double best = 1e9;
            for (int yy = 0; yy < 8; ++yy)
                for (int xx = 0; xx < 8; ++xx) {
                    bool masked = inside(d.crops[0], xx, yy) || inside(d.crops[1], xx, yy) ||
                                  (d.brows && (inside(d.crops[4], xx, yy) || inside(d.crops[5], xx, yy)));
                    auto *candidate = pixel(d.face, xx, yy);
                    if (masked || candidate[3] < 128 || distance(candidate, fill) > 35)
                        continue;
                    double score =
                        (xx - x) * (xx - x) + (yy - y) * (yy - y) + distance(candidate, fill) * .02;
                    if (score < best) {
                        best = score;
                        replacement = candidate;
                    }
                }
            for (unsigned dy = 0; dy < factor; ++dy)
                for (unsigned dx = 0; dx < factor; ++dx)
                    std::copy_n(
                        replacement, 4,
                        &p.skin.rgba[(size_t((8 + y) * factor + dy) * p.skin.width + (8 + x) * factor + dx) *
                                     4]);
        }
    for (int side = 0; side < 2; ++side) {
        auto eye = d.crops[side], pupil = d.crops[side + 2];
        Image white = crop(d.face, eye);
        for (int y = 0; y < eye.h; ++y)
            for (int x = 0; x < eye.w; ++x)
                if (inside(pupil, eye.x + x, eye.y + y)) {
                    const uint8_t *sample = nullptr;
                    int best = 100;
                    for (int xx = 0; xx < eye.w; ++xx)
                        if (!inside(pupil, eye.x + xx, eye.y + y) && std::abs(xx - x) < best) {
                            sample = pixel(d.face, eye.x + xx, eye.y + y);
                            best = std::abs(xx - x);
                        }
                    const uint8_t fallback[4] = {uint8_t(y ? 255 : 238), uint8_t(y ? 236 : 222),
                                                 uint8_t(y ? 227 : 215), 255};
                    std::copy_n(sample ? sample : fallback, 4, &white.rgba[(size_t(y) * eye.w + x) * 4]);
                }
        auto suffix = side ? L"_right" : L"_left";
        p.textures[std::wstring(L"sclera") + suffix] = std::move(white);
        p.textures[std::wstring(L"pupil") + suffix] = crop(d.face, pupil);
        if (d.brows)
            p.textures[std::wstring(L"brow") + suffix] = crop(d.face, d.crops[side + 4]);
    }
    for (auto kind : {L"upper_eyelids", L"lower_eyelids"}) {
        Image lid{64, 64, std::vector<uint8_t>(64 * 64 * 4)};
        int row = std::wstring(kind) == L"upper_eyelids" ? 13 : 12;
        int lidHeight = d.lids[std::wstring(kind) == L"upper_eyelids" ? 0 : 1].height;
        for (int ly = 0; ly < lidHeight; ++ly)
            for (int side = 0; side < 2; ++side)
                for (int x = d.crops[side].x; x < d.crops[side].x + d.crops[side].w; ++x)
                    std::copy_n(fill, 4, &lid.rgba[((row + ly) * 64 + 8 + x) * 4]);
        p.textures[kind] = std::move(lid);
    }
    auto eye = d.crops[0];
    double center = eye.y + eye.h / 2.;
    double upper = (center - d.aperture / 2. - d.lids[0].height / 2. + d.lids[0].offset - 4) / 16.,
           lower = (center + d.aperture / 2. + d.lids[1].height / 2. + d.lids[1].offset - 4) / 16.;
    double upperTravel = d.lids[0].travel ? d.lids[0].travel : d.aperture * 31.25;
    double lowerTravel = d.lids[1].travel ? d.lids[1].travel : d.aperture * 31.25;
    std::ostringstream rig, profile;
    rig << "namespace JDH.Generated;\n\nface GeneratedFace(" << quote(id) << ")\n{\n    Eyes\n    {\n";
    const char *kinds[] = {"Sclera Left", "Sclera Right", "Pupil Left", "Pupil Right", "Left", "Right"};
    const char *layerIds[] = {"sclera_left", "sclera_right", "pupil_left",
                              "pupil_right", "brow_left",    "brow_right"};
    profile << "namespace JDH.Generated;\n\nclass GeneratedPortrait : PortraitOverlayConfig\n{\n    public "
               "override PortraitOverlayDef Build()\n    {\n        return PortraitOverlay("
            << quote(id) << ", centerX: 0.5, centerY: 0.5, facePixels: 8,\n            layers: [\n";
    auto part = [&](int i) {
        auto r = d.crops[i];
        double x = (r.x + r.w / 2. - 4) / 16., y = (r.y + r.h / 2. - 4) / 16.;
        rig << "        " << kinds[i] << " { Position(" << pos(x, y, i < 2 ? 0 : (i < 4 ? .01 : .03))
            << "); Size(" << r.w << ", " << r.h << "); }\n";
        profile << "                Layer(\"" << layerIds[i]
                << "\", placeX: " << (r.x + std::floor(r.w / 2.) - 4) / 16.
                << ", placeY: " << (r.y + std::floor(r.h / 2.) - 4) / 16. << "),\n";
    };
    for (int i = 0; i < 4; ++i)
        part(i);
    rig << "        Eyelid Upper { Position(" << pos(0, upper, .02) << "); TextureArea(8, 13, 8, "
        << d.lids[0].height << "); }\n"
        << "        Eyelid Lower { Position(" << pos(0, lower, .02) << "); TextureArea(8, 12, 8, "
        << d.lids[1].height << "); }\n    }\n";
    if (d.brows) {
        rig << "    Brows\n    {\n";
        for (int i = 4; i < 6; ++i)
            part(i);
        rig << "    }\n";
    }
    for (int side = 0; side < 2; ++side)
        for (int lid = 0; lid < 2; ++lid)
            profile << "                Layer(\"" << (lid ? "lower" : "upper") << "_eyelid_"
                    << (side ? "right" : "left") << "\", placeX: " << (side ? .125 : -.125)
                    << ", placeY: " << (lid ? lower : upper) - (d.lids[lid].height % 2 ? .03125 : 0)
                    << "),\n";
    rig << "    Blink \"eye_closure\" { Travel(" << upperTravel << ", " << -lowerTravel
        << "); Gaze(1, 1); Idle(true); PeriodTicks(50); CloseTicks(" << (d.aperture == 1 ? 2 : 3)
        << "); HoldTicks(2); OpenTicks(" << (d.aperture == 1 ? 2 : 3) << "); }\n}\n";
    profile << "            ]);\n    }\n}\n";
    p.rig = rig.str();
    p.profile = profile.str();
    p.npc = "namespace JDH.Generated;\n\nclass GeneratedNpc : NpcResource\n{\n    Id(" + quote(id) +
            ");\n    Display(" + quote(name) + ");\n    Skin(" + quote(id) + ");\n    NameColor(" +
            quote(color) + ");\n}\n";
    std::string uniqueName = utf8(id);
    for (auto &c : uniqueName)
        if (c == ':' || c == '/' || c == '-' || c == '.')
            c = '_';
    p.rig.replace(p.rig.find("GeneratedFace("), 13, "GeneratedFace_" + uniqueName);
    p.profile.replace(p.profile.find("GeneratedPortrait :"), 17, "GeneratedPortrait_" + uniqueName);
    p.npc.replace(p.npc.find("GeneratedNpc :"), 12, "GeneratedNpc_" + uniqueName);
    auto preview = parseFaceRig(p.rig, {}, {});
    preview.profile = parsePortraitProfile(p.profile, {});
    for (auto &layer : preview.parts)
        layer.texture = p.textures.at(wide(layer.id));
    p.portrait = composeFace(makePortrait(p.skin), preview, closure);
    return p;
}
Npc writeFacePackage(const FaceDraft &d, const fs::path &pack, const std::wstring &id,
                     const std::wstring &name, const std::wstring &color, bool slim, const Npc *current) {
    if (findContentPack(pack) != fs::weakly_canonical(pack))
        throw std::runtime_error(tr8("error.faceauthoring.select_content_pack_root_with_assets_and_data"));
    auto package = prepareFacePackage(d, id, name, color);
    auto colon = id.find(L':');
    auto ns = id.substr(0, colon);
    fs::path relative = id.substr(colon + 1);
    if (relative.is_absolute() || id.find(L"..") != id.npos)
        throw std::runtime_error(tr8("error.faceauthoring.unsafe_npc_resource_path"));
    auto skinPath = pack / L"assets" / ns / L"textures" / L"echo_npc" / L"skins" / relative;
    skinPath += L".png";
    auto faceFolder = pack / L"assets" / ns / L"textures" / L"echo_animation" / L"faces" / relative;
    auto rigPath = pack / L"data" / ns / L"echo_animation" / L"face_rigs" / L"generated" / relative;
    rigPath += L".face-rig.es";
    auto npcPath = pack / L"data" / ns / L"echo_npc" / L"npcs" / L"generated" / relative;
    npcPath += L".npc.es";
    auto story = pack.parent_path().parent_path();
    if (story.filename() != L"story")
        throw std::runtime_error(
            tr8("error.faceauthoring.content_pack_must_be_under_story_content_pack_for_portrait"));
    auto profilePath =
        story / L"configs" / ns / L"echo_animation" / L"portrait_overlays" / L"generated" / relative;
    profilePath += L".portrait-overlay.es";
    auto metadata = skinPath;
    metadata.replace_extension(L".echo-npc");
    if (current) {
        if (current->faceRig || current->id != id)
            throw std::runtime_error(tr8("error.faceauthoring.use_portrait_editor_for_an_existing_face_rig"));
        auto relativeSkin =
            fs::weakly_canonical(current->skin->path).lexically_relative(fs::weakly_canonical(pack));
        if (!relativeSkin.empty() && *relativeSkin.begin() != L"..")
            skinPath = current->skin->path;
        if (!current->gameDefinition.empty())
            npcPath = current->gameDefinition;
        if (!current->metadata.empty())
            metadata = current->metadata;
        if (fs::exists(current->skin->path) &&
            loadSkin(current->skin->path)->image.rgba != current->skin->image.rgba)
            throw std::runtime_error(
                tr8("error.faceauthoring.source_skin_changed_externally_reload_before_editing_face"));
    }
    auto mayReplace = [&](const fs::path &path) {
        return current &&
               (path == current->skin->path || path == current->metadata || path == current->gameDefinition);
    };
    std::vector<fs::path> targets{skinPath, rigPath, npcPath, profilePath, metadata};
    for (auto &[key, image] : package.textures)
        targets.push_back(faceFolder / (key + L".png"));
    targets.push_back(faceFolder / L"source_skin.png");
    for (auto &path : targets)
        if (fs::exists(path) && !mayReplace(path))
            throw std::runtime_error(
                tr8("error.faceauthoring.resource_already_exists_choose_a_new_npc_id_existing_resources"));
    // Prepare every file in a private directory before committing new destinations.
    auto stage = fs::temp_directory_path() / (L"Echo-face-stage-" + std::to_wstring(GetCurrentProcessId()) +
                                              L"-" + std::to_wstring(GetTickCount64()));
    fs::create_directory(stage);
    std::vector<fs::path> committed;
    std::map<fs::path, fs::path> backups;
    try {
        savePng(stage / L"skin.png", package.skin);
        savePng(stage / L"source_skin.png", d.source->image);
        auto write = [&](const wchar_t *file, const std::string &text) {
            std::ofstream out(stage / file, std::ios::binary);
            out << text;
            if (!out)
                throw std::runtime_error(tr8("error.faceauthoring.cannot_stage_face_script"));
        };
        write(L"rig.es", package.rig);
        if (current && fs::exists(npcPath))
            fs::copy_file(npcPath, stage / L"npc.es");
        else
            write(L"npc.es", package.npc);
        write(L"profile.es", package.profile);
        for (auto &[key, image] : package.textures)
            savePng(stage / (key + L".png"), image);
        Npc n = current ? *current : npcFromSkin(loadSkin(stage / L"skin.png"));
        n.skin = loadSkin(stage / L"skin.png");
        n.skin->path = skinPath;
        n.id = id;
        n.name = name;
        n.color = color;
        n.slim = slim;
        saveMetadata(n, stage / L"metadata.echo-npc");
        n.extra["skin"] = utf8(skinPath.wstring());
        {
            std::ofstream out(stage / L"metadata.echo-npc", std::ios::binary | std::ios::trunc);
            out << "ECHO_NPC_STUDIO 1\n";
            for (auto &[key, value] : n.extra)
                out << key << ' ' << std::quoted(value) << '\n';
            if (!out)
                throw std::runtime_error(tr8("error.faceauthoring.cannot_stage_npc_metadata"));
        }
        std::vector<fs::path> files{stage / L"skin.png", stage / L"rig.es", stage / L"npc.es",
                                    stage / L"profile.es", stage / L"metadata.echo-npc"};
        for (auto &[key, image] : package.textures)
            files.push_back(stage / (key + L".png"));
        files.push_back(stage / L"source_skin.png");
        for (size_t i = 0; i < targets.size(); ++i) {
            fs::create_directories(targets[i].parent_path());
            if (fs::exists(targets[i])) {
                auto backup = stage / (L"backup-" + std::to_wstring(i));
                fs::copy_file(targets[i], backup);
                backups[targets[i]] = backup;
            }
            fs::copy_file(files[i], targets[i],
                          backups.contains(targets[i]) ? fs::copy_options::overwrite_existing
                                                       : fs::copy_options::none);
            committed.push_back(targets[i]);
        }
        n.skin = loadSkin(skinPath);
        n.metadata = metadata;
        n.gameDefinition = npcPath;
        {
            std::ifstream input(npcPath, std::ios::binary);
            n.gameSource.assign(std::istreambuf_iterator<char>(input), {});
        }
        n.faceRig = std::make_shared<FaceRig>(parseFaceRig(package.rig, rigPath, pack));
        loadFaceTextures(*n.faceRig);
        n.faceRig->profile = parsePortraitProfile(package.profile, profilePath);
        n.animationPortrait = package.portrait;
        for (auto &[target, backup] : backups)
            fs::remove(backup);
        for (auto &file : files)
            fs::remove(file);
        fs::remove(stage);
        return n;
    } catch (...) {
        for (auto &[target, backup] : backups)
            fs::copy_file(backup, target, fs::copy_options::overwrite_existing);
        for (auto &path : committed)
            if (!backups.contains(path))
                fs::remove(path);
        for (auto &entry : fs::directory_iterator(stage))
            if (entry.is_regular_file())
                fs::remove(entry.path());
        fs::remove(stage);
        throw;
    }
}
} // namespace echo
