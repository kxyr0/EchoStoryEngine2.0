#include "npc/Repository.h"
#include "npc/GameExport.h"
#include "npc/Removal.h"
#include "graphics/MinecraftModel.h"
#include "ui/UI.h"
#include "portrait/FaceAuthoring.h"
#include <iostream>
#include <fstream>
using namespace echo;
static void require(bool value, const char *message) {
    if (!value)
        throw std::runtime_error(message);
}
int wmain(int argc, wchar_t **argv) {
    try {
        ComApartment apartment;
        {
            auto root =
                fs::temp_directory_path() / (L"Echo-remove-test-" + std::to_wstring(GetTickCount64()));
            fs::create_directories(root);
            Npc first, second;
            first.metadata = root / L"first.echo-npc";
            first.gameDefinition = root / L"first.npc.es";
            first.skin = std::make_shared<Skin>();
            first.skin->path = root / L"shared.png";
            second.skin = first.skin;
            for (auto path : {first.metadata, first.gameDefinition, first.skin->path})
                std::ofstream(path) << "original";
            auto files = npcRemovalFiles({first, second}, 0, true);
            require(files.size() == 2, "shared skin must survive full NPC deletion");
            auto archive = archiveNpcFiles(files, root / L"archive");
            require(!fs::exists(first.gameDefinition) && fs::exists(first.skin->path),
                    "NPC script removed but shared skin retained");
            std::ofstream(first.metadata) << "new content";
            bool conflict = false;
            try {
                restoreNpcFiles(archive);
            } catch (const std::exception &) {
                conflict = true;
            }
            require(conflict && !fs::exists(first.gameDefinition),
                    "restore collision must not partially restore files");
            fs::remove(first.metadata);
            restoreNpcFiles(archive);
            require(fs::exists(first.metadata) && fs::exists(first.gameDefinition),
                    "restore returns all removed files");
            std::ifstream restored(first.gameDefinition);
            std::string value;
            restored >> value;
            require(value == "original", "restored script content retained");
            restored.close();
            require(npcRemovalFiles({first}, 0, false).size() == 2,
                    "keep-assets removes only definition and metadata");
            require(npcRemovalFiles({first}, 0, true).size() == 3, "unshared skin included in full deletion");
            fs::remove_all(root);
        }

        std::string rigSource =
            "// keep header\nnamespace JDH.Animation;\nface Ben(\"jdh:ben\") { Eyes { Sclera Left { "
            "Position(/* keep inline */ -0.125, 0.06, 0); Future(42); } } Blink \"eye_closure\" { Travel(65, "
            "-61); PeriodTicks(60); CloseTicks(3); HoldTicks(3); OpenTicks(3); } }";
        auto rig = parseFaceRig(rigSource, {}, {});
        require(rig.parts.size() == 1 && rig.parts[0].position[0] == -.125 && rig.upperTravel == 65 &&
                    rig.period == 60,
                "Read actual face DSL conventions");
        rig.parts[0].texture = {2, 2, std::vector<uint8_t>(16, 255)};
        require(updatedFaceSource(rig) == rigSource, "Unchanged face rig byte preservation");
        rig.parts[0].position[0] = -.1;
        auto edited = updatedFaceSource(rig);
        require(edited.find("/* keep inline */ -0.1") != std::string::npos &&
                    edited.find("Future(42)") != std::string::npos,
                "Face patch preserves comments and unknown calls");
        require(parseFaceRig(edited, {}, {}).parts[0].position[0] == -.1, "Reparse edited face rig");
        Image plainFace{8, 8, std::vector<uint8_t>(8 * 8 * 4, 255)};
        auto composed = composeFace(plainFace, rig);
        require(composed.width == 256 && composed.height == 256, "Face composition size");
        require(blinkClosure(rig, 2.7) > .9, "Authored blink timing");

        auto profileText =
            std::string("PortraitOverlay(\"jdh:ben\", centerX: \"0.50\", facePixels: \"8\", layers: "
                        "[Layer(\"sclera_left\", placeX: /* retain */ \"-0.2\", placeY: 0.1, scaleX: 1, "
                        "scaleY: 1)], idle: [Blink \"eye_closure\" { Travel(63,-63); PeriodTicks(50); }]);");
        auto profile = parsePortraitProfile(profileText, {});
        require(updatedPortraitSource(profile) == profileText, "Unchanged profile preserves exact bytes");
        rig.profile = profile;
        auto q = faceQuads(rig).front();
        require(q.placeX == -.2 && q.placeY == .1 && std::abs(q.x - (-.025)) < 1e-8,
                "Profile place overrides rig placement");
        profile.layers.at("sclera_left").values["placeX"] = -.15;
        auto profileEdited = updatedPortraitSource(profile);
        require(profileEdited.find("/* retain */ \"-0.15\"") != std::string::npos,
                "Profile patch preserves quoted numbers and comments");
        require(parsePortraitProfile(profileEdited, {}).layers.at("sclera_left").values.at("placeX") == -.15,
                "Edited profile readback");
        profile.layers.at("sclera_left").values["visible"] = 0;
        require(parsePortraitProfile(updatedPortraitSource(profile), {})
                        .layers.at("sclera_left")
                        .values.at("visible") == 0,
                "Insert absent optional profile argument");
        rig.parts[0].kind = "Pupil";
        rig.parts[0].id = "pupil_left";
        rig.parts[0].area = {0, 0, 1, 2};
        q = faceQuads(rig).front();
        require(std::abs(q.x - (.5 + rig.parts[0].position[0] * 2)) < 1e-8,
                "Minecraft integer centering for odd-width pupil");
        require(validIdentifier(L"jdh:chapter1/ben"), "Valid identifier");
        require(!validIdentifier(L"JDH:ben") && !validIdentifier(L"jdh:") && !validIdentifier(L"a:b:c"),
                "Identifier validation");
        require(validColor(L"#DEC6B2") && !validColor(L"#ZZZZZZ"), "Color validation");
        require(validSkinSize(64, 64) && validSkinSize(128, 128) && !validSkinSize(64, 32) &&
                    !validSkinSize(65, 65),
                "Layout validation");
        Image image{64, 64, std::vector<uint8_t>(64 * 64 * 4)};
        for (unsigned y = 8; y < 16; ++y)
            for (unsigned x = 8; x < 16; ++x) {
                auto i = (y * 64 + x) * 4;
                image.rgba[i] = 200;
                image.rgba[i + 3] = 255;
                auto h = (y * 64 + x + 32) * 4;
                image.rgba[h + 2] = 200;
                image.rgba[h + 3] = 128;
            }
        auto face = makePortrait(image);
        require(face.width == 8 && face.rgba[0] == 100 && face.rgba[2] == 100 && face.rgba[3] == 255,
                "Face alpha compositing");
        require(makePortrait(image, false).rgba[0] == 200, "Hat disabled portrait");
        auto mesh = buildModel({}, false);
        require(mesh.size() == 216, "Six base cuboids");
        require(mesh[0].uv.x == 8.f / 64 && mesh[0].uv.y == 8.f / 64, "Head front UV");
        require(mesh[6].uv.x == 24.f / 64 && mesh[6].uv.y == 8.f / 64, "Head back UV");
        require(mesh[12].uv.x == 0 && mesh[12].uv.y == 8.f / 64, "Head right UV");
        require(mesh[18].uv.x == 16.f / 64 && mesh[18].uv.y == 8.f / 64, "Head left UV");
        require(mesh[24].uv.x == 8.f / 64 && mesh[24].uv.y == 0, "Head top UV");
        require(mesh[30].uv.x == 16.f / 64 && mesh[30].uv.y == 0, "Head bottom UV");
        const unsigned origins[6][6][2] = {{{8, 8}, {24, 8}, {0, 8}, {16, 8}, {8, 0}, {16, 0}},
                                           {{20, 20}, {32, 20}, {16, 20}, {28, 20}, {20, 16}, {28, 16}},
                                           {{44, 20}, {52, 20}, {40, 20}, {48, 20}, {44, 16}, {48, 16}},
                                           {{36, 52}, {44, 52}, {32, 52}, {40, 52}, {36, 48}, {40, 48}},
                                           {{4, 20}, {12, 20}, {0, 20}, {8, 20}, {4, 16}, {8, 16}},
                                           {{20, 52}, {28, 52}, {16, 52}, {24, 52}, {20, 48}, {24, 48}}};
        for (unsigned part = 0; part < 6; ++part)
            for (unsigned side = 0; side < 6; ++side) {
                auto uv = mesh[part * 36 + side * 6].uv;
                require(uv.x == origins[part][side][0] / 64.f && uv.y == origins[part][side][1] / 64.f,
                        "UV origins of all six sides of all six body parts");
            }
        require(mesh[0].position.x < mesh[1].position.x && mesh[6].position.x > mesh[7].position.x,
                "Front and back horizontal orientation");
        require(mesh[24].position.z > mesh[26].position.z, "Head top face front/back orientation");
        ModelSettings slim;
        slim.slim = true;
        mesh = buildModel(slim, false);
        require(mesh[72 + 1].position.x - mesh[72].position.x == 3, "Slim arm geometry");
        require(mesh[72 + 1].uv.x - mesh[72].uv.x == 3.f / 64, "Slim front UV width");
        ModelSettings overlays;
        overlays.hat = overlays.jacket = overlays.sleeves = overlays.pants = false;
        require(buildModel(overlays, true).empty(), "Overlay visibility");
        TextEdit edit;
        edit.set(L"Бен");
        edit.anchor = 0;
        edit.character(L'А');
        require(edit.value == L"А", "Text selection replacement");
        edit.history(false);
        require(edit.value == L"Бен", "Undo text");
        edit.history(true);
        require(edit.value == L"А", "Redo text");
        edit.caret = edit.anchor = edit.value.size();
        edit.character(0xd83d);
        edit.character(0xde00);
        require(edit.value.size() == 3, "UTF-16 supplementary input");
        edit.key(VK_BACK, false, false, nullptr);
        require(edit.value == L"А", "Backspace removes complete surrogate pair");
        edit.history(false);
        require(edit.value.size() == 3, "Emoji undo");
        edit.key(VK_LEFT, false, false, nullptr);
        require(edit.caret == 1, "Arrow skips surrogate pair");
        edit.key(VK_DELETE, false, false, nullptr);
        require(edit.value == L"А", "Delete removes complete surrogate pair");
        auto temp =
            fs::temp_directory_path() / (L"EchoNpcStudio-tests-" + std::to_wstring(GetCurrentProcessId()));
        fs::create_directory(temp);
        auto png = temp / L"тест.png";
        savePng(png, image);
        auto loaded = loadSkin(png);
        require(loaded->image.rgba == image.rgba, "WIC lossless PNG Unicode roundtrip");

        auto authoredSkin = std::make_shared<Skin>();
        authoredSkin->image = {64, 64, std::vector<uint8_t>(64 * 64 * 4)};
        for (size_t i = 0; i < authoredSkin->image.rgba.size(); i += 4) {
            authoredSkin->image.rgba[i] = 220;
            authoredSkin->image.rgba[i + 1] = 170;
            authoredSkin->image.rgba[i + 2] = 140;
            authoredSkin->image.rgba[i + 3] = 255;
        }
        for (int y = 5; y < 7; ++y)
            for (int x : {1, 2, 5, 6}) {
                auto *px = &authoredSkin->image.rgba[((y + 8) * 64 + x + 8) * 4];
                px[0] = (x == 2 || x == 5) ? 50 : 250;
                px[1] = (x == 2 || x == 5) ? 120 : 250;
                px[2] = (x == 2 || x == 5) ? 220 : 250;
            }
        for (int x : {1, 2, 5, 6}) {
            auto *px = &authoredSkin->image.rgba[(12 * 64 + x + 8) * 4];
            px[0] = px[1] = px[2] = 30;
        }
        authoredSkin->basePortrait = makePortrait(authoredSkin->image, false);
        authoredSkin->portrait = makePortrait(authoredSkin->image);
        auto draft = detectFaceDraft(authoredSkin);
        require(draft.crops[0].y == 5 && draft.crops[0].h == 2 && draft.brows,
                "Detect two-pixel eye and brow rectangles");
        auto shadedSkin = std::make_shared<Skin>(*authoredSkin);
        for (int y = 5; y < 7; ++y)
            for (int x : {1, 2, 5, 6}) {
                auto *px = &shadedSkin->image.rgba[((y + 8) * 64 + x + 8) * 4];
                bool pupil = x == 2 || x == 5;
                const uint8_t colors[2][2][3] = {{{229, 229, 229}, {139, 177, 191}},
                                                 {{255, 255, 255}, {181, 203, 212}}};
                std::copy_n(colors[y - 5][pupil ? 1 : 0], 3, px);
            }
        shadedSkin->basePortrait = makePortrait(shadedSkin->image, false);
        auto shadedDraft = detectFaceDraft(shadedSkin);
        require(shadedDraft.crops[0].y == 5 && shadedDraft.crops[0].h == 2 && shadedDraft.crops[4].y == 4,
                "Two shaded eye rows must not be mistaken for a single lower row");
        auto expanded = shadedDraft;
        expanded.aperture = 1;
        for (int i = 0; i < 4; ++i) {
            expanded.crops[i].y = 6;
            expanded.crops[i].h = 1;
        }
        setEyeHeight(expanded, 2);
        require(expanded.crops[0].y == 5 && expanded.crops[0].h == 2 && expanded.crops[2].h == 2,
                "Two-pixel preset expands a short crop toward the other actual eye row");
        auto singleSkin = std::make_shared<Skin>(*shadedSkin);
        for (int x : {1, 2, 5, 6}) {
            auto *px = &singleSkin->image.rgba[(14 * 64 + x + 8) * 4];
            px[0] = 220;
            px[1] = 170;
            px[2] = 140;
        }
        singleSkin->basePortrait = makePortrait(singleSkin->image, false);
        auto singleDraft = detectFaceDraft(singleSkin);
        require(singleDraft.crops[0].y == 5 && singleDraft.crops[0].h == 1,
                "A genuine one-row eye must not include adjacent skin or eyebrow");
        auto package = prepareFacePackage(draft, L"jdh:test_author", L"Новый NPC", L"#DEC6B2");
        require(package.textures.at(L"sclera_left").width == 2 &&
                    package.textures.at(L"pupil_left").height == 2,
                "Crop actual two-pixel eye dimensions");
        require(package.textures.at(L"sclera_left").rgba[4] == 250 &&
                    package.textures.at(L"pupil_left").rgba[0] == 50,
                "Separate pupil and reconstruct white under pupil");
        require(package.skin.rgba[(13 * 64 + 10) * 4] == 220 &&
                    authoredSkin->image.rgba[(13 * 64 + 10) * 4] == 50,
                "Cleaned skin removes eye and retains original");
        setEyeHeight(draft, 1);
        auto narrow = prepareFacePackage(draft, L"jdh:test_narrow", L"Один пиксель", L"#DEC6B2");
        require(parseFaceRig(narrow.rig, {}, {}).upperTravel == 31.25 &&
                    narrow.textures.at(L"pupil_left").height == 2,
                "Father-style aperture changes eyelids without losing second source row");
        draft.crops[0].h = draft.crops[1].h = draft.crops[2].h = draft.crops[3].h = 1;
        auto one = prepareFacePackage(draft, L"jdh:test_one", L"Один пиксель", L"#DEC6B2");
        require(one.textures.at(L"pupil_left").height == 1 && one.textures.at(L"sclera_left").height == 1,
                "True one-pixel source creates one-pixel textures");
        auto authorPack = temp / L"story" / L"content-pack" / L"jdh";
        fs::create_directories(authorPack / L"assets");
        fs::create_directories(authorPack / L"data");
        auto generated =
            writeFacePackage(draft, authorPack, L"jdh:test_one", L"Один пиксель", L"#DEC6B2", false);
        require(generated.faceRig && generated.faceRig->profile && fs::exists(generated.gameDefinition),
                "Write complete game NPC and animation package");
        for (const auto &part : generated.faceRig->parts) {
            auto expectedZ = part.kind == "Sclera"   ? 0.
                             : part.kind == "Pupil"  ? .01
                             : part.kind == "Eyelid" ? .02
                                                     : .03;
            require(part.position[2] == expectedZ, "Generated depths match working game face rigs");
        }
        auto depthRig = *generated.faceRig;
        depthRig.parts[2].position[2] = .001;
        require(setDefaultFaceDepths(depthRig) && depthRig.parts[2].position[2] == .01 && !depthRig.dirty,
                "Depth repair restores original authored defaults without a stale dirty flag");
        auto smallDepthRig = *generated.faceRig;
        for (auto &part : smallDepthRig.parts)
            part.position[2] = part.kind == "Pupil"    ? .00001
                               : part.kind == "Eyelid" ? .0001
                               : part.kind == "Brow"   ? .001
                                                       : 0.;
        auto stableGeometry = buildFaceLayers(smallDepthRig);
        auto depthOf = [&](const std::string &id) {
            return std::find_if(stableGeometry.begin(), stableGeometry.end(),
                                [&](const auto &layer) { return layer.id == id; })
                ->vertices[0]
                .position.z;
        };
        require(depthOf("pupil_left") < depthOf("sclera_left") &&
                    depthOf("upper_eyelid_left") < depthOf("pupil_left") &&
                    depthOf("lower_eyelid_left") < depthOf("upper_eyelid_left") &&
                    depthOf("brow_left") < depthOf("lower_eyelid_left"),
                "Manual Sasha/Father depths remain separate in native 3D preview");
        auto metadataRead = npcFromSkin(generated.skin);
        metadataRead.metadata = generated.metadata;
        readMetadata(metadataRead);
        require(metadataRead.name == L"Один пиксель" && metadataRead.skin->path == generated.skin->path,
                "Staged metadata skin path remains valid at final destination");
        bool duplicateRejected = false;
        try {
            writeFacePackage(draft, authorPack, L"jdh:test_one", L"Overwrite", L"#DEC6B2", false);
        } catch (...) {
            duplicateRejected = true;
        }
        require(duplicateRejected, "Generated assets must never overwrite existing resources");
        std::atomic_bool authorStop = false;
        auto rescan = scanProject(authorPack, authorStop);
        require(rescan.npcs.size() == 1 && rescan.diagnostics.empty() && rescan.npcs[0].faceRig,
                "Generated package discovered as one real NPC with face rig");
        auto rawPath =
            authorPack / L"assets" / L"jdh" / L"textures" / L"echo_npc" / L"skins" / L"test_current.png";
        savePng(rawPath, authoredSkin->image);
        auto rawNpc = npcFromSkin(loadSkin(rawPath));
        rawNpc.id = L"jdh:test_current";
        rawNpc.name = L"Текущий";
        rawNpc.hat = false;
        saveMetadata(rawNpc, rawNpc.metadata);
        auto existingDefinition = authorPack / L"data" / L"jdh" / L"echo_npc" / L"npcs" / L"current.npc.es";
        std::string originalDefinition =
            "class Existing : NpcResource { Id(\"jdh:test_current\"); Display(\"Current\"); "
            "Skin(\"jdh:test_current\"); DefaultForm(\"jdh:custom/form\"); }";
        {
            std::ofstream out(existingDefinition);
            out << originalDefinition;
        }
        rawNpc.gameDefinition = existingDefinition;
        auto currentDraft = detectFaceDraft(rawNpc.skin);
        auto editedCurrent = writeFacePackage(currentDraft, authorPack, rawNpc.id, rawNpc.name, rawNpc.color,
                                              rawNpc.slim, &rawNpc);
        std::ifstream preservedFile(existingDefinition);
        std::string preservedText((std::istreambuf_iterator<char>(preservedFile)), {});
        preservedFile.close();
        require(preservedText == originalDefinition && !editedCurrent.hat && editedCurrent.id == rawNpc.id,
                "Current NPC editing must preserve game definition, ID and skin flags");
        require(loadPng(authorPack / L"assets" / L"jdh" / L"textures" / L"echo_animation" / L"faces" /
                        L"test_current" / L"source_skin.png")
                        .rgba == authoredSkin->image.rgba,
                "Current NPC face editing preserves source backup");
        auto currentScan = scanProject(authorPack, authorStop);
        require(currentScan.npcs.size() == 2, "Current face save must not create a duplicate NPC");
        editedCurrent.name = L"Незнакомец \"гость\"";
        editedCurrent.color = L"#112233";
        editedCurrent.faceRig->parts[6].position[0] += .125;
        editedCurrent.faceRig->dirty = true;
        auto &exportProfile = *editedCurrent.faceRig->profile;
        exportProfile.layers.at("brow_left").values["placeX"] = .375;
        exportProfile.dirty = true;
        auto expectedRig = updatedFaceSource(*editedCurrent.faceRig);
        auto expectedProfile = updatedPortraitSource(exportProfile);
        saveGameBundle(editedCurrent, authorPack);
        auto calls = scriptStringCalls(editedCurrent.gameSource);
        require(calls.at("Display") == utf8(editedCurrent.name) && calls.at("NameColor") == "#112233" &&
                    calls.at("DefaultForm") == "jdh:custom/form",
                "Game export changes name and color while retaining custom NPC fields");
        require(editedCurrent.faceRig->source == expectedRig &&
                    editedCurrent.faceRig->profile->source == expectedProfile && !editedCurrent.dirty &&
                    !editedCurrent.faceRig->dirty && !editedCurrent.faceRig->profile->dirty,
                "Game export saves brow rig and portrait edits together");
        auto savedScan = scanProject(authorPack, authorStop);
        auto savedCurrent = std::find_if(savedScan.npcs.begin(), savedScan.npcs.end(),
                                         [](const auto &n) { return n.id == L"jdh:test_current"; });
        require(savedScan.diagnostics.empty() && savedCurrent != savedScan.npcs.end() &&
                    savedCurrent->name == editedCurrent.name && savedCurrent->color == editedCurrent.color &&
                    savedCurrent->faceRig->profile->layers.at("brow_left").values.at("placeX") == .375,
                "Full game bundle is discoverable after reload");
        auto readBytes = [](const fs::path &p) {
            std::ifstream in(p, std::ios::binary);
            return std::string(std::istreambuf_iterator<char>(in), {});
        };
        auto beforeRig = readBytes(editedCurrent.faceRig->path);
        {
            std::ofstream out(editedCurrent.gameDefinition, std::ios::binary | std::ios::app);
            out << "// external edit\n";
        }
        bool externalRejected = false;
        try {
            saveGameBundle(editedCurrent, authorPack);
        } catch (...) {
            externalRejected = true;
        }
        require(externalRejected && readBytes(editedCurrent.faceRig->path) == beforeRig,
                "External script changes cancel bundle before any writes");
        auto originalMetadata = generated.metadata;
        auto blockedMetadata = authorPack / L"blocked.echo-npc";
        fs::create_directory(blockedMetadata);
        generated.metadata = blockedMetadata;
        generated.name = L"Не должно сохраниться";
        auto originalNpcBytes = readBytes(generated.gameDefinition);
        auto originalRigBytes = readBytes(generated.faceRig->path);
        auto originalProfileBytes = readBytes(generated.faceRig->profile->path);
        auto originalSkinBytes = readBytes(generated.skin->path);
        bool transactionRejected = false;
        try {
            saveGameBundle(generated, authorPack);
        } catch (...) {
            transactionRejected = true;
        }
        require(transactionRejected && readBytes(generated.gameDefinition) == originalNpcBytes &&
                    readBytes(generated.faceRig->path) == originalRigBytes &&
                    readBytes(generated.faceRig->profile->path) == originalProfileBytes &&
                    readBytes(generated.skin->path) == originalSkinBytes,
                "Failed bundle write restores NPC, rig, profile and original PNG bytes");
        generated.metadata = originalMetadata;
        fs::remove(blockedMetadata);
        generated.gameSource = patchScriptStrings(generated.gameSource, {{"NameColor", "#AABBCC77"}});
        {
            std::ofstream out(generated.gameDefinition, std::ios::binary | std::ios::trunc);
            out << generated.gameSource;
        }
        generated.color = L"#112233";
        saveGameBundle(generated, authorPack);
        require(scriptStringCalls(generated.gameSource).at("NameColor") == "#11223377",
                "Game export retains original alpha channel");
        require(readBytes(generated.skin->path) == originalSkinBytes,
                "Full export keeps original skin PNG bytes");
        auto alias =
            patchScriptStrings("class Test : NpcResource { display(\"Old\"); DefaultForm(\"jdh:form\"); }",
                               {{"Display", "New"}, {"NameColor", "#112233"}});
        require(alias.find("display(\"New\")") != std::string::npos &&
                    alias.find("DefaultForm(\"jdh:form\")") != std::string::npos,
                "Token patch preserves lowercase calls and custom fields");
        bool expressionRejected = false;
        try {
            patchScriptStrings("class Test : NpcResource { Display(Name()); }", {{"Display", "New"}});
        } catch (...) {
            expressionRejected = true;
        }
        require(expressionRejected, "Unsupported NPC expressions must not generate duplicate fields");
        std::vector<fs::path> generatedEntries;
        for (auto &entry : fs::recursive_directory_iterator(temp / L"story"))
            generatedEntries.push_back(entry.path());
        std::sort(generatedEntries.begin(), generatedEntries.end(),
                  [](auto &a, auto &b) { return a.native().size() > b.native().size(); });
        for (auto &entry : generatedEntries)
            fs::remove(entry);
        fs::remove(temp / L"story");
        auto npc = npcFromSkin(loaded);
        npc.id = L"jdh:ben";
        npc.name = L"Бен";
        npc.extra["future-property"] = "preserved";
        auto path = temp / L"Бен.echo-npc";
        saveMetadata(npc, path);
        auto copy = npcFromSkin(loaded);
        copy.metadata = path;
        readMetadata(copy);
        require(copy.name == L"Бен" && copy.id == L"jdh:ben" && copy.extra["future-property"] == "preserved",
                "Metadata Unicode and unknown fields roundtrip");
        require(!npc.dirty, "Save clears dirty state");
        auto duplicate = npc;
        duplicate.id = L"jdh:ben_copy";
        auto copyPath = temp / L"copy.echo-npc";
        saveMetadata(duplicate, copyPath);
        std::atomic_bool stop = false;
        auto scan = scanProject(temp, stop);
        require(scan.npcs.size() == 2 && scan.diagnostics.empty(),
                "Discover multiple NPC definitions sharing a skin");
        require(scan.npcs[0].skin == scan.npcs[1].skin, "Shared CPU skin cache");
        fs::remove(copyPath);
        if (argc > 1) {
            auto real = loadSkin(fs::path(argv[1]));
            require(real->portrait.width == real->image.width / 8, "Real skin portrait");
            std::cout << "Real PNG: " << real->image.width << "x" << real->image.height << "\n";
            std::vector<std::wstring> diagnostics;
            auto faces = loadFaceCatalog(fs::path(argv[1]), diagnostics);
            require(diagnostics.empty(), "Real face catalog diagnostics");
            require(faces.size() >= 8 && faces.contains(L"jdh:ben"),
                    "Load existing face rigs alongside newly authored NPCs");
            require(faces.at(L"jdh:ben")->parts.size() == 8 && faces.at(L"jdh:molly")->parts.size() == 9,
                    "Load eyes, brows, eyelids and glasses");
            require(faces.at(L"jdh:ben")->parts[0].texture.width == 2, "Small face texture decoding");
            auto faceSource = faces.at(L"jdh:ben")->source;
            require(updatedFaceSource(*faces.at(L"jdh:ben")) == faceSource,
                    "Real face source preserved without changes");

            auto &ben = *faces.at(L"jdh:ben");
            require(ben.profile &&
                        ben.profile->path.parent_path().parent_path().filename() == L"echo_animation",
                    "Canonical portrait profile precedence over copied folder");
            require(ben.profile->layers.size() == 11 && faceQuads(ben).size() == 10,
                    "Per-eye split eyelid overrides loaded");
            require(faceQuads(ben)[2].placeX == -.125 && faceQuads(ben)[2].placeY == .06,
                    "Real pupil placement must use portrait profile");
            require(updatedPortraitSource(*ben.profile) == ben.profile->source,
                    "Real profile byte preservation");

            auto world = worldFaceQuads(ben);
            auto pupil = world[2], sclera = world[0];
            require(pupil.x >= sclera.x && pupil.y >= sclera.y &&
                        pupil.x + pupil.w <= sclera.x + sclera.w + 1e-8 &&
                        pupil.y + pupil.h <= sclera.y + sclera.h + 1e-8,
                    "World pupil must fit within its authored sclera");
            require(std::abs(pupil.y - sclera.y) < 1e-8 &&
                        std::abs(pupil.y - faceQuads(ben, 0, 256, false)[2].y) > .01,
                    "World projector must clamp unaligned authored pupil");
            auto geometry = buildFaceLayers(ben);
            require(geometry.size() == 10 && geometry[0].vertices[0].position.z < -4 &&
                        geometry[0].vertices[0].position.z > -4.5f,
                    "Separate face geometry between head and hat surfaces");
            auto gameScan = scanProject(fs::path(argv[1]).parent_path(), stop);
            require(gameScan.diagnostics.empty(), "Game NPC defaults import diagnostics");
            auto benNpc = std::find_if(gameScan.npcs.begin(), gameScan.npcs.end(),
                                       [](auto &n) { return n.id == L"jdh:ben"; });
            auto mollyNpc = std::find_if(gameScan.npcs.begin(), gameScan.npcs.end(),
                                         [](auto &n) { return n.id == L"jdh:molly"; });
            require(benNpc != gameScan.npcs.end() && benNpc->name == L"Бен" &&
                        mollyNpc != gameScan.npcs.end() && mollyNpc->name == L"Молли",
                    "Localized names from actual NPC definitions");
            require(!benNpc->gameDefinition.empty() && skinForPreview(*benNpc) == benNpc->skin,
                    "GPU preview retains original skin without baking");
            std::cout << "Real face rigs: " << faces.size() << "\n";
        }
        fs::remove(path);
        fs::remove(png);
        fs::remove(temp);
        std::cout << "All core tests passed.\n";
        return 0;
    } catch (const std::exception &e) {
        std::cerr << e.what() << "\n";
        return 1;
    }
}
