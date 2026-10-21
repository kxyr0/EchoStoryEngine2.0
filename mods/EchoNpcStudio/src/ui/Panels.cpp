#include "app/Application.h"
#include <commdlg.h>
#include <fstream>
#include <shlobj.h>
namespace echo {
static std::wstring lower(std::wstring s) {
    std::transform(s.begin(), s.end(), s.begin(), towlower);
    return s;
}
void Application::toolbar() {
    graphics.fill(rect(0, 0, width, 58), 0x191C22);
    ui.button(
        1, tr("ui.panels.open_folder"), rect(18, 12, 112, 34), [this] { openProject(folderDialog()); },
        !scanning);
    ui.button(
        2, tr("ui.panels.import_png"), rect(138, 12, 105, 34), [this] { importSkin(false); }, !scanning);
    ui.button(
        3, tr("ui.common.save"), rect(251, 12, 90, 34), [this] { save(); },
        selected >= 0 && !scanning && (leftTab == 0 || npcs[selected].faceRig));
    ui.button(
        4, tr("ui.panels.save_as"), rect(349, 12, 125, 34), [this] { save(true); },
        selected >= 0 && !scanning && (leftTab == 0 || npcs[selected].faceRig));
    ui.button(
        5, tr("ui.common.reload"), rect(482, 12, 90, 34),
        [this] {
            if (!project.empty())
                openProject(project);
            else if (selected >= 0 && discard()) {
                auto n = npcFromSkin(loadSkin(npcs[selected].skin->path));
                readMetadata(n);
                auto index = selected;
                npcs[index] = std::move(n);
                selected = -1;
                select(index);
            }
        },
        !scanning && (!project.empty() || selected >= 0));
    ui.button(
        400, tr("ui.panels.create_npc_face"), rect(580, 12, 180, 34), [this] { openFaceWizard(); },
        selected >= 0 && !scanning && !imageLoading);
    ui.toggle(480, tr("ui.panels.properties"), rect(width - 390, 12, 120, 34), showProperties, [this] {
        if (showProperties)
            savedLeft = left;
        else
            left = savedLeft;
        showProperties = !showProperties;
        ui.clearFocus();
        layout();
    });
    ui.toggle(481, tr("ui.panels.library"), rect(width - 262, 12, 120, 34), showLibrary, [this] {
        if (showLibrary)
            savedRight = right;
        else
            right = savedRight;
        showLibrary = !showLibrary;
        ui.clearFocus();
        layout();
    });
    ui.toggle(482, tr("ui.panels.tabs"), rect(width - 134, 12, 120, 34), showBottom, [this] {
        if (showBottom)
            savedBottom = bottom;
        else
            bottom = savedBottom;
        showBottom = !showBottom;
        ui.clearFocus();
        layout();
    });
}
void Application::properties() {
    float end = height - bottom;
    graphics.fill(rect(0, 58, left, end - 58), 0x191C22);
    ui.button(
        300, tr("ui.panels.properties"), rect(12, 72, (left - 30) / 2, 31),
        [this] {
            leftTab = 0;
            if (selected >= 0)
                graphics.setSkin(skinForPreview(npcs[selected]));
            modelDropdown = false;
        },
        true, leftTab == 0);
    ui.button(
        301, tr("ui.panels.portrait"), rect(18 + (left - 30) / 2, 72, (left - 30) / 2, 31),
        [this] {
            leftTab = 1;
            modelDropdown = false;
            rebuildPortrait();
        },
        true, leftTab == 1);
    if (leftTab == 1) {
        portraitPanel();
        return;
    }
    graphics.clip(rect(0, 110, left, end - 110));
    auto firstWidget = ui.widgets.size();
    if (selected < 0) {
        graphics.text(tr("ui.panels.select_an_npc_in_the_library"), rect(18, 126, left - 36, 65), 0x969BA6);
        graphics.unclip();
        return;
    }
    auto &n = npcs[selected];
    float x = 18, w = left - 36, y = 116 - propertiesScroll;
    auto field = [&](const std::wstring &title, TextEdit &edit, int id) {
        graphics.text(title, rect(x, y, w, 21), 0x969BA6);
        ui.input(id, edit, rect(x, y + 24, w, 34));
        y += 70;
    };
    field(tr("ui.panels.identifier"), identifier, 10);
    if (!validIdentifier(n.id) || duplicateId(n.id, selected)) {
        graphics.text(!validIdentifier(n.id) ? tr("ui.panels.invalid_namespace_path")
                                             : tr("ui.panels.duplicate_identifier"),
                      rect(x, y - 11, w, 18), 0xF49B7D);
    }
    field(n.gameDefinition.empty() ? tr("ui.panels.display_name") : tr("ui.panels.npc_name_from_game_script"),
          display, 11);
    graphics.text(tr("ui.panels.skin_resource"), rect(x, y, w, 21), 0x969BA6);
    graphics.text(n.skin->path.filename().wstring(), rect(x, y + 24, w, 25));
    y += 53;
    ui.button(12, tr("ui.panels.choose_png"), rect(x, y, w / 2 - 4, 32), [this] { importSkin(true); });
    ui.button(13, tr("ui.panels.reveal"), rect(x + w / 2 + 4, y, w / 2 - 4, 32),
              [this] { reveal(npcs[selected].skin->path); });
    y += 46;
    graphics.text(tr("ui.panels.name_color"), rect(x, y, w, 21), 0x969BA6);
    ui.input(14, color, rect(x, y + 24, w - 45, 34));
    uint32_t c = validColor(n.color) ? std::stoul(n.color.substr(1), nullptr, 16) : 0xFF4466;
    graphics.fill(rect(x + w - 35, y + 24, 35, 34), c, 4);
    ui.button(15, tr("ui.panels.pick_color"), rect(x, y + 66, w / 2 - 4, 30), [this] {
        static COLORREF custom[16]{};
        auto &current = npcs[selected];
        COLORREF initial = RGB(222, 198, 178);
        if (validColor(current.color)) {
            auto v = std::stoul(current.color.substr(1), nullptr, 16);
            initial = RGB(v >> 16 & 255, v >> 8 & 255, v & 255);
        }
        CHOOSECOLORW picker{};
        picker.lStructSize = sizeof(picker);
        picker.hwndOwner = window;
        picker.rgbResult = initial;
        picker.lpCustColors = custom;
        picker.Flags = CC_FULLOPEN | CC_RGBINIT;
        if (ChooseColorW(&picker)) {
            wchar_t hex[8];
            swprintf_s(hex, L"#%02X%02X%02X", GetRValue(picker.rgbResult), GetGValue(picker.rgbResult),
                       GetBValue(picker.rgbResult));
            color.set(hex);
            sync();
        }
    });
    ui.button(512, tr("ui.panels.from_clothing"), rect(x + w / 2 + 4, y + 66, w / 2 - 4, 30),
              [this] { openNameColorPicker(); });
    y += 110;
    ui.button(16, n.slim ? tr("ui.panels.model_slim") : tr("ui.panels.model_classic"), rect(x, y, w, 32),
              [this] { modelDropdown = !modelDropdown; });
    y += 43;
    auto flag = [&](int id, const std::wstring &title, bool Npc::*member) {
        ui.toggle(id, title, rect(x, y, w, 29), n.*member, [this, member] {
            npcs[selected].*member = !(npcs[selected].*member);
            npcs[selected].dirty = true;
        });
        y += 34;
    };
    ui.button(510, tr("ui.panels.face_settings"), rect(x, y, w, 31), [this] { openCurrentFaceSettings(); });
    y += 39;
    ui.button(547, tr("ui.export.save"), rect(x, y, w, 31), [this] { saveEchoScript(); });
    y += 39;
    ui.button(511,
              showSkinLayers ? tr("ui.panels.skin_layers_expanded") : tr("ui.panels.skin_layers_collapsed"),
              rect(x, y, w, 29), [this] { showSkinLayers = !showSkinLayers; });
    y += 34;
    if (showSkinLayers) {
        flag(17, tr("ui.panels.hat_layer"), &Npc::hat);
        flag(18, tr("ui.skin.jacket"), &Npc::jacket);
        flag(19, tr("ui.skin.sleeves"), &Npc::sleeves);
        flag(20, tr("ui.panels.pants_overlay"), &Npc::pants);
    }
    graphics.unclip();
    for (size_t i = firstWidget; i < ui.widgets.size(); ++i) {
        auto &widget = ui.widgets[i];
        if (widget.bounds.top < 110 || widget.bounds.bottom > end)
            widget.enabled = false;
    }
    if (y > end)
        graphics.text(tr("ui.panels.scroll_for_more_properties"), rect(18, end - 20, left - 36, 18),
                      0x969BA6);
}
void Application::library() {
    float x = width - right, end = height - bottom;
    graphics.fill(rect(x, 58, right, end - 58), 0x191C22);
    ui.input(30, search, rect(x + 18, 76, right - 36, 34));
    ui.button(
        31, tr("ui.application.create_npc"), rect(x + 18, 122, (right - 44) * .5f, 33),
        [this] {
            if (selected >= 0) {
                openFaceWizard();
                return;
            }
            newId.set(L"jdh:new_npc");
            newName.set(tr("ui.panels.new_npc"));
            newSkin.set(selected >= 0 ? npcs[selected].skin->path.wstring() : L"");
            newFolder.set(project.empty()
                              ? (selected >= 0 ? npcs[selected].skin->path.parent_path().wstring() : L"")
                              : project.wstring());
            newSlim = false;
            newNpcColor = L"#DEC6B2";
            createDialog = true;
            ui.clearFocus();
        },
        !scanning);
    ui.button(
        570, tr("ui.removal.delete"), rect(x + 22 + (right - 44) * .5f, 122, (right - 44) * .5f, 33),
        [this] { removeNpc(selected); }, selected >= 0 && !scanning);
    ui.button(
        571, tr("ui.removal.restore"), rect(x + 18, 164, right - 36, 28), [this] { restoreRemovedNpc(); },
        lastRemoval.has_value() && !scanning);
    graphics.clip(rect(x, 200, right, end - 200));
    float y = 204 - libraryScroll;
    for (int category = 0; category < 2; ++category) {
        std::vector<size_t> indices;
        for (size_t i = 0; i < npcs.size(); ++i) {
            const auto &n = npcs[i];
            if (n.gameDefinition.empty() != (category == 1))
                continue;
            if (!search.value.empty() &&
                lower(n.id + L" " + n.name + L" " + n.skin->path.wstring()).find(lower(search.value)) ==
                    std::wstring::npos)
                continue;
            indices.push_back(i);
        }
        auto title = tr(category ? "ui.library.without_script" : "ui.library.with_script") + L" (" +
                     std::to_wstring(indices.size()) + L")";
        bool categoryCollapsed = collapsedCategories[category] && search.value.empty();
        if (y >= 200 && y + 28 <= end)
            ui.button(560 + category, (categoryCollapsed ? L"▸ " : L"▾ ") + title,
                      rect(x + 12, y, right - 24, 26), [this, category] {
                          collapsedCategories[category] = !collapsedCategories[category];
                          libraryScroll = 0;
                      });
        y += 30;
        if (categoryCollapsed)
            continue;
        std::wstring group;
        for (auto i : indices) {
            auto &n = npcs[i];
            auto folder = n.metadata.parent_path();
            std::error_code ec;
            auto relative = project.empty() ? folder.filename() : fs::relative(folder, project, ec);
            auto label = relative.wstring();
            if (label.empty() || label == L".")
                label = tr("ui.panels.root");
            auto key = std::to_wstring(category) + L":" + label;
            if (key != group) {
                group = key;
                bool collapsed = collapsedFolders[group] && search.value.empty();
                if (y >= 200 && y + 28 <= end)
                    ui.button(200000 + int(i), (collapsed ? L"▸ " : L"▾ ") + label,
                              rect(x + 12, y, right - 24, 26), [this, key] {
                                  collapsedFolders[key] = !collapsedFolders[key];
                                  libraryScroll = 0;
                              });
                y += 30;
            }
            if (collapsedFolders[group] && search.value.empty())
                continue;
            if (y >= 200 && y + 45 <= end) {
                D2D1_RECT_F r = rect(x + 12, y, right - 24, 45);
                ui.button(
                    1000 + int(i), L"", r, [this, i] { select(int(i)); }, !scanning, int(i) == selected);
                graphics.image(n.animationPortrait.width ? n.animationPortrait
                                                         : (n.hat ? n.skin->portrait : n.skin->basePortrait),
                               rect(x + 20, y + 6, 32, 32));
                graphics.text(n.name + (n.dirty ? L" *" : L""), rect(x + 62, y + 5, right - 86, 20));
                graphics.text(n.id, rect(x + 62, y + 24, right - 86, 18), 0x969BA6);
            }
            y += 51;
        }
    }
    libraryContentHeight = y + libraryScroll - 204;
    graphics.unclip();
}
void Application::bottomPanel() {
    float y = height - bottom;
    graphics.fill(rect(0, y, width, bottom - 28), 0x191C22);
    const std::wstring tabs[] = {tr("ui.panels.preview"), tr("ui.panels.echo_script"),
                                 tr("ui.tabs.diagnostics"), tr("ui.panels.assets")};
    float x = 18;
    for (int i = 0; i < 4; ++i) {
        float w = i == 1 ? 110.f : 100.f;
        ui.button(40 + i, tabs[i], rect(x, y + 9, w, 31), [this, i] { tab = i; }, true, tab == i);
        x += w + 8;
    }
    graphics.clip(rect(0, y + 48, width, bottom - 78));
    if (tab == 2) {
        float lineY = y + 54 - diagnosticsScroll;
        graphics.text(tr("ui.panels.diagnostics") + std::to_wstring(diagnostics.size()),
                      rect(18, lineY, width - 36, 24), 0x969BA6);
        lineY += 29;
        for (auto &entry : diagnostics) {
            graphics.text(entry, rect(18, lineY, width - 36, 36), 0xF49B7D);
            lineY += 38;
        }
    } else if (selected >= 0) {
        auto &n = npcs[selected];
        if (tab == 0 && leftTab == 1) {
            portraitBottom();
        } else if (tab == 0) {
            graphics.image(n.animationPortrait.width ? n.animationPortrait
                                                     : (n.hat ? n.skin->portrait : n.skin->basePortrait),
                           rect(18, y + 57, 112, 112));
            graphics.image(n.animationPortrait.width ? n.animationPortrait
                                                     : (n.hat ? n.skin->portrait : n.skin->basePortrait),
                           rect(148, y + 57, 32, 32));
            graphics.text(n.name, rect(206, y + 55, 350, 25), 0xE6E8EC, 1);
            graphics.text(
                std::to_wstring(n.skin->image.width) + L" × " + std::to_wstring(n.skin->image.height) +
                    tr("ui.panels.png_rgba_face_crop_and_hat_alpha_compositing") +
                    (n.dirty ? tr("ui.panels.unsaved_changes") : tr("ui.panels.metadata_saved_source_skin")),
                rect(206, y + 88, 400, 76), 0x969BA6);
            ui.button(44, tr("ui.panels.export_portrait"), rect(640, y + 57, 145, 33),
                      [this] { exportPortrait(); });
            ui.button(45, tr("ui.panels.face_eye_editor"), rect(640, y + 102, 145, 33),
                      [this] { openEyes(); });
        } else if (tab == 1 && leftTab == 1 && n.faceRig) {
            graphics.text(wide(n.faceRig->profile ? updatedPortraitSource(*n.faceRig->profile)
                                                  : updatedFaceSource(*n.faceRig)),
                          rect(18, y + 55, width - 240, bottom - 82), 0x969BA6, 2);
            ui.button(287, tr("ui.panels.copy_source"), rect(width - 205, y + 57, 180, 33), [this] {
                applyPortraitPart();
                auto &rig = *npcs[selected].faceRig;
                clipboard(wide(rig.profile ? updatedPortraitSource(*rig.profile) : updatedFaceSource(rig)));
            });
            ui.button(288, tr("ui.panels.save_portrait"), rect(width - 205, y + 101, 180, 33),
                      [this] { savePortraitRig(); });
            ui.button(47, tr("ui.export.save"), rect(width - 225, y + 145, 200, 33),
                      [this] { saveEchoScript(); });
        } else if (tab == 1) {
            graphics.text(gameBundlePreview(n), rect(18, y + 55, width - 240, bottom - 82), 0x969BA6, 2);
            ui.button(46, tr("ui.panels.copy_values"), rect(width - 190, y + 57, 165, 33),
                      [this] { clipboard(gameBundlePreview(npcs[selected])); });
            ui.button(47, tr("ui.export.save"), rect(width - 225, y + 101, 200, 33),
                      [this] { saveEchoScript(); });
        } else if (leftTab == 1 && tab == 3 && n.faceRig) {
            auto &rig = *n.faceRig;
            std::wstring textures = tr("ui.panels.echo_animation_textures");
            for (auto &part : rig.parts)
                textures += wide(part.id) + L" → " + part.texturePath.filename().wstring() + L"\n";
            std::wstring data = tr("ui.panels.related_data") + rig.path.filename().wstring() + L"\n";
            if (rig.profile)
                data += rig.profile->path.filename().wstring() + L"\n";
            for (auto &path : rig.related)
                data += path.filename().wstring() + L"\n";
            graphics.text(textures, rect(18, y + 55, width * .43f, bottom - 82), 0x969BA6);
            graphics.text(data, rect(width * .46f, y + 55, width * .36f, bottom - 82), 0x969BA6);
            ui.button(289, tr("ui.panels.texture_folder"), rect(width - 205, y + 57, 180, 33),
                      [this] { reveal(npcs[selected].faceRig->parts.front().texturePath.parent_path()); });
            ui.button(290, tr("ui.panels.face_rig_file"), rect(width - 205, y + 101, 180, 33),
                      [this] { reveal(npcs[selected].faceRig->path); });
        } else {
            graphics.text(tr("ui.panels.skin_linked") + n.skin->path.wstring() + tr("ui.panels.metadata") +
                              n.metadata.wstring() +
                              tr("ui.panels.original_png_is_retained_without_recompression"),
                          rect(18, y + 55, width - 240, bottom - 80), 0x969BA6);
            ui.button(48, tr("ui.panels.copy_skin_to"), rect(width - 190, y + 57, 165, 33), [this] {
                auto folder = folderDialog();
                if (folder.empty())
                    return;
                auto &current = npcs[selected];
                auto destination = folder / current.skin->path.filename();
                if (fs::exists(destination) && fs::equivalent(destination, current.skin->path))
                    return;
                if (fs::exists(destination) &&
                    messageBox(window, tr("ui.panels.overwrite_destination_png"), tr("ui.panels.copy_skin"),
                               MB_YESNO | MB_ICONQUESTION) != IDYES)
                    return;
                fs::copy_file(current.skin->path, destination, fs::copy_options::overwrite_existing);
                updateSkin(loadSkin(destination));
            });
            ui.button(
                49, tr("ui.panels.reveal_metadata"), rect(width - 190, y + 101, 165, 33),
                [this] { reveal(npcs[selected].metadata); }, fs::exists(n.metadata));
        }
    } else
        graphics.text(tr("ui.panels.select_an_npc_to_see_its_assets"), rect(18, y + 58, width - 36, 35),
                      0x969BA6);
    graphics.unclip();
    graphics.fill(rect(0, height - 28, width, 28), 0x101216);
    graphics.text(status, rect(12, height - 22, width - 24, 23), 0x969BA6);
}
void Application::dialogs() {
    if (modelDropdown && selected >= 0 && !createDialog && !eyeDialog) {
        float y = 466 - propertiesScroll;
        if (y >= 110 && y + 113 < height - bottom) {
            graphics.fill(rect(16, y + 35, left - 32, 78), 0x343A46, 5);
            ui.button(
                90, tr("ui.repository.classic"), rect(20, y + 39, left - 40, 32),
                [this] {
                    npcs[selected].slim = false;
                    npcs[selected].dirty = true;
                    modelDropdown = false;
                },
                true, !npcs[selected].slim);
            ui.button(
                91, tr("ui.repository.slim"), rect(20, y + 75, left - 40, 32),
                [this] {
                    npcs[selected].slim = true;
                    npcs[selected].dirty = true;
                    modelDropdown = false;
                },
                true, npcs[selected].slim);
        } else
            modelDropdown = false;
    }
    if (contextIndex >= 0 && !createDialog && !eyeDialog) {
        int index = contextIndex;
        float x = std::clamp(contextX, 0.f, width - 195), y = std::clamp(contextY, 0.f, height - 215);
        graphics.fill(rect(x - 4, y - 4, 194, 209), 0x343A46, 5);
        const std::wstring labels[] = {tr("ui.panels.open"), tr("ui.panels.duplicate"),
                                       tr("ui.panels.rename"), tr("ui.panels.delete"),
                                       tr("ui.panels.reveal_in_explorer")};
        for (int i = 0; i < 5; ++i)
            ui.button(60 + i, labels[i], rect(x, y + i * 40, 186, 36), [this, index, i] {
                contextIndex = -1;
                switch (i) {
                case 0:
                    select(index);
                    break;
                case 1:
                    duplicateNpc(index);
                    break;
                case 2:
                    select(index);
                    ui.focusInput(display);
                    status = tr("ui.panels.edit_display_name_then_save");
                    break;
                case 3:
                    removeNpc(index);
                    break;
                case 4:
                    reveal(npcs[index].skin->path);
                    break;
                }
            });
    }
    if (createDialog) {
        ui.widgets.clear();
        float x = (width - 560) / 2, y = (height - 495) / 2;
        graphics.fill(rect(x - 8, y - 8, 576, 511), 0x343A46, 9);
        graphics.fill(rect(x, y, 560, 495), 0x191C22, 7);
        graphics.text(tr("ui.application.create_npc"), rect(x + 24, y + 20, 245, 35), 0xE6E8EC, 1);
        ui.button(530, tr("ui.template.background"), rect(x + 280, y + 17, 256, 32),
                  [this] { applyBackgroundTemplate(false); });
        float fy = y + 70;
        auto field = [&](const std::wstring &label, TextEdit &edit, int id, bool browse) {
            graphics.text(label, rect(x + 24, fy, 510, 20), 0x969BA6);
            ui.input(id, edit, rect(x + 24, fy + 23, browse ? 420.f : 512.f, 34));
            fy += 72;
        };
        field(tr("ui.panels.identifier"), newId, 70, false);
        field(tr("ui.panels.display_name"), newName, 71, false);
        field(tr("ui.panels.skin_file"), newSkin, 72, true);
        ui.button(73, tr("ui.panels.browse"), rect(x + 454, fy - 49, 82, 34), [this] {
            auto p = fileDialog(false, tr("ui.panels.png_skin_png"));
            if (!p.empty())
                newSkin.set(p.wstring());
        });
        field(tr("ui.panels.destination_folder"), newFolder, 74, true);
        ui.button(75, tr("ui.panels.browse"), rect(x + 454, fy - 49, 82, 34), [this] {
            auto p = folderDialog();
            if (!p.empty())
                newFolder.set(p.wstring());
        });
        ui.button(76, newSlim ? tr("ui.panels.model_slim") : tr("ui.panels.model_classic"),
                  rect(x + 24, fy, 512, 33), [this] { newSlim = !newSlim; });
        ui.button(77, tr("ui.panels.create"), rect(x + 320, y + 435, 102, 35), [this] { createNpc(); });
        ui.button(78, tr("ui.panels.cancel"), rect(x + 434, y + 435, 102, 35), [this] {
            createDialog = false;
            ui.clearFocus();
        });
    }
    if (eyeDialog) {
        ui.widgets.clear();
        float x = (width - 610) / 2, y = (height - 540) / 2;
        graphics.fill(rect(x - 8, y - 8, 626, 556), 0x343A46, 9);
        graphics.fill(rect(x, y, 610, 540), 0x191C22, 7);
        graphics.text(tr("ui.panels.face_eye_editor"), rect(x + 26, y + 20, 550, 30), 0xE6E8EC, 1);
        graphics.text(tr("ui.panels.heuristic_selection_inspect_and_correct_both_masks"),
                      rect(x + 26, y + 57, 554, 26), 0xF4C478);
        graphics.fill(rect(x + 30, y + 90, 320, 320), 0x272D37);
        graphics.image(face, rect(x + 30, y + 90, 320, 320));
        float cell = 320.f / face.width;
        for (unsigned yy = 0; yy < face.height; ++yy)
            for (unsigned xx = 0; xx < face.width; ++xx) {
                size_t p = yy * face.width + xx;
                if (eyes.left[p] || eyes.right[p]) {
                    float px = x + 30 + xx * cell, py = y + 90 + yy * cell;
                    uint32_t c = eyes.left[p] ? 0x2585E6 : 0xE6A325;
                    graphics.line(px + 1, py + 1, px + cell - 1, py + 1, c, 2);
                    graphics.line(px + 1, py + cell - 1, px + cell - 1, py + cell - 1, c, 2);
                }
            }
        for (unsigned i = 0; i <= face.width; ++i) {
            float p = i * cell;
            graphics.line(x + 30 + p, y + 90, x + 30 + p, y + 410, 0x525866, .5f);
            graphics.line(x + 30, y + 90 + p, x + 350, y + 90 + p, 0x525866, .5f);
        }
        ui.button(
            80, tr("ui.panels.left_eye_mask"), rect(x + 376, y + 95, 206, 35), [this] { eyeSide = 0; }, true,
            eyeSide == 0);
        ui.button(
            81, tr("ui.panels.right_eye_mask"), rect(x + 376, y + 142, 206, 35), [this] { eyeSide = 1; },
            true, eyeSide == 1);
        ui.button(82, tr("ui.panels.detect_again"), rect(x + 376, y + 205, 206, 35),
                  [this] { eyes = detectEyes(face); });
        ui.button(83, tr("ui.panels.clear_selected"), rect(x + 376, y + 252, 206, 35), [this] {
            auto &mask = eyeSide == 0 ? eyes.left : eyes.right;
            std::fill(mask.begin(), mask.end(), uint8_t{0});
        });
        ui.button(84, tr("ui.panels.export_masks"), rect(x + 376, y + 315, 206, 35),
                  [this] { exportEyes(); });
        graphics.text(tr("ui.panels.confidence") + std::to_wstring(int(eyes.confidence * 100)) +
                          tr("ui.panels.click_or_drag_to_toggle_pixels_left_right_refer_to"),
                      rect(x + 376, y + 370, 206, 110), 0x969BA6);
        ui.button(85, tr("ui.panels.close"), rect(x + 476, y + 487, 106, 33), [this] {
            eyeDialog = false;
            ui.clearFocus();
        });
    }
}
} // namespace echo
