#include "app/Application.h"
#include "npc/Templates.h"
namespace echo {
void Application::applyBackgroundTemplate(bool forFace) {
    if (forFace && editCurrentFace)
        return;
    wchar_t executable[32768]{};
    GetModuleFileNameW(nullptr, executable, DWORD(std::size(executable)));
    auto settings =
        loadBackgroundTemplate(fs::path(executable).parent_path() / L"templates" / L"background.json");
    auto folder = forFace ? fs::path(draftPack.value) : fs::path(newFolder.value);
    auto pack = forFace ? folder : findContentPack(folder);
    auto id = nextTemplateId(settings, npcs, pack, forFace ? fs::path{} : folder);
    if (forFace) {
        draftId.set(id);
        draftName.set(settings.name);
        draftNpcColor = settings.color;
        rebuildDraft();
    } else {
        newId.set(id);
        newName.set(settings.name);
        newNpcColor = settings.color;
    }
    ui.clearFocus();
}
void Application::openFaceWizard() {
    if (selected < 0 || scanning || imageLoading)
        return;
    exportAfterWizard = false;
    editCurrentFace = false;
    faceDraft = detectFaceDraft(npcs[selected].skin);
    draftId.set(npcs[selected].id + L"_npc");
    draftName.set(npcs[selected].name);
    draftNpcColor = npcs[selected].color;
    auto pack = findContentPack(npcs[selected].skin->path);
    if (pack.empty())
        pack = findContentPack(project);
    draftPack.set(pack.wstring());
    draftCrop = 0;
    draftSection = 0;
    draftLid = 0;
    draftClosure = 0;
    refreshLidFields();
    draftUndoStates.clear();
    draftRedoStates.clear();
    draftPower.set(L"1");
    draftNewRect = false;
    draftPickingSkin = false;
    createDialog = eyeDialog = modelDropdown = false;
    contextIndex = -1;
    faceWizard = true;
    refreshDraftFields();
    refreshLidFields();
    rebuildDraft();
    ui.clearFocus();
}
void Application::openCurrentFaceSettings() {
    if (selected < 0 || scanning || imageLoading)
        return;
    sync();
    if (npcs[selected].faceRig) {
        leftTab = 1;
        modelDropdown = false;
        rebuildPortrait();
        selectPortraitPart(0);
        ui.clearFocus();
        status = tr("ui.facewizard.current_npc_settings_select_an_eye_pupil_brow_or_eyelid");
        return;
    }
    openFaceWizard();
    editCurrentFace = true;
    draftId.set(npcs[selected].id);
}
void Application::saveEchoScript() {
    if (selected < 0 || scanning || imageLoading)
        return;
    sync();
    if (duplicateId(npcs[selected].id, selected))
        throw std::runtime_error(tr8("error.facewizard.choose_a_new_unique_npc_identifier"));
    if (!npcs[selected].faceRig) {
        openCurrentFaceSettings();
        exportAfterWizard = true;
        return;
    }
    applyPortraitPart();
    auto &n = npcs[selected];
    auto oldRig = n.faceRig.get();
    auto pack = n.faceRig->packRoot;
    saveGameBundle(n, pack);
    portraitHistories.erase(oldRig);
    portraitSnapshot.clear();
    identifier.set(n.id);
    display.set(n.name);
    color.set(n.color);
    refreshPortraitFields();
    rebuildPortrait();
    graphics.setSkin(skinForPreview(n));
    status = tr("ui.export.saved");
}
void Application::refreshDraftFields() {
    auto r = faceDraft.crops[draftCrop];
    draftX.set(std::to_wstring(r.x));
    draftY.set(std::to_wstring(r.y));
    draftW.set(std::to_wstring(r.w));
    draftH.set(std::to_wstring(r.h));
}
void Application::applyDraftFields(bool record) {
    auto integer = [](const std::wstring &text) {
        size_t end;
        int n = std::stoi(text, &end);
        if (end != text.size() || n < 0 || n > 8)
            throw std::runtime_error(tr8("error.facewizard.crop_coordinates_must_be_integer_pixels_0_8"));
        return n;
    };
    FaceCrop r{integer(draftX.value), integer(draftY.value), integer(draftW.value), integer(draftH.value)};
    if (r.w < 1 || r.h < 1 || r.x + r.w > 8 || r.y + r.h > 8)
        throw std::runtime_error(tr8("error.facewizard.crop_outside_face"));
    auto next = faceDraft;
    auto old = next.crops[draftCrop];
    next.crops[draftCrop] = r;
    if (draftCrop < 2) {
        next.aperture = std::min(next.aperture, r.h);
        next.crops[1 - draftCrop].y = r.y;
        next.crops[1 - draftCrop].h = r.h;
        for (int side = 0; side < 2; ++side) {
            auto eye = next.crops[side];
            auto &p = next.crops[2 + side];
            p.w = std::min(p.w, eye.w);
            p.h = std::min(p.h, eye.h);
            p.x = std::clamp(p.x + (side == draftCrop ? r.x - old.x : 0), eye.x, eye.x + eye.w - p.w);
            p.y = std::clamp(p.y + r.y - old.y, eye.y, eye.y + eye.h - p.h);
        }
        if (next.brows && old.y != r.y)
            for (int i = 4; i < 6; ++i)
                next.crops[i].y = std::clamp(next.crops[i].y + r.y - old.y, 0, 8 - next.crops[i].h);
    }
    validateFaceDraft(next);
    if (record && next.crops != faceDraft.crops) {
        draftUndoStates.push_back(faceDraft);
        draftRedoStates.clear();
    }
    faceDraft = std::move(next);
    refreshDraftFields();
    rebuildDraft();
}
void Application::refreshLidFields() {
    auto lid = faceDraft.lids[draftLid];
    lidOffset.set(std::to_wstring(lid.offset));
    lidHeight.set(std::to_wstring(lid.height));
    lidTravel.set(std::to_wstring(lid.travel));
    lidClosure.set(std::to_wstring(draftClosure));
}
void Application::applyLidFields() {
    auto number = [](std::wstring text) {
        std::replace(text.begin(), text.end(), L',', L'.');
        size_t end;
        double n = std::stod(text, &end);
        if (end != text.size() || !std::isfinite(n))
            throw std::runtime_error(tr8("error.facewizard.invalid_eyelid_number"));
        return n;
    };
    auto next = faceDraft;
    double h = number(lidHeight.value), closure = number(lidClosure.value);
    if (h != std::floor(h) || h < 1 || h > 4 || closure < 0 || closure > 1)
        throw std::runtime_error(tr8("error.facewizard.eyelid_height_1_4_closure_0_1"));
    next.lids[draftLid] = {number(lidOffset.value), number(lidTravel.value), int(h)};
    validateFaceDraft(next);
    if (next.lids != faceDraft.lids) {
        draftUndoStates.push_back(faceDraft);
        draftRedoStates.clear();
    }
    faceDraft = std::move(next);
    draftClosure = closure;
    refreshLidFields();
    rebuildDraft();
}
void Application::rebuildDraft() {
    graphics.invalidateImages();
    try {
        auto previewColor = validColor(draftNpcColor) ? draftNpcColor : L"#DEC6B2";
        facePackage = prepareFacePackage(faceDraft, L"jdh:preview", tr("ui.facewizard.preview"), previewColor,
                                         draftClosure);
        for (int i = 0; i < 2; ++i) {
            const auto &atlas = facePackage.textures.at(i ? L"lower_eyelids" : L"upper_eyelids");
            for (int side = 0; side < 2; ++side) {
                const auto &eye = faceDraft.crops[side];
                auto &mini = eyelidMini[i * 2 + side];
                mini = {unsigned(eye.w), unsigned(faceDraft.lids[i].height), {}};
                mini.rgba.resize(mini.width * mini.height * 4);
                for (unsigned yy = 0; yy < mini.height; ++yy)
                    std::copy_n(&atlas.rgba[((yy + (i ? 12 : 13)) * 64 + 8 + eye.x) * 4], mini.width * 4,
                                &mini.rgba[yy * mini.width * 4]);
            }
        }
        draftError.clear();
    } catch (const std::exception &e) {
        facePackage = {};
        draftError = wide(e.what());
    }
}
void Application::commitFaceWizard() {
    if (draftSection == 1)
        applyLidFields();
    applyDraftFields();
    if (duplicateId(draftId.value, editCurrentFace ? selected : -1))
        throw std::runtime_error(tr8("error.facewizard.choose_a_new_unique_npc_identifier"));
    auto n =
        writeFacePackage(faceDraft, fs::path(draftPack.value), draftId.value, draftName.value, draftNpcColor,
                         npcs[selected].slim, editCurrentFace ? &npcs[selected] : nullptr);
    auto source = faceDraft.source->path;
    faceWizard = false;
    draftDragging = false;
    ui.clearFocus();
    if (editCurrentFace) {
        auto index = selected;
        portraitHistories.erase(npcs[index].faceRig.get());
        portraitPart = -1;
        portraitSnapshot.clear();
        npcs[index] = std::move(n);
        identifier.set(npcs[index].id);
        display.set(npcs[index].name);
        color.set(npcs[index].color);
        select(index);
    } else {
        npcs.push_back(std::move(n));
        select(int(npcs.size()) - 1);
    }
    leftTab = 1;
    if (exportAfterWizard) {
        exportAfterWizard = false;
        saveEchoScript();
        return;
    }
    status = editCurrentFace ? tr("ui.facewizard.face_settings_saved_for_the_current_npc")
                             : tr("ui.facewizard.game_npc_created_source") + source.wstring();
}
void Application::drawFaceWizard() {
    ui.widgets.clear();
    float x = (width - 980) / 2, y = (height - 640) / 2;
    graphics.fill(rect(0, 58, width, height - 58), 0x101216);
    graphics.fill(rect(x - 3, y - 3, 986, 646), 0x343A46, 8);
    graphics.fill(rect(x, y, 980, 640), 0x191C22, 6);
    graphics.text(editCurrentFace ? tr("ui.facewizard.current_npc_face_settings")
                                  : tr("ui.facewizard.create_game_npc_extract_face"),
                  rect(x + 20, y + 16, editCurrentFace ? 925.f : 640.f, 30), 0xE6E8EC, 1);
    if (!editCurrentFace)
        ui.button(530, tr("ui.template.background"), rect(x + 695, y + 15, 265, 31),
                  [this] { applyBackgroundTemplate(true); });
    graphics.text(editCurrentFace ? tr("ui.facewizard.current_npc_id") : tr("ui.facewizard.new_npc_id"),
                  rect(x + 20, y + 55, 450, 20), 0x969BA6);
    graphics.text(tr("ui.facewizard.npc_name"), rect(x + 505, y + 55, 450, 20), 0x969BA6);
    ui.input(410, draftId, rect(x + 20, y + 78, 450, 31), !editCurrentFace);
    ui.input(411, draftName, rect(x + 505, y + 78, 450, 31), !editCurrentFace);
    graphics.text(tr("ui.facewizard.content_pack_root_under_story_content_pack"),
                  rect(x + 20, y + 114, 820, 20), 0x969BA6);
    ui.input(412, draftPack, rect(x + 20, y + 136, 825, 31));
    ui.button(413, tr("ui.facewizard.browse"), rect(x + 855, y + 136, 105, 31), [this] {
        auto p = folderDialog();
        if (!p.empty())
            draftPack.set(p.wstring());
    });
    graphics.text(tr("ui.facewizard.source_face_8_8"), rect(x + 20, y + 179, 320, 22), 0x969BA6);
    graphics.text(tr("ui.facewizard.composed_face"), rect(x + 750, y + 179, 210, 22), 0x969BA6);
    draftCanvas = rect(x + 20, y + 206, 320, 320);
    graphics.image(faceDraft.face, draftCanvas);
    for (int i = 0; i <= 8; ++i) {
        graphics.line(x + 20 + i * 40, y + 206, x + 20 + i * 40, y + 526, 0x586174, .6f);
        graphics.line(x + 20, y + 206 + i * 40, x + 340, y + 206 + i * 40, 0x586174, .6f);
    }
    if (draftSection == 0)
        for (int pass = 0; pass < 2; ++pass)
            for (int i = 0; i < 6; ++i) {
                if ((i == draftCrop) != (pass == 1) || (i >= 4 && !faceDraft.brows) ||
                    (!draftAllBounds && i != draftCrop))
                    continue;
                auto r = faceDraft.crops[i];
                auto bounds = rect(x + 20 + r.x * 40, y + 206 + r.y * 40, r.w * 40.f, r.h * 40.f);
                graphics.contrastOutline(faceDraft.face, draftCanvas, bounds, i == draftCrop ? 3.f : 1.f);
                if (i == draftCrop) {
                    for (float hx : {bounds.left, (bounds.left + bounds.right) / 2, bounds.right})
                        for (float hy : {bounds.top, (bounds.top + bounds.bottom) / 2, bounds.bottom}) {
                            if (hx == (bounds.left + bounds.right) / 2 &&
                                hy == (bounds.top + bounds.bottom) / 2)
                                continue;
                            graphics.fill(rect(hx - 5, hy - 5, 10, 10), 0x101010);
                            graphics.fill(rect(hx - 3, hy - 3, 6, 6), 0xFFFFFF);
                        }
                }
            }
    if (draftSection == 0) {
        ui.toggle(442, tr("ui.facewizard.new_region"), rect(x + 20, y + 535, 155, 27), draftNewRect,
                  [this] { draftNewRect = !draftNewRect; });
        ui.toggle(443, tr("ui.facewizard.all_bounds"), rect(x + 185, y + 535, 155, 27), draftAllBounds,
                  [this] { draftAllBounds = !draftAllBounds; });
        graphics.text(tr("ui.facewizard.drag_inside_to_move_shift_slows_down"),
                      rect(x + 20, y + 565, 325, 20), 0x969BA6);
    } else {
        auto eye = faceDraft.crops[0];
        double center = eye.y + eye.h / 2.;
        auto lid = faceDraft.lids[draftLid];
        double top = center + (draftLid ? faceDraft.aperture / 2. : -faceDraft.aperture / 2. - lid.height) +
                     lid.offset;
        for (int side = 0; side < 2; ++side) {
            const auto &crop = faceDraft.crops[side];
            auto bounds = rect(draftCanvas.left + crop.x * 40, draftCanvas.top + float(top) * 40,
                               crop.w * 40.f, lid.height * 40.f);
            graphics.contrastOutline(faceDraft.face, draftCanvas, bounds, 3);
            float handleX = (bounds.left + bounds.right) / 2;
            for (float hy : {bounds.top, bounds.bottom}) {
                graphics.fill(rect(handleX - 5, hy - 5, 10, 10), 0x101010);
                graphics.fill(rect(handleX - 3, hy - 3, 6, 6), 0xFFFFFF);
            }
        }
        ui.toggle(436, tr("ui.facewizard.eyelid_color_from_skin"), rect(x + 20, y + 535, 320, 28),
                  draftPickingSkin, [this] { draftPickingSkin = !draftPickingSkin; });
        graphics.text(tr("ui.facewizard.eyelid_width_matches_eye_width_drag_inside_to_move_the"),
                      rect(x + 20, y + 565, 325, 38), 0x969BA6);
    }
    float cx = x + 365, cy = y + 206;
    ui.button(
        488, tr("ui.facewizard.eyes_and_brows"), rect(cx, y + 179, 170, 23),
        [this] {
            if (draftSection == 1)
                applyLidFields();
            draftSection = 0;
            refreshDraftFields();
        },
        true, draftSection == 0);
    ui.button(
        489, tr("ui.facewizard.eyelids"), rect(cx + 180, y + 179, 170, 23),
        [this] {
            applyDraftFields();
            draftSection = 1;
            refreshLidFields();
        },
        true, draftSection == 1);
    if (draftSection == 0) {
        ui.button(414, tr("ui.facewizard.auto_detect"), rect(cx, cy, 350, 29), [this] {
            draftUndoStates.push_back(faceDraft);
            draftRedoStates.clear();
            faceDraft = detectFaceDraft(faceDraft.source);
            refreshDraftFields();
            rebuildDraft();
        });
        cy += 36;
        ui.button(415, tr("ui.facewizard.sasha_aperture_2_px"), rect(cx, cy, 170, 29), [this] {
            draftUndoStates.push_back(faceDraft);
            draftRedoStates.clear();
            setEyeHeight(faceDraft, 2);
            refreshDraftFields();
            rebuildDraft();
        });
        ui.button(416, tr("ui.facewizard.father_aperture_1_px"), rect(cx + 180, cy, 170, 29), [this] {
            draftUndoStates.push_back(faceDraft);
            draftRedoStates.clear();
            setEyeHeight(faceDraft, 1);
            refreshDraftFields();
            rebuildDraft();
        });
        cy += 37;
        const std::wstring labels[] = {tr("ui.facewizard.left_eye"),   tr("ui.facewizard.right_eye"),
                                       tr("ui.facewizard.left_pupil"), tr("ui.facewizard.right_pupil"),
                                       tr("ui.facewizard.left_brow"),  tr("ui.facewizard.right_brow")};
        for (int i = 0; i < 6; ++i) {
            ui.button(
                420 + i, labels[i], rect(cx + (i % 2) * 180, cy, 170, 28),
                [this, i] {
                    applyDraftFields();
                    draftCrop = i;
                    draftPickingSkin = false;
                    refreshDraftFields();
                    ui.clearFocus();
                },
                i < 4 || faceDraft.brows, draftCrop == i);
            if (i % 2)
                cy += 34;
        }
        cy += 5;
        graphics.text(tr("ui.facewizard.x_y_width_height"), rect(cx, cy, 350, 20), 0x969BA6);
        cy += 23;
        TextEdit *fields[] = {&draftX, &draftY, &draftW, &draftH};
        for (int i = 0; i < 4; ++i)
            ui.input(430 + i, *fields[i], rect(cx + i * 90, cy, 80, 31));
        cy += 39;
        ui.button(434, tr("ui.facewizard.apply_region"), rect(cx, cy, 350, 29), [this] {
            try {
                applyDraftFields();
                ui.clearFocus();
            } catch (const std::exception &e) {
                draftError = wide(e.what());
            }
        });
        cy += 37;
        ui.toggle(435, tr("ui.facewizard.extract_brows"), rect(cx, cy, 170, 28), faceDraft.brows, [this] {
            draftUndoStates.push_back(faceDraft);
            draftRedoStates.clear();
            faceDraft.brows = !faceDraft.brows;
            if (!faceDraft.brows && draftCrop >= 4)
                draftCrop = 0;
            refreshDraftFields();
            rebuildDraft();
        });
        ui.toggle(436, tr("ui.facewizard.skin_sample"), rect(cx + 180, cy, 170, 28), draftPickingSkin,
                  [this] {
                      draftPickingSkin = !draftPickingSkin;
                      ui.clearFocus();
                  });
        cy += 35;
        graphics.text(
            trf("face.sample_status", {{L"x", std::to_wstring(faceDraft.fillX)},
                                       {L"y", std::to_wstring(faceDraft.fillY)},
                                       {L"confidence", std::to_wstring(int(faceDraft.confidence * 100))}}),
            rect(cx, cy, 350, 20), 0x969BA6);
        graphics.text(tr("ui.facewizard.sensitivity"), rect(cx, cy + 29, 50, 24), 0x969BA6);
        ui.input(444, draftPower, rect(cx + 50, cy + 26, 64, 28));
        ui.button(445, tr("ui.facewizard.sensitivity_quarter"), rect(cx + 121, cy + 26, 65, 28),
                  [this] { draftPower.set(L"0.25"); });
        ui.button(446, tr("ui.facewizard.sensitivity_normal"), rect(cx + 191, cy + 26, 49, 28),
                  [this] { draftPower.set(L"1"); });
        ui.button(447, tr("ui.facewizard.sensitivity_double"), rect(cx + 245, cy + 26, 49, 28),
                  [this] { draftPower.set(L"2"); });
        ui.button(
            448, tr("ui.control.undo"), rect(cx + 300, cy + 26, 50, 28), [this] { draftUndo(); },
            !draftUndoStates.empty());
    } else {
        ui.button(
            490, tr("ui.facewizard.upper"), rect(cx, cy, 170, 29),
            [this] {
                applyLidFields();
                draftLid = 0;
                refreshLidFields();
            },
            true, draftLid == 0);
        ui.button(
            491, tr("ui.facewizard.lower"), rect(cx + 180, cy, 170, 29),
            [this] {
                applyLidFields();
                draftLid = 1;
                refreshLidFields();
            },
            true, draftLid == 1);
        cy += 40;
        TextEdit *fields[] = {&lidOffset, &lidHeight, &lidTravel, &lidClosure};
        const std::wstring labels[] = {
            tr("ui.facewizard.y_offset_pixels"), tr("ui.facewizard.thickness_pixels"),
            tr("ui.facewizard.blink_travel_0_auto"), tr("ui.facewizard.preview_closure_0_1")};
        for (int i = 0; i < 4; ++i) {
            graphics.text(labels[i], rect(cx, cy, 350, 20), 0x969BA6);
            ui.input(496 + i, *fields[i], rect(cx, cy + 23, 350, 31));
            cy += 64;
        }
        ui.button(500, tr("ui.facewizard.apply_eyelids"), rect(cx, cy, 350, 29), [this] {
            try {
                applyLidFields();
                ui.clearFocus();
            } catch (const std::exception &e) {
                draftError = wide(e.what());
            }
        });
        cy += 36;
        ui.button(501, tr("ui.facewizard.open"), rect(cx, cy, 110, 28), [this] {
            applyLidFields();
            draftClosure = 0;
            refreshLidFields();
            rebuildDraft();
        });
        ui.button(502, tr("ui.facewizard.closed"), rect(cx + 120, cy, 110, 28), [this] {
            applyLidFields();
            draftClosure = 1;
            refreshLidFields();
            rebuildDraft();
        });
        ui.button(
            503, tr("ui.control.undo"), rect(cx + 240, cy, 110, 28), [this] { draftUndo(); },
            !draftUndoStates.empty());
    }
    if (facePackage.portrait.width) {
        graphics.image(facePackage.portrait, rect(x + 750, y + 206, 210, 210));
        float ty = y + 430;
        if (draftSection == 1) {
            for (int i = 0; i < 2; ++i) {
                graphics.text(i ? tr("ui.facewizard.lower_eyelids") : tr("ui.facewizard.upper_eyelids"),
                              rect(x + 750, ty, 210, 20), 0x969BA6);
                for (int side = 0; side < 2; ++side) {
                    const auto &mini = eyelidMini[i * 2 + side];
                    float mx = x + 750 + side * 105.f;
                    graphics.image(mini, rect(mx, ty + 22, mini.width * 12.f, mini.height * 12.f));
                    graphics.text(std::to_wstring(mini.width) + L"×" + std::to_wstring(mini.height),
                                  rect(mx + 52, ty + 22, 50, 20), 0x969BA6);
                }
                ty += 32 + faceDraft.lids[i].height * 12;
            }
        } else

            for (auto key : {L"sclera_left", L"pupil_left", L"brow_left"}) {
                auto it = facePackage.textures.find(key);
                if (it == facePackage.textures.end())
                    continue;
                graphics.image(it->second,
                               rect(x + 750, ty, it->second.width * 22.f, it->second.height * 22.f));
                const auto &label = tr(key == std::wstring(L"sclera_left")  ? "ui.facewizard.left_eye"
                                       : key == std::wstring(L"pupil_left") ? "ui.facewizard.left_pupil"
                                                                            : "ui.facewizard.left_brow");
                graphics.text(label + L" " + std::to_wstring(it->second.width) + L"×" +
                                  std::to_wstring(it->second.height),
                              rect(x + 810, ty, 150, 40), 0x969BA6);
                ty += 48;
            }
    }
    if (!draftError.empty())
        graphics.text(draftError, rect(x + 20, y + 589, 675, 42), 0xF49B7D);
    ui.button(
        440,
        exportAfterWizard
            ? tr("ui.export.save")
            : (editCurrentFace ? tr("ui.facewizard.save_face") : tr("ui.facewizard.create_npc")),
        rect(x + 695, y + 595, 160, 32), [this] { commitFaceWizard(); }, draftError.empty());
    ui.button(441, tr("ui.facewizard.cancel"), rect(x + 865, y + 595, 95, 32), [this] {
        faceWizard = false;
        draftDragging = false;
        ui.clearFocus();
        graphics.invalidateImages();
    });
}
int Application::draftHit(float x, float y) const {
    if (draftSection == 1) {
        auto eye = faceDraft.crops[0];
        auto lid = faceDraft.lids[draftLid];
        double top = eye.y + eye.h / 2. +
                     (draftLid ? faceDraft.aperture / 2. : -faceDraft.aperture / 2. - lid.height) +
                     lid.offset;
        float a = draftCanvas.top + float(top) * 40, b = a + lid.height * 40;
        bool overEye = false;
        for (int side = 0; side < 2; ++side) {
            const auto &crop = faceDraft.crops[side];
            float eyeLeft = draftCanvas.left + crop.x * 40, eyeRight = eyeLeft + crop.w * 40;
            overEye |= x >= eyeLeft && x <= eyeRight;
        }
        if (!overEye || y < a - 8 || y > b + 8)
            return -1;
        if (std::abs(y - a) <= 8)
            return 4;
        if (std::abs(y - b) <= 8)
            return 8;
        return 0;
    }
    auto r = faceDraft.crops[draftCrop];
    float l = draftCanvas.left + r.x * 40, t = draftCanvas.top + r.y * 40, edgeRight = l + r.w * 40,
          edgeBottom = t + r.h * 40;
    if (x < l - 8 || x > edgeRight + 8 || y < t - 8 || y > edgeBottom + 8)
        return -1;
    int edges = 0;
    if (std::abs(x - l) <= 8)
        edges |= 1;
    else if (std::abs(x - edgeRight) <= 8)
        edges |= 2;
    if (std::abs(y - t) <= 8)
        edges |= 4;
    else if (std::abs(y - edgeBottom) <= 8)
        edges |= 8;
    return edges;
}
bool Application::draftDown(float x, float y) {
    if (!faceWizard)
        return false;
    int hit = draftHit(x, y);
    if (!contains(draftCanvas, x, y) && (hit < 0 || draftNewRect || draftPickingSkin))
        return false;
    int px = std::clamp(int((x - draftCanvas.left) / 40), 0, 7),
        py = std::clamp(int((y - draftCanvas.top) / 40), 0, 7);
    if (draftPickingSkin) {
        draftUndoStates.push_back(faceDraft);
        draftRedoStates.clear();
        faceDraft.fillX = px;
        faceDraft.fillY = py;
        draftPickingSkin = false;
        rebuildDraft();
        return true;
    }
    try {
        size_t end;
        auto powerText = draftPower.value;
        std::replace(powerText.begin(), powerText.end(), L',', L'.');
        auto power = std::stod(powerText, &end);
        if (end != powerText.size() || !std::isfinite(power) || power < .1 || power > 4)
            throw std::runtime_error(tr8("error.facewizard.sensitivity_a_number_from_0_1_to_4"));
        draftDragPower = float(power) * ((GetKeyState(VK_SHIFT) & 0x8000) ? .25f : 1.f);
        if (draftSection == 1)
            applyLidFields();
        else
            applyDraftFields();
    } catch (const std::exception &e) {
        draftError = wide(e.what());
        return true;
    }
    ui.clearFocus();
    hit = draftHit(x, y);
    draftDragBase = faceDraft;
    draftPointerX = x;
    draftPointerY = y;
    draftStartX = px;
    draftStartY = py;
    if (draftSection == 1 && hit < 0)
        return true;
    draftDragEdges = draftSection == 1 ? hit : (draftNewRect || hit < 0 ? 16 : hit);
    draftDragging = true;
    SetCapture(window);
    if (draftDragEdges == 16)
        draftMove(x, y);
    return true;
}
void Application::draftMove(float x, float y) {
    if (!draftDragging)
        return;
    if (draftSection == 1) {
        auto next = draftDragBase;
        auto &lid = next.lids[draftLid];
        double delta = (y - draftPointerY) / 40 * draftDragPower;
        if (draftDragEdges == 4 || draftDragEdges == 8) {
            int old = lid.height;
            lid.height = std::clamp(old + int(std::lround(delta)) * (draftDragEdges == 4 ? -1 : 1), 1, 4);
            if (draftDragEdges == 4 && draftLid == 1)
                lid.offset += old - lid.height;
            if (draftDragEdges == 8 && draftLid == 0)
                lid.offset += lid.height - old;
        } else
            lid.offset += std::round(delta * 4) / 4;
        lid.offset = std::clamp(lid.offset, -4., 4.);
        faceDraft = std::move(next);
        refreshLidFields();
        rebuildDraft();
        return;
    }
    auto r = draftDragBase.crops[draftCrop];
    int dx = int(std::lround((x - draftPointerX) / 40 * draftDragPower)),
        dy = int(std::lround((y - draftPointerY) / 40 * draftDragPower));
    int minX = 0, maxX = 8, minY = 0, maxY = 8, maxH = 8;
    if (draftCrop < 2) {
        minX = draftCrop * 4;
        maxX = minX + 4;
        maxH = 2;
    } else if (draftCrop < 4) {
        auto e = draftDragBase.crops[draftCrop - 2];
        minX = e.x;
        maxX = e.x + e.w;
        minY = e.y;
        maxY = e.y + e.h;
    }
    if (draftDragEdges == 16) {
        int px = std::clamp(draftStartX + dx, minX, maxX - 1),
            py = std::clamp(draftStartY + dy, minY, maxY - 1);
        int sx = std::clamp(draftStartX, minX, maxX - 1), sy = std::clamp(draftStartY, minY, maxY - 1);
        r = {std::min(px, sx), std::min(py, sy), std::abs(px - sx) + 1,
             std::min(maxH, std::abs(py - sy) + 1)};
    } else if (draftDragEdges == 0) {
        r.x = std::clamp(r.x + dx, minX, maxX - r.w);
        r.y = std::clamp(r.y + dy, minY, maxY - r.h);
    } else {
        int l = r.x, rr = r.x + r.w, t = r.y, b = r.y + r.h;
        if (draftDragEdges & 1)
            l = std::clamp(l + dx, minX, rr - 1);
        if (draftDragEdges & 2)
            rr = std::clamp(rr + dx, l + 1, maxX);
        if (draftDragEdges & 4)
            t = std::clamp(t + dy, std::max(minY, b - maxH), b - 1);
        if (draftDragEdges & 8)
            b = std::clamp(b + dy, t + 1, std::min(maxY, t + maxH));
        r = {l, t, rr - l, b - t};
    }
    auto previous = faceDraft;
    faceDraft = draftDragBase;
    draftX.set(std::to_wstring(r.x));
    draftY.set(std::to_wstring(r.y));
    draftW.set(std::to_wstring(r.w));
    draftH.set(std::to_wstring(r.h));
    try {
        applyDraftFields(false);
    } catch (const std::exception &e) {
        faceDraft = std::move(previous);
        refreshDraftFields();
        draftError = wide(e.what());
    }
}
void Application::finishDraftDrag() {
    if (!draftDragging)
        return;
    draftDragging = false;
    if (faceDraft.crops != draftDragBase.crops || faceDraft.lids != draftDragBase.lids) {
        draftUndoStates.push_back(draftDragBase);
        draftRedoStates.clear();
    }
    refreshDraftFields();
    rebuildDraft();
}
void Application::draftUndo(bool redo) {
    auto &from = redo ? draftRedoStates : draftUndoStates;
    auto &to = redo ? draftUndoStates : draftRedoStates;
    if (from.empty())
        return;
    to.push_back(faceDraft);
    faceDraft = from.back();
    from.pop_back();
    refreshDraftFields();
    refreshLidFields();
    rebuildDraft();
    ui.clearFocus();
}
} // namespace echo
