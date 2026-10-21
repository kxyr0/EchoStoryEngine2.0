#include "app/Application.h"
#include <sstream>
#include <iomanip>
namespace echo {
std::wstring Application::portraitFields() const {
    return partX.value + L"\n" + partY.value + L"\n" + partZ.value + L"\n" + areaX.value + L"\n" +
           areaY.value + L"\n" + areaW.value + L"\n" + areaH.value;
}
static std::wstring numeric(double value) {
    std::wostringstream s;
    s << std::setprecision(8) << value;
    return s.str();
}

Application::PortraitState Application::capturePortrait() const {
    PortraitState state;
    auto &rig = *npcs[selected].faceRig;
    for (auto &part : rig.parts)
        state.parts.push_back({part.position, part.area, part.visible});
    if (rig.profile)
        for (auto &[id, layer] : rig.profile->layers)
            state.layers[id] = layer.values;
    return state;
}
void Application::recordPortrait() {
    auto &history = portraitHistories[npcs[selected].faceRig.get()];
    history.undo.push_back(capturePortrait());
    if (history.undo.size() > 200)
        history.undo.erase(history.undo.begin());
    history.redo.clear();
}
void Application::restorePortrait(const PortraitState &state) {
    auto &rig = *npcs[selected].faceRig;
    for (size_t i = 0; i < rig.parts.size(); ++i) {
        rig.parts[i].position = state.parts[i].position;
        rig.parts[i].area = state.parts[i].area;
        rig.parts[i].visible = state.parts[i].visible;
    }
    if (rig.profile) {
        for (auto &[id, layer] : rig.profile->layers) {
            auto it = state.layers.find(id);
            layer.values = it == state.layers.end() ? std::map<std::string, double>{} : it->second;
        }

        auto neutral = faceQuads(rig, 0, 256, false);
        for (auto &[id, layer] : rig.profile->layers) {
            for (auto &[key, span] : layer.spans)
                if (!layer.values.contains(key)) {
                    if (key == "visible")
                        layer.values[key] = 1;
                    if (key == "placeX" || key == "placeY" || key == "w" || key == "h") {
                        auto it =
                            std::find_if(neutral.begin(), neutral.end(), [&](auto &q) { return q.id == id; });
                        if (it != neutral.end())
                            layer.values[key] = key == "placeX"   ? it->placeX
                                                : key == "placeY" ? it->placeY
                                                : key == "w"      ? it->area[2]
                                                                  : it->area[3];
                    }
                }
        }
        rig.profile->dirty = updatedPortraitSource(*rig.profile) != rig.profile->source;
    }
    rig.dirty = updatedFaceSource(rig) != rig.source;
    refreshPortraitFields();
    rebuildPortrait();
}
void Application::portraitUndo(bool forward) {
    if (selected < 0 || !npcs[selected].faceRig || portraitDragging)
        return;
    if (portraitFields() != portraitSnapshot) {
        refreshPortraitFields(); // Discard unapplied numeric values first.
        if (!forward)
            return;
    }
    auto &history = portraitHistories[npcs[selected].faceRig.get()];
    auto &from = forward ? history.redo : history.undo;
    auto &to = forward ? history.undo : history.redo;
    if (from.empty())
        return;
    to.push_back(capturePortrait());
    auto state = std::move(from.back());
    from.pop_back();
    restorePortrait(state);
    status = forward ? tr("ui.portraitpanel.portrait_action_redone")
                     : tr("ui.portraitpanel.portrait_action_undone");
}
void Application::refreshPortraitFields() {
    if (selected < 0 || !npcs[selected].faceRig || portraitPart < 0)
        return;
    auto &rig = *npcs[selected].faceRig;
    auto q = faceQuads(rig).at(portraitPart);
    auto &part = rig.parts[q.part];
    partX.set(numeric(q.placeX));
    partY.set(numeric(q.placeY));
    partZ.set(numeric(part.position[2]));
    areaX.set(std::to_wstring(part.area[0]));
    areaY.set(std::to_wstring(part.area[1]));
    areaW.set(std::to_wstring(part.area[2]));
    areaH.set(std::to_wstring(part.area[3]));
    if (rig.profile && rig.profile->layers.contains(q.id)) {
        auto &layer = rig.profile->layers.at(q.id).values;
        areaW.set(numeric(layer.contains("w") ? layer.at("w") : q.area[2]));
        areaH.set(numeric(layer.contains("h") ? layer.at("h") : q.area[3]));
    }
    portraitSnapshot = portraitFields();
}
void Application::selectPortraitPart(int index) {
    if (selected < 0 || !npcs[selected].faceRig)
        return;
    if (portraitPart >= 0)
        applyPortraitPart();
    if (index < 0 || index >= int(faceQuads(*npcs[selected].faceRig).size()))
        return;
    portraitPart = index;
    refreshPortraitFields();
    ui.clearFocus();
}
bool Application::portraitIncluded(const FaceQuad &q, int index) const {
    if (portraitGroup == 0)
        return index == portraitPart;
    if (portraitGroup == 1)
        return q.id.ends_with("_left") && !q.id.starts_with("brow");
    if (portraitGroup == 2)
        return q.id.ends_with("_right") && !q.id.starts_with("brow");
    if (portraitGroup == 3)
        return q.id.starts_with("pupil");
    if (portraitGroup == 4)
        return q.id.starts_with("brow");
    return true;
}
void Application::movePortrait(double dx, double dy, bool record) {
    if (selected < 0 || !npcs[selected].faceRig || portraitPart < 0 || (dx == 0 && dy == 0))
        return;
    auto &rig = *npcs[selected].faceRig;
    auto quads = faceQuads(rig);
    // Check every selected profile layer before committing the group as one operation.
    for (size_t i = 0; i < quads.size(); ++i)
        if (portraitIncluded(quads[i], int(i))) {
            auto &q = quads[i];
            if (rig.profile && !rig.profile->layers.contains(q.id))
                throw std::runtime_error(tr8("error.portraitpanel.layer_missing_from_portrait_profile_use_an_"
                                             "existing_declared_layer"));
            if (std::abs(q.placeX + dx) > 10000 || std::abs(q.placeY + dy) > 10000)
                throw std::runtime_error(
                    tr8("error.portraitpanel.portrait_position_outside_supported_range"));
        }
    if (record)
        recordPortrait();
    std::vector<int> moved;
    for (size_t i = 0; i < quads.size(); ++i)
        if (portraitIncluded(quads[i], int(i))) {
            auto &q = quads[i];
            if (rig.profile) {
                auto &layer = rig.profile->layers.at(q.id);
                layer.values["placeX"] = q.placeX + dx;
                layer.values["placeY"] = q.placeY + dy;
            } else if (std::find(moved.begin(), moved.end(), q.part) == moved.end()) {
                rig.parts[q.part].position[0] += dx;
                rig.parts[q.part].position[1] += dy;
                moved.push_back(q.part);
            }
        }
    if (rig.profile)
        rig.profile->dirty = updatedPortraitSource(*rig.profile) != rig.profile->source;
    else
        rig.dirty = updatedFaceSource(rig) != rig.source;
    refreshPortraitFields();
    rebuildPortrait();
}
void Application::applyPortraitPart() {
    if (selected < 0 || !npcs[selected].faceRig || portraitPart < 0 || portraitFields() == portraitSnapshot)
        return;
    auto &rig = *npcs[selected].faceRig;
    auto q = faceQuads(rig).at(portraitPart);
    auto &part = rig.parts[q.part];
    auto number = [](const std::wstring &text) {
        size_t n;
        double v = std::stod(text, &n);
        if (n != text.size() || !std::isfinite(v) || std::abs(v) > 10000)
            throw std::runtime_error(tr8("error.portraitpanel.invalid_face_layer_numeric_value"));
        return v;
    };
    double x = number(partX.value), y = number(partY.value), z = number(partZ.value);
    if (z < 0)
        throw std::runtime_error(tr8("error.portraitpanel.negative_depth"));
    if (rig.profile) {
        if (!rig.profile->layers.contains(q.id))
            throw std::runtime_error(tr8("error.portraitpanel.layer_missing_from_portrait_profile"));
        auto &layer = rig.profile->layers.at(q.id);
        double w = number(areaW.value), h = number(areaH.value);
        if (w < 1 || h < 1 || w > 4096 || h > 4096 || w != std::floor(w) || h != std::floor(h))
            throw std::runtime_error(tr8("error.portraitpanel.portrait_size_must_be_integer_pixels_1_4096"));
        double oldW = layer.values.contains("w") ? layer.values.at("w") : q.area[2];
        double oldH = layer.values.contains("h") ? layer.values.at("h") : q.area[3];
        if (x != q.placeX || y != q.placeY || w != oldW || h != oldH || z != part.position[2]) {
            recordPortrait();
            part.position[2] = z;
            rig.dirty = updatedFaceSource(rig) != rig.source;
            layer.values["placeX"] = x;
            layer.values["placeY"] = y;
            if (w != oldW)
                layer.values["w"] = w;
            if (h != oldH)
                layer.values["h"] = h;
            rig.profile->dirty = updatedPortraitSource(*rig.profile) != rig.profile->source;
            rebuildPortrait();
        }
    } else {
        std::array<double, 3> position{x - (part.kind == "Eyelid" ? (q.piece ? .125 : -.125) : 0), y, z};
        std::array<int, 4> area{};
        TextEdit *inputs[] = {&areaX, &areaY, &areaW, &areaH};
        for (int i = 0; i < 4; ++i) {
            auto v = number(inputs[i]->value);
            if (v != std::floor(v))
                throw std::runtime_error(tr8("error.portraitpanel.texturearea_must_use_integer_pixels"));
            area[i] = int(v);
        }
        if (area[0] < 0 || area[1] < 0 || area[2] <= 0 || area[3] <= 0 ||
            area[0] + area[2] > int(part.texture.width) || area[1] + area[3] > int(part.texture.height) ||
            (part.kind == "Eyelid" && area[2] % 2))
            throw std::runtime_error(
                tr8("error.portraitpanel.texturearea_outside_png_or_eyelid_width_is_not_even"));
        if (part.calls.contains("Size") && !part.calls.contains("TextureArea") &&
            (area[0] != 0 || area[1] != 0))
            throw std::runtime_error(tr8("error.portraitpanel.size_requires_zero_atlas_x_y"));
        if (position != part.position || area != part.area) {
            recordPortrait();
            part.position = position;
            part.area = area;
            rig.dirty = updatedFaceSource(rig) != rig.source;
            rebuildPortrait();
        }
    }
    portraitSnapshot = portraitFields();
}
void Application::rebuildPortrait() {
    graphics.invalidateImages();
    if (selected < 0 || !npcs[selected].faceRig) {
        portraitComposite = {};
        return;
    }
    auto &npc = npcs[selected];
    portraitComposite =
        composeFace(npc.hat ? npc.skin->portrait : npc.skin->basePortrait, *npc.faceRig, portraitClosure);
    npc.animationPortrait = portraitComposite;
}

void Application::savePortraitRig(bool saveAs) {
    if (selected < 0 || !npcs[selected].faceRig)
        return;
    applyPortraitPart();
    auto &rig = *npcs[selected].faceRig;
    auto path = rig.profile ? rig.profile->path : rig.path;
    if (saveAs) {
        path = rig.profile ? fileDialog(true, tr("ui.portraitpanel.portrait_profile_portrait_overlay_es"),
                                        L"portrait-overlay.es")
                           : fileDialog(true, tr("ui.portraitpanel.face_rig_face_rig_es"), L"face-rig.es");
        if (path.empty())
            return;
    } else if (messageBox(window,
                          (tr("ui.portraitpanel.save_changes") + path.wstring() +
                           (rig.profile && rig.dirty ? L"\n" + rig.path.wstring() : L""))
                              .c_str(),
                          tr("ui.portraitpanel.portrait_editor"), MB_YESNO | MB_ICONQUESTION) != IDYES)
        return;
    if (rig.profile && rig.dirty && !saveAs)
        saveFaceRig(rig, rig.path);
    if (rig.profile)
        savePortraitProfile(*rig.profile, path);
    else
        saveFaceRig(rig, path);
    // Saved source spans can change; history remains valid because it stores values only.
    refreshPortraitFields();
    status = tr("ui.portraitpanel.saved") + path.wstring();
    rebuildPortrait();
}
void Application::exportComposedPortrait() {
    if (selected < 0 || !npcs[selected].faceRig)
        return;
    applyPortraitPart();
    auto path = fileDialog(true, tr("ui.portraitpanel.png_portrait_png"), L"png");
    if (path.empty())
        return;
    for (auto &n : npcs) {
        if (fs::exists(path) && fs::equivalent(path, n.skin->path))
            throw std::runtime_error(tr8("error.portraitpanel.cannot_replace_source_skin_with_portrait"));
        if (n.faceRig)
            for (auto &p : n.faceRig->parts)
                if (fs::exists(path) && fs::equivalent(path, p.texturePath))
                    throw std::runtime_error(
                        tr8("error.portraitpanel.cannot_replace_source_face_layer_with_portrait"));
    }
    savePng(path, portraitComposite);
    status = tr("ui.portraitpanel.composed_portrait_exported") + path.wstring();
}
void Application::portraitPanel() {
    float end = height - bottom;
    graphics.clip(rect(0, 110, left, end - 110));
    size_t begin = ui.widgets.size();
    float x = 18, w = left - 36, y = 116 - portraitScroll;
    if (selected < 0 || !npcs[selected].faceRig) {
        graphics.text(tr("ui.portraitpanel.echo_animation_select_an_npc_with_a_face_rig_open"),
                      rect(x, y, w, 155), 0x969BA6);
        ui.button(
            200, tr("ui.portraitpanel.open_content_pack"), rect(x, y + 178, w, 34),
            [this] { openProject(folderDialog()); }, !scanning);
        graphics.unclip();
        return;
    }

    auto &rig = *npcs[selected].faceRig;
    bool profile = rig.profile.has_value();
    graphics.text(rig.id + ((rig.dirty || (profile && rig.profile->dirty)) ? L" *" : L""), rect(x, y, w, 24),
                  0x62AEFA);
    y += 27;
    graphics.text(profile ? tr("ui.portraitpanel.minecraft_profile_connected")
                          : tr("ui.portraitpanel.face_rig_no_portrait_profile"),
                  rect(x, y, w, 22), 0x969BA6);
    y += 27;
    ui.button(566, tr("ui.portraitpanel.default_depth"), rect(x, y, w, 30), [this] {
        applyPortraitPart();
        auto preview = *npcs[selected].faceRig;
        if (setDefaultFaceDepths(preview)) {
            recordPortrait();
            setDefaultFaceDepths(*npcs[selected].faceRig);
            refreshPortraitFields();
            rebuildPortrait();
        }
    });
    y += 37;
    ui.button(201, profile ? tr("ui.portraitpanel.save_profile") : tr("ui.portraitpanel.save_rig"),
              rect(x, y, w * .64f - 4, 32), [this] { savePortraitRig(); });
    ui.button(202, tr("ui.panels.save_as"), rect(x + w * .64f + 4, y, w * .36f - 4, 32),
              [this] { savePortraitRig(true); });
    y += 39;
    auto &history = portraitHistories[&rig];
    ui.button(
        310, tr("ui.portraitpanel.ctrl_z"), rect(x, y, w / 2 - 4, 29), [this] { portraitUndo(); },
        !history.undo.empty());
    ui.button(
        311, tr("ui.portraitpanel.ctrl_y"), rect(x + w / 2 + 4, y, w / 2 - 4, 29),
        [this] { portraitUndo(true); }, !history.redo.empty());
    y += 36;
    const std::wstring groups[] = {tr("ui.portraitpanel.element"),   tr("ui.portraitpanel.left_eye"),
                                   tr("ui.portraitpanel.right_eye"), tr("ui.portraitpanel.pupils"),
                                   tr("ui.portraitpanel.brows"),     tr("ui.portraitpanel.whole_face")};
    for (int i = 0; i < 6; ++i) {
        ui.button(
            320 + i, groups[i], rect(x + (i % 2) * (w / 2 + 4), y, w / 2 - 4, 25),
            [this, i] {
                applyPortraitPart();
                portraitGroup = i;
                if (portraitPart < 0)
                    selectPortraitPart(0);
                ui.clearFocus();
            },
            true, portraitGroup == i);
        if (i % 2)
            y += 29;
    }
    y += 5;
    ui.button(263, tr("ui.control.move_left"), rect(x, y, 36, 28), [this] {
        applyPortraitPart();
        movePortrait(-portraitStep / 16, 0);
    });
    ui.button(264, tr("ui.control.move_right"), rect(x + 42, y, 36, 28), [this] {
        applyPortraitPart();
        movePortrait(portraitStep / 16, 0);
    });
    ui.button(265, tr("ui.control.move_up"), rect(x + 84, y, 36, 28), [this] {
        applyPortraitPart();
        movePortrait(0, -portraitStep / 16);
    });
    ui.button(266, tr("ui.control.move_down"), rect(x + 126, y, 36, 28), [this] {
        applyPortraitPart();
        movePortrait(0, portraitStep / 16);
    });
    y += 34;
    const double steps[] = {.01, .1, 1};
    const std::wstring labels[] = {tr("ui.portraitpanel.0_01_px"), tr("ui.portraitpanel.0_1_px"),
                                   tr("ui.portraitpanel.1_px")};
    for (int i = 0; i < 3; ++i)
        ui.button(
            330 + i, labels[i], rect(x + i * (w / 3 + 2), y, w / 3 - 4, 25),
            [this, i] {
                const double values[] = {.01, .1, 1};
                portraitStep = values[i];
            },
            true, portraitStep == steps[i]);
    y += 40;
    y += 4;
    auto quads = faceQuads(rig);
    for (size_t i = 0; i < quads.size(); ++i) {
        auto q = quads[i];
        static const std::map<std::string, std::wstring> partLabels = {
            {"sclera_left", tr("ui.facewizard.left_eye")},
            {"sclera_right", tr("ui.facewizard.right_eye")},
            {"pupil_left", tr("ui.facewizard.left_pupil")},
            {"pupil_right", tr("ui.facewizard.right_pupil")},
            {"brow_left", tr("ui.facewizard.left_brow")},
            {"brow_right", tr("ui.facewizard.right_brow")},
            {"upper_eyelid_left", tr("ui.portraitpanel.upper_left_eyelid")},
            {"upper_eyelid_right", tr("ui.portraitpanel.upper_right_eyelid")},
            {"lower_eyelid_left", tr("ui.portraitpanel.lower_left_eyelid")},
            {"lower_eyelid_right", tr("ui.portraitpanel.lower_right_eyelid")},
            {"glasses", tr("ui.portraitpanel.glasses")}};
        auto label = partLabels.contains(q.id) ? partLabels.at(q.id) : wide(q.id);
        ui.button(
            220 + int(i), label, rect(x, y, w - 40, 25), [this, i] { selectPortraitPart(int(i)); }, true,
            portraitPart == int(i));
        ui.button(240 + int(i), q.visible ? tr("ui.control.visible") : tr("ui.control.hidden"),
                  rect(x + w - 33, y, 33, 25), [this, q] {
                      auto &faceRig = *npcs[selected].faceRig;
                      applyPortraitPart();
                      recordPortrait();
                      if (faceRig.profile && faceRig.profile->layers.contains(q.id)) {
                          faceRig.profile->layers.at(q.id).values["visible"] = q.visible ? 0 : 1;
                          faceRig.profile->dirty =
                              updatedPortraitSource(*faceRig.profile) != faceRig.profile->source;
                      } else
                          faceRig.parts[q.part].visible = !faceRig.parts[q.part].visible;
                      rebuildPortrait();
                  });
        y += 29;
    }
    if (portraitPart >= 0) {
        auto &part = rig.parts[quads[portraitPart].part];
        y += 10;
        graphics.text(profile ? tr("ui.portraitpanel.profile_place_x_y_depth")
                              : tr("ui.portraitpanel.position_x_y_z"),
                      rect(x, y, w, 20), 0x969BA6);
        y += 25;
        float fw = (w - 16) / 3;
        ui.input(260, partX, rect(x, y, fw, 32));
        ui.input(261, partY, rect(x + fw + 8, y, fw, 32));
        ui.input(262, partZ, rect(x + 2 * (fw + 8), y, fw, 32));
        y += 40;

        graphics.text(profile ? tr("ui.portraitpanel.portrait_size_w_h")
                              : tr("ui.portraitpanel.texturearea_x_y_w_h"),
                      rect(x, y, w, 20), 0x969BA6);
        y += 25;
        float aw = (w - 18) / 4;
        TextEdit *fields[] = {&areaX, &areaY, &areaW, &areaH};
        for (int i = 0; i < 4; ++i)
            ui.input(270 + i, *fields[i], rect(x + i * (aw + 6), y, aw, 32), !profile || i >= 2);
        y += 43;
        ui.button(275, tr("ui.portraitpanel.apply_parameters"), rect(x, y, w, 32),
                  [this] { applyPortraitPart(); });
        y += 44;
        graphics.text(part.texturePath.filename().wstring() + L" · " + std::to_wstring(part.texture.width) +
                          L"×" + std::to_wstring(part.texture.height),
                      rect(x, y, w, 35), 0x969BA6);
        y += 43;
    }
    ui.button(276, tr("ui.portraitpanel.reveal_source_in_explorer"), rect(x, y, w, 32), [this] {
        auto &r = *npcs[selected].faceRig;
        reveal(r.profile ? r.profile->path : r.path);
    });
    y += 42;
    portraitMaxScroll = std::max(0.f, y + portraitScroll - end + 15);
    graphics.unclip();
    for (size_t i = begin; i < ui.widgets.size(); ++i)
        if (ui.widgets[i].bounds.top < 110 || ui.widgets[i].bounds.bottom > end)
            ui.widgets[i].enabled = false;
    if (y > end)
        graphics.text(tr("ui.portraitpanel.scroll_for_layer_settings"), rect(18, end - 20, left - 36, 18),
                      0x969BA6);
}
void Application::portraitPreview() {
    float end = height - bottom, cw = width - right - left;
    graphics.fill(rect(left + 3, 58, cw - 6, end - 58), 0x101216);
    graphics.text(tr("ui.portraitpanel.portrait_editor_echo_animation"), rect(left + 18, 73, cw - 36, 27),
                  0xE6E8EC, 1);
    if (selected < 0 || !npcs[selected].faceRig) {
        graphics.text(tr("ui.portraitpanel.no_face_rig_found_for_this_npc_check_diagnostics_for"),
                      rect(left + 30, 160, cw - 60, 90), 0x969BA6);
        return;
    }
    float size = std::max(80.f, std::min(cw - 72, end - 210)), x = left + (cw - size) / 2, y = 116;
    portraitCanvas = rect(x, y, size, size);
    for (int yy = 0; yy < 16; ++yy)
        for (int xx = 0; xx < 16; ++xx)
            graphics.fill(rect(x + xx * size / 16, y + yy * size / 16, size / 16, size / 16),
                          (xx + yy) % 2 ? 0x252A32 : 0x303743);
    graphics.image(portraitComposite, portraitCanvas);
    if (portraitGrid)
        for (int i = 0; i <= 8; ++i) {
            float a = i * size / 8;
            graphics.line(x + a, y, x + a, y + size, 0x606A7B, .6f);
            graphics.line(x, y + a, x + size, y + a, 0x606A7B, .6f);
        }

    auto &rig = *npcs[selected].faceRig;
    auto quads = faceQuads(rig, portraitClosure);
    for (size_t i = 0; i < quads.size(); ++i)
        if (portraitPart >= 0 && portraitIncluded(quads[i], int(i))) {
            auto &q = quads[i];
            float px = x + size * float(q.x), py = y + size * float(q.y), pw = size * float(q.w),
                  ph = size * float(q.h);
            graphics.contrastOutline(portraitComposite, portraitCanvas, rect(px, py, pw, ph));
        }
    float by = y + size + 16;
    ui.toggle(280, tr("ui.portraitpanel.grid"), rect(left + 18, by, 95, 31), portraitGrid,
              [this] { portraitGrid = !portraitGrid; });
    ui.toggle(281, tr("ui.portraitpanel.blink"), rect(left + 122, by, 124, 31), portraitBlink, [this] {
        portraitBlink = !portraitBlink;
        if (portraitBlink)
            SetTimer(window, 6, 33, nullptr);
        else {
            KillTimer(window, 6);
            portraitClosure = 0;
            rebuildPortrait();
        }
    });
    ui.button(
        282, tr("ui.portraitpanel.close_eyes"), rect(left + 255, by, 135, 31),
        [this] {
            portraitClosure = portraitClosure > .5 ? 0 : 1;
            rebuildPortrait();
        },
        !portraitBlink);
    graphics.text(tr("ui.portraitpanel.drag_or_arrow_keys_move_the_selected_element_group_shift"),
                  rect(left + 18, by + 43, cw - 36, 44), 0x969BA6);
}
void Application::portraitBottom() {
    float y = height - bottom + 55;
    if (selected < 0 || !npcs[selected].faceRig)
        return;
    auto &rig = *npcs[selected].faceRig;
    graphics.image(portraitComposite, rect(18, y, 112, 112));
    graphics.text((rig.profile ? tr("ui.portraitpanel.portrait_profile") : tr("ui.portraitpanel.face_rig")) +
                      rig.id + tr("ui.portraitpanel.layers") + std::to_wstring(rig.parts.size()) +
                      tr("ui.portraitpanel.related_poses_animations") + std::to_wstring(rig.related.size()) +
                      L"\n" + (rig.profile ? rig.profile->path : rig.path).wstring(),
                  rect(150, y, width - 390, 85), 0x969BA6);
    ui.button(285, tr("ui.portraitpanel.export_png"), rect(width - 205, y, 180, 32),
              [this] { exportComposedPortrait(); });
    ui.button(286, tr("ui.panels.copy_source"), rect(width - 205, y + 43, 180, 32), [this] {
        applyPortraitPart();
        auto &r = *npcs[selected].faceRig;
        clipboard(wide(r.profile ? updatedPortraitSource(*r.profile) : updatedFaceSource(r)));
    });
}

bool Application::portraitDown(float x, float y) {
    if (leftTab != 1 || selected < 0 || !npcs[selected].faceRig || !contains(portraitCanvas, x, y))
        return false;
    auto &rig = *npcs[selected].faceRig;
    auto quads = faceQuads(rig, portraitClosure);
    float size = portraitCanvas.right - portraitCanvas.left;
    // Retain an explicitly selected group; individual mode picks the visible topmost layer under the cursor.
    if (portraitGroup == 0) {
        int hit = -1, order = -100000;
        for (int i = 0; i < int(quads.size()); ++i) {
            auto &q = quads[i];
            if (q.visible && q.order >= order &&
                contains(rect(portraitCanvas.left + float(q.x) * size, portraitCanvas.top + float(q.y) * size,
                              float(q.w) * size, float(q.h) * size),
                         x, y)) {
                hit = i;
                order = q.order;
            }
        }
        if (hit >= 0)
            selectPortraitPart(hit);
    }
    if (portraitPart < 0)
        selectPortraitPart(0);
    applyPortraitPart();
    ui.clearFocus();
    portraitDragging = true;
    portraitDragRecorded = false;
    lastX = x;
    lastY = y;
    SetCapture(window);
    return true;
}
void Application::portraitMove(float x, float y) {
    if (!portraitDragging || selected < 0 || !npcs[selected].faceRig || portraitPart < 0)
        return;
    float size = portraitCanvas.right - portraitCanvas.left;
    double pixels = npcs[selected].faceRig->profile ? npcs[selected].faceRig->profile->facePixels : 8;
    double fine = (GetKeyState(VK_SHIFT) & 0x8000) ? .1 : 1;
    double dx = (x - lastX) / size * pixels / 16 * fine, dy = (y - lastY) / size * pixels / 16 * fine;
    if (dx == 0 && dy == 0)
        return;
    movePortrait(dx, dy, !portraitDragRecorded);
    portraitDragRecorded = true;
}
} // namespace echo
