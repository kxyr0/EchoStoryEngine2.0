#include "app/Application.h"
#include <commdlg.h>
#include <fstream>
#include <shlobj.h>
namespace echo {
static std::wstring lower(std::wstring s) {
    std::transform(s.begin(), s.end(), s.begin(), towlower);
    return s;
}
void Application::smokeTest() {
    if (smokeFailed)
        return;
    if (std::chrono::steady_clock::now() - start > std::chrono::seconds(30))
        throw std::runtime_error("Smoke test timed out");
    if (scanning || imageLoading)
        return;
    auto require = [](bool ok, const char *reason) {
        if (!ok)
            throw std::runtime_error(reason);
    };
    auto click = [&](int id) {
        for (auto &widget : ui.widgets)
            if (widget.id == id && widget.enabled) {
                auto r = widget.bounds;
                float x = (r.left + r.right) / 2, y = (r.top + r.bottom) / 2;
                ui.down(x, y);
                ui.up(x, y);
                return;
            }
        throw std::runtime_error("Smoke test: widget not available");
    };
    if (smokeStage == 0) {
        require(!npcs.empty(), "Scan did not find a skin");
        auto it = std::find_if(npcs.begin(), npcs.end(), [](const Npc &n) {
            return lower(n.skin->path.filename().wstring()) == L"ben.png";
        });
        if (it != npcs.end())
            select(int(it - npcs.begin()));

        require(npcs[selected].name == L"Бен", "Localized Ben name missing in library");
        head = true;
        yaw = pitch = 0;
        graphics.captureNextFrame = smokeFolder / L"head-front.png";
        draw();
        yaw = .55f;
        graphics.captureNextFrame = smokeFolder / L"head-orbit.png";
        draw();
        auto oldHat = npcs[selected].hat;
        npcs[selected].hat = false;
        graphics.captureNextFrame = smokeFolder / L"head-no-hat.png";
        draw();
        npcs[selected].hat = oldHat;
        head = false;
        yaw = 0;
        pitch = .05f;
        draw();
        click(11);
        display.key('A', true, false, window);
        for (wchar_t c : std::wstring(L"Бен — тест сохранения"))
            message(WM_CHAR, c, 0);
        sync();
        require(npcs[selected].dirty, "Text editing did not set dirty flag");
        npcs[selected].metadata = smokeFolder / L"ben-test.echo-npc";
        if (fs::exists(npcs[selected].metadata))
            throw std::runtime_error("Smoke test output must be a fresh folder");
        draw();
        click(3);
        require(!npcs[selected].dirty, "Save did not clear dirty flag");
        Npc loaded = npcFromSkin(npcs[selected].skin);
        loaded.metadata = npcs[selected].metadata;
        readMetadata(loaded);
        require(loaded.name == L"Бен — тест сохранения", "Saved Unicode name did not roundtrip");
        graphics.captureNextFrame = smokeFolder / L"front.png";
        draw();
    } else if (smokeStage == 1) {
        float x = (left + width - right) / 2, y = 300;
        message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(x * scale), int(y * scale)));
        message(WM_MOUSEMOVE, MK_LBUTTON, MAKELPARAM(int((x + 90) * scale), int((y + 20) * scale)));
        message(WM_LBUTTONUP, 0, MAKELPARAM(int((x + 90) * scale), int((y + 20) * scale)));
        require(yaw < -.5f && pitch < 0, "Inverted mouse orbit failed");
        graphics.captureNextFrame = smokeFolder / L"orbit.png";
        draw();
        click(16);
        draw();
        click(91);
        require(npcs[selected].slim, "Slim model toggle failed");
        npcs[selected].dirty = false;
        draw();
        require(!showSkinLayers, "Skin layers should start collapsed");
        require(std::none_of(ui.widgets.begin(), ui.widgets.end(), [](const auto &w) { return w.id == 17; }),
                "Collapsed skin layer controls still visible");
        click(511);
        draw();
        require(showSkinLayers && std::any_of(ui.widgets.begin(), ui.widgets.end(),
                                              [](const auto &w) { return w.id == 17; }),
                "Expanding skin layers failed");
        click(511);
        draw();
        require(!npcs[selected].dirty, "Collapsing settings changed NPC data");
        graphics.captureNextFrame = smokeFolder / L"current-npc-properties.png";
        draw();
    } else if (smokeStage == 2) {
        graphics.captureNextFrame = smokeFolder / L"slim.png";
        draw();
        openEyes();
        require(eyes.left.size() == size_t(face.width) * face.height, "Eye mask invalid");
        eyePaint((width - 610) / 2 + 30 + 60, (height - 540) / 2 + 90 + 180, true);
        painting = false;
        ReleaseCapture();
        graphics.captureNextFrame = smokeFolder / L"eyes.png";
        draw();
        savePng(smokeFolder / L"left-eye.png", maskImage(face, eyes.left));
        savePng(smokeFolder / L"portrait.png", face);
        eyeDialog = false;
        if (npcs[selected].faceRig) {
            draw();
            click(510);
            require(leftTab == 1 && !faceWizard, "Existing NPC face settings did not open its actual layers");
            selectPortraitPart(2);
            partX.set(std::to_wstring(std::stod(partX.value) + .01));
            applyPortraitPart();
            float px = (portraitCanvas.left + portraitCanvas.right) / 2,
                  py = (portraitCanvas.top + portraitCanvas.bottom) / 2;
            draw();
            px = (portraitCanvas.left + portraitCanvas.right) / 2;
            py = (portraitCanvas.top + portraitCanvas.bottom) / 2;

            auto widthBefore = areaW.value;
            areaW.set(L"2");
            applyPortraitPart();
            require(npcs[selected].faceRig->profile->layers.at("pupil_left").values.at("w") == 2,
                    "Current NPC portrait width input is not editable");
            portraitUndo();
            require(areaW.value == widthBefore, "Portrait bounds undo failed");
            auto &faceRig = *npcs[selected].faceRig;
            require(faceRig.profile.has_value(), "Minecraft portrait profile missing");
            auto originalX = faceQuads(faceRig)[2].placeX;
            auto siblingX = faceQuads(faceRig)[3].placeX;
            portraitGroup = 3; // Both pupils are a single edit operation.
            message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(px * scale), int(py * scale)));
            message(WM_MOUSEMOVE, MK_LBUTTON, MAKELPARAM(int((px + 8) * scale), int(py * scale)));
            message(WM_MOUSEMOVE, MK_LBUTTON, MAKELPARAM(int((px + 16) * scale), int(py * scale)));
            message(WM_LBUTTONUP, 0, MAKELPARAM(int((px + 16) * scale), int(py * scale)));
            auto movedX = faceQuads(faceRig)[2].placeX;
            require(movedX > originalX, "Portrait dragging failed");
            require(std::abs((faceQuads(faceRig)[3].placeX - siblingX) - (movedX - originalX)) < 1e-8,
                    "Group move changed relative pupil placement");
            auto key = [&](WPARAM code, bool ctrl, bool shift = false) {
                BYTE previous[256], keys[256]{};
                GetKeyboardState(previous);
                if (ctrl)
                    keys[VK_CONTROL] = 0x80;
                if (shift)
                    keys[VK_SHIFT] = 0x80;
                SetKeyboardState(keys);
                message(WM_KEYDOWN, code, 0);
                SetKeyboardState(previous);
            };
            key('Z', true);
            require(std::abs(faceQuads(faceRig)[2].placeX - originalX) < 1e-8,
                    "Ctrl+Z did not undo the entire drag");
            key('Y', true);
            require(std::abs(faceQuads(faceRig)[2].placeX - movedX) < 1e-8, "Ctrl+Y failed");
            portraitGroup = 0;
            portraitStep = .1;
            key(VK_RIGHT, false, true);
            require(std::abs(faceQuads(faceRig)[2].placeX - movedX - .000625) < 1e-8,
                    "Shift+arrow fine movement failed");
            key('Z', true);
            require(std::abs(faceQuads(faceRig)[2].placeX - movedX) < 1e-8, "Nudge undo failed");
            portraitGroup = 1;
            auto beforeEye = faceQuads(faceRig);
            movePortrait(.00625, -.00625);
            auto afterEye = faceQuads(faceRig);
            for (size_t i = 0; i < beforeEye.size(); ++i) {
                bool included = portraitIncluded(beforeEye[i], int(i));
                require(std::abs(afterEye[i].placeX - beforeEye[i].placeX - (included ? .00625 : 0)) < 1e-8,
                        "Whole-eye movement affected wrong layers");
            }
            portraitUndo();
            portraitGroup = 0;
            graphics.captureNextFrame = smokeFolder / L"portrait-editor.png";
            draw();
            savePng(smokeFolder / L"composed-portrait.png", portraitComposite);
            auto &rig = *npcs[selected].faceRig;

            auto sourceRig = rig.source;
            savePortraitProfile(*rig.profile, smokeFolder / L"ben-edited.portrait-overlay.es");
            require(!rig.profile->dirty && rig.source == sourceRig && !rig.dirty,
                    "Profile save affected source rig or dirty state");
            portraitUndo();
            require(rig.profile->dirty, "Undo after save must mark profile dirty");
            portraitUndo(true);
            require(!rig.profile->dirty, "Redo to saved state must clear dirty flag");

            recordPortrait();
            rig.profile->layers.at("pupil_left").values["visible"] = 0;
            rig.profile->dirty = true;
            savePortraitProfile(*rig.profile, smokeFolder / L"ben-hidden.portrait-overlay.es");
            portraitUndo();
            require(faceQuads(rig)[2].visible && rig.profile->dirty,
                    "Undo after saving inserted visible field failed");
            portraitUndo(true);
            require(!faceQuads(rig)[2].visible && !rig.profile->dirty, "Redo inserted visible field failed");
            portraitUndo();
            savePortraitProfile(*rig.profile, smokeFolder / L"ben-restored.portrait-overlay.es");
            portraitClosure = 1;
            rebuildPortrait();
            graphics.captureNextFrame = smokeFolder / L"portrait-blink.png";
            draw();
            portraitClosure = 0;
            leftTab = 0;
            rebuildPortrait();
        }
        auto modern = std::find_if(npcs.begin(), npcs.end(), [](const Npc &n) {
            return n.skin->path.stem().wstring().find(L"skin_Modern_") != std::wstring::npos;
        });
        if (modern != npcs.end()) {
            select(int(modern - npcs.begin()));
            draw();
            click(400);
            require(faceDraft.crops[0].y == 5 && faceDraft.crops[0].h == 2 && faceDraft.crops[4].y == 4,
                    "Reported shaded modern skin regression");
            graphics.captureNextFrame = smokeFolder / L"face-wizard-shaded.png";
            draw();
            click(441);
        }
        auto blue = std::find_if(npcs.begin(), npcs.end(), [](const Npc &n) {
            return n.skin->path.stem().wstring().find(L"skin_Blue Eyes_White Hair") != std::wstring::npos;
        });
        require(blue != npcs.end(), "Actual raw blue-eye skin missing");
        select(int(blue - npcs.begin()));
        draw();
        auto originalNameColor = npcs[selected].color;
        auto originalDirty = npcs[selected].dirty;
        click(512);
        draw();
        require(nameColorPicker, "Clothing color picker did not open");
        graphics.captureNextFrame = smokeFolder / L"name-color-clothing.png";
        draw();
        float transparentX = clothingFrontRect.left + 6, transparentY = clothingFrontRect.top + 6;
        message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(transparentX * scale), int(transparentY * scale)));
        message(WM_LBUTTONUP, 0, MAKELPARAM(int(transparentX * scale), int(transparentY * scale)));
        require(nameColorPicker && npcs[selected].color == originalNameColor,
                "Transparent clothing picker background changed name color");
        unsigned factor = npcs[selected].skin->image.width / 64;
        auto *clothPixel = &clothingFront.rgba[(size_t(12 * factor) * clothingFront.width + 7 * factor) * 4];
        wchar_t expectedColor[8];
        swprintf_s(expectedColor, L"#%02X%02X%02X", clothPixel[0], clothPixel[1], clothPixel[2]);
        float clothX = clothingFrontRect.left + 7.5f * 12, clothY = clothingFrontRect.top + 12.5f * 12;
        message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(clothX * scale), int(clothY * scale)));
        message(WM_LBUTTONUP, 0, MAKELPARAM(int(clothX * scale), int(clothY * scale)));
        require(!nameColorPicker && npcs[selected].color == expectedColor && color.value == expectedColor,
                "Clothing picker did not use exact unlit RGB");
        draw();
        click(512);
        draw();
        click(460);
        require(!nameColorPicker && color.value == expectedColor, "Cancel color picker changed color");
        color.set(originalNameColor);
        sync();
        npcs[selected].dirty = originalDirty;
        draw();
        click(400);
        require(faceWizard && faceDraft.crops[0].y == 5 && faceDraft.crops[0].h == 2,
                "Automatic real-skin eye detection failed");
        auto originalSkin = faceDraft.source->image.rgba;
        auto authorPack = smokeFolder / L"story" / L"content-pack" / L"jdh";
        fs::create_directories(authorPack / L"assets");
        fs::create_directories(authorPack / L"data");
        draftPack.set(authorPack.wstring());
        draw();
        auto templateCrops = faceDraft.crops;
        click(530);
        require(draftId.value.starts_with(L"jdh:background_") && !duplicateId(draftId.value) &&
                    draftName.value == L"Незнакомец" && draftNpcColor == L"#DEC6B2" &&
                    faceDraft.crops == templateCrops,
                "Background template did not fill defaults or changed face regions");
        auto reservedBackgroundId = draftId.value;
        fs::create_directories(authorPack / L"assets/jdh/textures/echo_animation/faces" /
                               reservedBackgroundId.substr(4));
        draw();
        click(530);
        require(draftId.value != reservedBackgroundId && !duplicateId(draftId.value),
                "Template reused an occupied face resource");
        auto generatedBackgroundId = draftId.value;
        graphics.captureNextFrame = smokeFolder / L"background-template.png";
        draw();
        graphics.captureNextFrame = smokeFolder / L"face-wizard-two.png";
        draw();
        click(416);
        require(faceDraft.aperture == 1 && faceDraft.crops[2].h == 2,
                "One-pixel aperture incorrectly cropped pupil texture");
        graphics.captureNextFrame = smokeFolder / L"face-wizard-one.png";
        draw();
        click(415);
        draw();
        click(422);
        float sx = draftCanvas.left + 2 * 40 + 20, sy = draftCanvas.top + 5 * 40 + 20;
        message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(sx * scale), int(sy * scale)));
        message(WM_MOUSEMOVE, MK_LBUTTON, MAKELPARAM(int(sx * scale), int((sy + 40) * scale)));
        message(WM_LBUTTONUP, 0, MAKELPARAM(int(sx * scale), int((sy + 40) * scale)));
        require(faceDraft.crops[2].x == 2 && faceDraft.crops[2].h == 2, "Grid crop editing failed");
        auto drag = [&](float ax, float ay, float bx, float by) {
            message(WM_LBUTTONDOWN, 0, MAKELPARAM(int(ax * scale), int(ay * scale)));
            message(WM_MOUSEMOVE, MK_LBUTTON, MAKELPARAM(int(bx * scale), int(by * scale)));
            message(WM_LBUTTONUP, 0, MAKELPARAM(int(bx * scale), int(by * scale)));
        };
        auto wizardKey = [&](WPARAM code, bool ctrl = false, bool shift = false) {
            BYTE oldKeys[256], keys[256]{};
            GetKeyboardState(oldKeys);
            if (ctrl)
                keys[VK_CONTROL] = 0x80;
            if (shift)
                keys[VK_SHIFT] = 0x80;
            SetKeyboardState(keys);
            message(WM_KEYDOWN, code, 0);
            SetKeyboardState(oldKeys);
        };
        auto pupilBefore = faceDraft.crops[2];
        float rightEdge = draftCanvas.left + 3 * 40, lowerEdge = draftCanvas.top + 7 * 40;
        drag(rightEdge, lowerEdge, rightEdge, lowerEdge - 40);
        require(faceDraft.crops[2].h == 1 && draftH.value == L"1" &&
                    facePackage.textures.at(L"pupil_left").height == 1,
                "Corner resizing did not update crop, numeric input and live extracted texture");
        wizardKey('Z', true);
        require(faceDraft.crops[2] == pupilBefore, "Wizard Ctrl+Z did not undo entire resize");
        wizardKey('Y', true);
        require(faceDraft.crops[2].h == 1, "Wizard redo failed");
        wizardKey('Z', true);
        draw();
        click(424);
        auto browBefore = faceDraft.crops[4];
        draw();
        click(445);
        draw();
        float mx = draftCanvas.left + (browBefore.x + browBefore.w * .5f) * 40;
        float my = draftCanvas.top + (browBefore.y + .5f) * 40;
        drag(mx, my, mx + 40, my);
        require(faceDraft.crops[4] == browBefore, "Quarter sensitivity moved too far");
        drag(mx, my, mx + 160, my);
        require(faceDraft.crops[4].x == browBefore.x + 1 && faceDraft.crops[4].w == browBefore.w,
                "Dragging inside region resized instead of moving or ignored sensitivity");
        wizardKey('Z', true);
        require(faceDraft.crops[4] == browBefore, "Whole-region drag undo failed");
        draw();
        click(446);
        draw();
        draftX.set(std::to_wstring(browBefore.x + 1));
        wizardKey(VK_RETURN);
        require(faceDraft.crops[4].x == browBefore.x + 1, "Enter did not apply manual numeric coordinates");
        wizardKey('Z', true);
        draftPower.set(L"нечисло");
        drag(mx, my, mx + 40, my);
        require(!draftDragging && faceDraft.crops[4] == browBefore && !draftError.empty(),
                "Invalid sensitivity changed region");
        draftPower.set(L"1");
        rebuildDraft();
        draw();
        click(422);
        graphics.captureNextFrame = smokeFolder / L"face-wizard-manual.png";
        draw();
        draw();
        click(489);
        draw();
        require(draftSection == 1, "Eyelid tab did not open");
        auto originalLids = faceDraft.lids;
        lidOffset.set(L"0.25");
        lidHeight.set(L"2");
        lidTravel.set(L"45");
        lidClosure.set(L"1");
        click(500);
        draw();
        require(faceDraft.lids[0].height == 2 && faceDraft.lids[0].offset == .25 &&
                    faceDraft.lids[0].travel == 45,
                "Upper eyelid numeric editing failed");
        auto editedLidRig = parseFaceRig(facePackage.rig, {}, {});
        require(editedLidRig.upperTravel == 45 &&
                    facePackage.textures.at(L"upper_eyelids").rgba[(14 * 64 + 9) * 4 + 3] == 255,
                "Eyelid parameters not exported to rig and atlas");
        graphics.captureNextFrame = smokeFolder / L"eyelids-editor.png";
        draw();
        draftUndo();
        require(faceDraft.lids == originalLids, "Eyelid undo failed");
        draftClosure = 0;
        refreshLidFields();
        rebuildDraft();
        draw();
        click(491);
        draw();
        auto eyeForLid = faceDraft.crops[0];
        float lx = draftCanvas.left + (eyeForLid.x + eyeForLid.w / 2.f) * 40;
        float ly =
            draftCanvas.top + float(eyeForLid.y + eyeForLid.h / 2. + faceDraft.aperture / 2. + .5) * 40;
        require(draftHit((draftCanvas.left + draftCanvas.right) / 2, ly) == -1 && draftHit(lx, ly) == 0,
                "Eyelid bounds must cover each eye, not the face between them");
        const auto &lowerAtlas = facePackage.textures.at(L"lower_eyelids");
        for (int xx = 0; xx < 8; ++xx) {
            bool inEye = false;
            for (int side = 0; side < 2; ++side) {
                const auto &crop = faceDraft.crops[side];
                inEye |= xx >= crop.x && xx < crop.x + crop.w;
            }
            require(lowerAtlas.rgba[(12 * 64 + 8 + xx) * 4 + 3] == (inEye ? 255 : 0),
                    "Eyelid mask includes pixels outside the eyes");
        }
        require(eyelidMini[2].width == unsigned(eyeForLid.w), "Eyelid preview has full-face width");
        drag(lx, ly, lx, ly + 40);
        require(faceDraft.lids[1].offset == 1, "Lower eyelid grid drag failed");
        draftUndo();
        draw();
        click(488);
        draw();
        draw();
        click(440);
        require(!faceWizard && npcs[selected].id == generatedBackgroundId &&
                    npcs[selected].name == L"Незнакомец" && npcs[selected].faceRig,
                "Generated NPC creation failed");
        require(faceDraft.source->image.rgba == originalSkin, "Wizard mutated source skin");
        std::atomic_bool authorStop{false};
        auto generated = scanProject(authorPack, authorStop);
        require(generated.npcs.size() == 1 && generated.diagnostics.empty() && generated.npcs[0].faceRig,
                "Generated NPC resources failed reload");
        leftTab = 0;
        head = true;
        yaw = pitch = 0;
        graphics.captureNextFrame = smokeFolder / L"generated-npc-head.png";
        draw();
        head = false;
        auto rawGuiPath =
            authorPack / L"assets" / L"jdh" / L"textures" / L"echo_npc" / L"skins" / L"current_gui.png";
        savePng(rawGuiPath, faceDraft.source->image);
        auto rawGui = npcFromSkin(loadSkin(rawGuiPath));
        rawGui.id = L"jdh:current_gui";
        rawGui.name = L"Текущий NPC";
        saveMetadata(rawGui, rawGui.metadata);
        npcs.push_back(std::move(rawGui));
        select(int(npcs.size()) - 1);
        leftTab = 0;
        draw();
        auto countBeforeCurrent = npcs.size();
        tab = 1;
        draw();
        click(47);
        draw();
        require(faceWizard && editCurrentFace && exportAfterWizard && draftId.value == L"jdh:current_gui",
                "Raw current NPC opened duplicate creation mode");
        require(std::none_of(ui.widgets.begin(), ui.widgets.end(),
                             [](const Widget &widget) { return widget.id == 530; }),
                "Creation template must not change an existing NPC");
        require(std::any_of(ui.widgets.begin(), ui.widgets.end(),
                            [](const auto &w) { return w.id == 410 && !w.enabled; }),
                "Current NPC identifier must remain fixed");
        graphics.captureNextFrame = smokeFolder / L"current-face-settings.png";
        draw();
        click(440);
        require(npcs.size() == countBeforeCurrent && npcs[selected].id == L"jdh:current_gui" &&
                    npcs[selected].faceRig,
                "Saving current face created duplicate NPC");
        auto currentReload = scanProject(authorPack, authorStop);
        require(currentReload.npcs.size() == 2 && currentReload.diagnostics.empty(),
                "Current face failed pack reload");
        display.set(L"Незнакомец после экспорта");
        color.set(L"#ABCDEF");
        sync();
        selectPortraitPart(6);
        movePortrait(.125, 0);
        auto depthPart = faceQuads(*npcs[selected].faceRig).at(portraitPart).part;
        partZ.set(L"0.07");
        applyPortraitPart();
        require(npcs[selected].faceRig->parts[depthPart].position[2] == .07 && npcs[selected].faceRig->dirty,
                "Depth must be editable with a connected portrait profile");
        portraitUndo();
        require(npcs[selected].faceRig->parts[depthPart].position[2] == .02, "Depth undo failed");
        portraitUndo(true);
        leftTab = 1;
        draw();
        portraitScroll = portraitMaxScroll;
        draw();
        require(std::any_of(ui.widgets.begin(), ui.widgets.end(),
                            [](const auto &w) { return w.id == 262 && w.enabled; }),
                "Z input remains disabled with a portrait profile");
        graphics.captureNextFrame = smokeFolder / L"depth-editor.png";
        draw();
        portraitScroll = 0;
        draw();
        click(566);
        require(npcs[selected].faceRig->parts[depthPart].position[2] == .02, "Default depth button failed");
        portraitUndo();
        require(npcs[selected].faceRig->parts[depthPart].position[2] == .07,
                "Default depth button undo failed");
        auto expectedDepthSource = updatedFaceSource(*npcs[selected].faceRig);
        auto expectedFullProfile = updatedPortraitSource(*npcs[selected].faceRig->profile);
        tab = 1;
        leftTab = 1;
        draw();
        click(47);
        auto fullReload = scanProject(authorPack, authorStop);
        auto fullNpc = std::find_if(fullReload.npcs.begin(), fullReload.npcs.end(),
                                    [](const auto &n) { return n.id == L"jdh:current_gui"; });
        require(fullNpc != fullReload.npcs.end() && fullNpc->name == L"Незнакомец после экспорта" &&
                    fullNpc->color == L"#ABCDEF" &&
                    fullNpc->faceRig->profile->source == expectedFullProfile &&
                    fullNpc->faceRig->source == expectedDepthSource && !npcs[selected].dirty,
                "Save to EchoScript did not persist NPC and portrait together");
        leftTab = 0;
        tab = 1;
        draw();
        libraryScroll = 0;
        collapsedCategories[0] = true;
        draw();
        require(std::any_of(ui.widgets.begin(), ui.widgets.end(), [](const auto &w) { return w.id == 561; }),
                "Raw skin category not visible after collapsing scripted NPCs");
        click(561);
        draw();
        require(collapsedCategories[1], "Raw skin category did not collapse");
        graphics.captureNextFrame = smokeFolder / L"echo-script-export.png";
        draw();
        collapsedCategories = {};
        tab = 0;
        leftTab = 0;
        draw();
        float oldLeft = left, oldRight = right, oldBottom = bottom;
        draw();
        click(480);
        draw();
        click(481);
        draw();
        click(482);
        draw();
        require(left == 0 && right == 0 && bottom == 28 && !showProperties && !showLibrary && !showBottom,
                "Hidden panels did not release viewport space");
        graphics.captureNextFrame = smokeFolder / L"panels-hidden.png";
        draw();
        click(480);
        draw();
        click(481);
        draw();
        click(482);
        draw();
        require(left == oldLeft && right == oldRight && bottom == oldBottom,
                "Restoring panels lost their sizes");
    } else if (smokeStage == 3) {
        SetWindowPos(window, nullptr, 0, 0, int(1160 * scale), int(780 * scale),
                     SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
        layout();
        graphics.captureNextFrame = smokeFolder / L"resized.png";
        draw();
        recover();
        draw();
    } else if (smokeStage == 4) {
        createDialog = true;
        newSkin.set(npcs[selected].skin->path.wstring());
        newFolder.set(smokeFolder.wstring());
        newSlim = true;
        draw();
        click(530);
        require(newId.value.starts_with(L"jdh:background_") && !duplicateId(newId.value) &&
                    newName.value == L"Незнакомец" && newSlim,
                "Metadata dialog background template defaults or model preservation failed");
        graphics.captureNextFrame = smokeFolder / L"background-template-metadata.png";
        draw();
        auto metadataBackgroundId = newId.value;
        click(77);
        require(npcs[selected].id == metadataBackgroundId && npcs[selected].name == L"Незнакомец" &&
                    npcs[selected].color == L"#DEC6B2" && fs::exists(npcs[selected].metadata),
                "Create NPC button failed");
        auto path = npcs[selected].skin->path.wstring();
        HGLOBAL memory = GlobalAlloc(GHND, sizeof(DROPFILES) + (path.size() + 2) * sizeof(wchar_t));
        require(memory != nullptr, "Drop payload allocation failed");
        auto payload = static_cast<DROPFILES *>(GlobalLock(memory));
        payload->pFiles = sizeof(DROPFILES);
        payload->fWide = TRUE;
        payload->pt = {LONG((left + 80) * scale), LONG(200 * scale)};
        memcpy(reinterpret_cast<BYTE *>(payload) + sizeof(DROPFILES), path.c_str(),
               (path.size() + 1) * sizeof(wchar_t));
        GlobalUnlock(memory);
        message(WM_DROPFILES, reinterpret_cast<WPARAM>(memory), 0);
    } else if (smokeStage == 5) {
        require(npcs[selected].dirty, "Drag/drop did not mark NPC dirty");
        KillTimer(window, 3); // Modal dialogs pump messages; avoid re-entering this test stage.
        smokeCloseChoice = 102;
        message(WM_CLOSE, 0, 0);
        require(IsWindow(window) && unsaved(), "Cancel exit did not retain unsaved editor");
        smokeCloseChoice = 100;
        require(confirmClose() && !unsaved(), "Save-and-exit did not save pending changes");
        auto ben = std::find_if(npcs.begin(), npcs.end(), [](auto &n) { return n.id == L"jdh:ben"; });
        require(ben != npcs.end(), "Ben missing for unsaved portrait exit test");
        select(int(ben - npcs.begin()));
        auto profileSource = npcs[selected].faceRig->profile->source;
        auto profilePath = npcs[selected].faceRig->profile->path;
        partX.set(L"нечисловое значение");
        require(unsaved(), "Invalid pending portrait input not detected");
        smokeCloseChoice = 102;
        message(WM_CLOSE, 0, 0);
        require(IsWindow(window) && partX.value == L"нечисловое значение",
                "Cancel discarded pending portrait input");
        std::ofstream(smokeFolder / L"passed.txt")
            << "PASS: native D3D11/D2D window, recursive PNG scan, real skin rendering, Unicode text edit, "
               "Save button + readback, mouse orbit, Slim dropdown, portrait, eye mask editing/export, "
               "resize, graphics recreation, Create NPC, asynchronous PNG drag/drop, Echo Animation face "
               "catalog, portrait tab, layer editing and dragging, source patch + readback, authored "
               "blink, Minecraft portrait profiles, grouped moves, fine keyboard nudges, Ctrl+Z/Ctrl+Y, undo "
               "after save, separate 3D face geometry and localized NPC names, exit dialog: cancel, save, "
               "discard with invalid input, automatic face wizard, 1/2-pixel aperture presets, grid editing, "
               "generated NPC reload, clothing name color picker and shaded-eye detection "
               "regression, resize handles, region movement, numeric input, sensitivity and wizard "
               "undo/redo, upper/lower eyelid editing, hidden/restored panels, collapsible skin controls and "
               "current NPC face save.\nRenderer: "
            << (graphics.warp ? "WARP" : "hardware") << "\nNPC count: " << npcs.size() << "\n";
        smokeCloseChoice = 101;
        message(WM_CLOSE, 0, 0);
        require(!IsWindow(window), "Exit without saving did not close editor");
        std::ifstream profileFile(profilePath, std::ios::binary);
        std::string profileAfter((std::istreambuf_iterator<char>(profileFile)), {});
        require(profileAfter == profileSource, "Exit without saving modified portrait profile");
        return;
    }
    ++smokeStage;
}
} // namespace echo
