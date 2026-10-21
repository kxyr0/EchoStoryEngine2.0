#include "app/Application.h"
#include <shellapi.h>
#include <shobjidl.h>
#include <shlobj.h>
#include <windowsx.h>
#include <commdlg.h>
#include <fstream>
#include <sstream>
#include <commctrl.h>
#include <set>
namespace echo {
static constexpr UINT scanTimer = 1, animationTimer = 2;
Application::~Application() {
    stop = true;
    if (scan.valid())
        scan.wait();
    if (imageTask.valid())
        imageTask.wait();
}
bool Application::unsaved() const {
    return (!portraitSnapshot.empty() && portraitFields() != portraitSnapshot) ||
           std::any_of(npcs.begin(), npcs.end(), [](const Npc &n) {
               return n.dirty ||
                      (n.faceRig && (n.faceRig->dirty || (n.faceRig->profile && n.faceRig->profile->dirty)));
           });
}
bool Application::discard() {
    sync();
    return !unsaved() || messageBox(window, tr("ui.application.discard_unsaved_npc_changes"),
                                    tr("ui.application.echo_npc_studio"), MB_YESNO | MB_ICONWARNING) == IDYES;
}
void Application::saveAllChanges() {
    sync();
    applyPortraitPart();
    for (size_t i = 0; i < npcs.size(); ++i)
        if (npcs[i].dirty &&
            (!validIdentifier(npcs[i].id) || !validColor(npcs[i].color) || duplicateId(npcs[i].id, int(i))))
            throw std::runtime_error(
                tr8("error.application.fix_invalid_or_duplicate_npc_values_before_saving"));
    std::set<FaceRig *> saved;
    for (auto &npc : npcs) {
        if (npc.faceRig && saved.insert(npc.faceRig.get()).second) {
            auto &rig = *npc.faceRig;
            if (rig.dirty)
                saveFaceRig(rig, rig.path);
            if (rig.profile && rig.profile->dirty)
                savePortraitProfile(*rig.profile, rig.profile->path);
        }
        if (npc.dirty)
            saveMetadata(npc, npc.metadata);
    }
    refreshPortraitFields();
}
bool Application::confirmClose() {
    // Discard must remain possible even if a numeric field contains invalid input.
    sync();
    if (!unsaved())
        return true;
    const TASKDIALOG_BUTTON buttons[] = {
        {100, tr("ui.application.save_and_exit_save_npc_and_portrait_changes").c_str()},
        {101, tr("ui.application.exit_without_saving_discard_all_changes_from_this_session").c_str()},
        {102, tr("ui.application.cancel_return_to_the_editor").c_str()}};
    TASKDIALOGCONFIG config{};
    config.cbSize = sizeof(config);
    config.hwndParent = window;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_ALLOW_DIALOG_CANCELLATION | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = tr("ui.application.echo_npc_studio").c_str();
    config.pszMainInstruction = tr("ui.application.there_are_unsaved_changes").c_str();
    config.pszContent = tr("ui.application.choose_what_to_do_before_closing_the_editor").c_str();
    config.pszMainIcon = TD_WARNING_ICON;
    config.cButtons = UINT(std::size(buttons));
    config.pButtons = buttons;
    config.nDefaultButton = 102;
    if (!smokeFolder.empty() && smokeCloseChoice) {
        config.lpCallbackData = smokeCloseChoice;
        config.pfCallback = [](HWND dialog, UINT notification, WPARAM, LPARAM, LONG_PTR choice) -> HRESULT {
            if (notification == TDN_CREATED)
                PostMessageW(dialog, TDM_CLICK_BUTTON, WPARAM(choice), 0);
            return S_OK;
        };
    }
    int choice = 102;
    check(TaskDialogIndirect(&config, &choice, nullptr, nullptr), tr8("error.application.exit_dialog"));
    if (choice == 101)
        return true;
    if (choice == 100) {
        saveAllChanges();
        return !unsaved();
    }
    return false;
}
bool Application::duplicateId(const std::wstring &id, int except) const {
    for (size_t i = 0; i < npcs.size(); ++i)
        if (int(i) != except && npcs[i].id == id)
            return true;
    return false;
}
void Application::report(const std::exception &e) {
    auto message = wide(e.what());
    diagnostics.push_back(message);
    status = message;
    if (!smokeFolder.empty()) {
        smokeFailed = true;
        std::ofstream(smokeFolder / L"failure.txt") << e.what();
        PostQuitMessage(1);
        return;
    }
    messageBox(window, message.c_str(), tr("ui.application.echo_npc_studio_error"), MB_OK | MB_ICONERROR);
    InvalidateRect(window, nullptr, FALSE);
}
void Application::layout() {
    RECT r;
    GetClientRect(window, &r);
    scale = GetDpiForWindow(window) / 96.f;
    width = r.right / scale;
    height = r.bottom / scale;
    left = showProperties ? std::clamp(left, 240.f, width * .34f) : 0;
    right = showLibrary ? std::clamp(right, 240.f, width * .34f) : 0;
    bottom = showBottom ? std::clamp(bottom, 160.f, height - 360) : 28;
}
fs::path Application::fileDialog(bool saveFile, const std::wstring &filter, const wchar_t *extension) {
    wchar_t buffer[32768]{};
    OPENFILENAMEW o{};
    o.lStructSize = sizeof(o);
    o.hwndOwner = window;
    o.lpstrFilter = filter.c_str();
    o.lpstrFile = buffer;
    o.nMaxFile = 32768;
    o.lpstrDefExt = extension;
    o.Flags = OFN_EXPLORER | OFN_NOCHANGEDIR | OFN_PATHMUSTEXIST |
              (saveFile ? OFN_OVERWRITEPROMPT : OFN_FILEMUSTEXIST);
    if (saveFile ? GetSaveFileNameW(&o) : GetOpenFileNameW(&o))
        return fs::path(buffer);
    return {};
}
fs::path Application::folderDialog() {
    ComPtr<IFileOpenDialog> dialog;
    check(CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dialog)),
          tr8("error.application.folder_dialog"));
    DWORD options;
    check(dialog->GetOptions(&options), tr8("error.application.folder_dialog_options"));
    check(dialog->SetOptions(options | FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM),
          tr8("error.application.folder_selection"));
    auto hr = dialog->Show(window);
    if (hr == HRESULT_FROM_WIN32(ERROR_CANCELLED))
        return {};
    check(hr, tr8("error.application.folder_dialog"));
    ComPtr<IShellItem> item;
    check(dialog->GetResult(&item), tr8("error.application.folder_result"));
    PWSTR text = nullptr;
    check(item->GetDisplayName(SIGDN_FILESYSPATH, &text), tr8("error.application.folder_path"));
    fs::path path(text);
    CoTaskMemFree(text);
    return path;
}
void Application::reveal(const fs::path &path) {
    if (path.empty())
        return;
    if (fs::is_directory(path))
        ShellExecuteW(window, L"open", path.c_str(), nullptr, nullptr, SW_SHOWNORMAL);
    else {
        auto args = L"/select,\"" + path.wstring() + L"\"";
        ShellExecuteW(window, L"open", L"explorer.exe", args.c_str(), nullptr, SW_SHOWNORMAL);
    }
}
void Application::clipboard(const std::wstring &text) {
    TextEdit edit;
    edit.set(text);
    edit.anchor = 0;
    edit.copy(window);
    status = tr("ui.application.copied_to_clipboard");
}
void Application::sync() {
    if (selected < 0 || selected >= int(npcs.size()))
        return;
    auto &n = npcs[selected];
    if (identifier.value != n.id || display.value != n.name || color.value != n.color) {
        n.id = identifier.value;
        n.name = display.value;
        n.color = color.value;
        n.dirty = true;
    }
}
void Application::select(int index) {
    if (portraitPart >= 0 && !portraitSnapshot.empty())
        applyPortraitPart();
    portraitPart = -1;
    portraitSnapshot.clear();
    portraitScroll = 0;
    portraitClosure = 0;
    sync();
    selected = index;
    ui.clearFocus();
    graphics.invalidateImages();
    if (index < 0 || index >= int(npcs.size())) {
        graphics.setSkin(nullptr);
        return;
    }
    auto &n = npcs[index];
    identifier.set(n.id);
    display.set(n.name);
    color.set(n.color);
    skinPath.set(n.skin->path.wstring());
    graphics.setSkin(skinForPreview(n));
    if (!n.faceRig) {
        auto catalog = loadFaceCatalog(n.skin->path, diagnostics);
        auto it = catalog.find(n.id);
        if (it != catalog.end())
            n.faceRig = it->second;
        else if (n.skin->path.stem() == L"player" && catalog.contains(L"echo_content_player:player/base"))
            n.faceRig = catalog[L"echo_content_player:player/base"];
    }
    rebuildPortrait();
    if (n.faceRig) {
        selectPortraitPart(0);
        graphics.setSkin(skinForPreview(n));
    }
    contextIndex = -1;
    status = n.skin->path.filename().wstring() + L" — " + std::to_wstring(n.skin->image.width) + L"×" +
             std::to_wstring(n.skin->image.height);
}
void Application::updateSkin(std::shared_ptr<Skin> skin) {
    if (selected < 0) {
        npcs.push_back(npcFromSkin(std::move(skin)));
        npcs.back().dirty = true;
        select(int(npcs.size()) - 1);
    } else {
        auto &n = npcs[selected];
        n.skin = std::move(skin);
        n.dirty = true;
        skinPath.set(n.skin->path.wstring());
        graphics.invalidateImages();
        graphics.setSkin(skinForPreview(n));
        rebuildPortrait();
    }
    status = tr("ui.application.skin_applied_portrait_and_library_icon_updated");
}
void Application::openProject(const fs::path &root) {
    if ((root.filename() == L"faces" || root.filename() == L"echo_animation") && !root.empty()) {
        auto pack = findContentPack(root);
        auto skinFolder = pack / L"assets" / L"jdh" / L"textures" / L"echo_npc" / L"skins";
        if (!pack.empty() && fs::is_directory(skinFolder)) {
            openProject(skinFolder);
            return;
        }
    }
    if (scanning) {
        status = tr("ui.application.a_scan_is_already_running");
        return;
    }
    if (root.empty() || !discard())
        return;
    project = root;
    stop = false;
    scanning = true;
    status = tr("ui.application.scanning_png_skins_recursively");
    ui.clearFocus();
    selected = -1;
    graphics.setSkin(nullptr);
    graphics.invalidateImages();
    npcs.clear();
    portraitHistories.clear();
    portraitPart = -1;
    portraitSnapshot.clear();
    portraitComposite = {};
    scan = std::async(std::launch::async, [this, root] {
        ComApartment apartment(COINIT_MULTITHREADED);
        return scanProject(root, stop);
    });
    SetTimer(window, scanTimer, 75, nullptr);
}
void Application::pollScan() {
    if (!scanning || scan.wait_for(std::chrono::seconds(0)) != std::future_status::ready)
        return;
    scanning = false;
    KillTimer(window, scanTimer);
    try {
        auto result = scan.get();
        ui.clearFocus();
        graphics.setSkin(nullptr);
        graphics.invalidateImages();
        npcs = std::move(result.npcs);
        diagnostics = std::move(result.diagnostics);
        diagnostics.insert(diagnostics.end(), localizationIssues.begin(), localizationIssues.end());
        selected = -1;
        libraryScroll = 0;
        if (!npcs.empty())
            select(0);
        status = trf("status.scan_complete", {{L"count", std::to_wstring(npcs.size())},
                                              {L"diagnostics", std::to_wstring(diagnostics.size())}});
    } catch (const std::exception &e) {
        report(e);
    }
    InvalidateRect(window, nullptr, FALSE);
}
void Application::importSkin(bool replace) {
    auto path = fileDialog(false, tr("ui.application.minecraft_png_skin_png"));
    if (!path.empty())
        loadSkinAsync(path, replace);
}
void Application::loadSkinAsync(const fs::path &path, bool replace) {
    if (imageLoading || scanning)
        return;
    imageReplace = replace;
    imageTarget = selected;
    imageTask = std::async(std::launch::async, [path] {
        ComApartment apartment(COINIT_MULTITHREADED);
        return loadSkin(path);
    });
    imageLoading = true;
    status = tr("ui.application.loading_png_and_generating_portrait");
    SetTimer(window, 5, 50, nullptr);
}
void Application::pollImage() {
    if (!imageLoading || imageTask.wait_for(std::chrono::seconds(0)) != std::future_status::ready)
        return;
    imageLoading = false;
    KillTimer(window, 5);
    auto skin = imageTask.get();
    if (imageReplace) {
        if (imageTarget >= 0)
            select(imageTarget);
        updateSkin(std::move(skin));
    } else {
        auto n = npcFromSkin(std::move(skin));
        readMetadata(n);
        n.dirty = !fs::exists(n.metadata);
        npcs.push_back(std::move(n));
        select(int(npcs.size()) - 1);
    }
    InvalidateRect(window, nullptr, FALSE);
}
void Application::save(bool saveAs) {
    if (leftTab == 1) {
        savePortraitRig(saveAs);
        return;
    }
    sync();
    if (selected < 0)
        return;
    auto &n = npcs[selected];
    if (duplicateId(n.id, selected))
        throw std::runtime_error(
            tr8("error.application.duplicate_npc_identifier_change_identifier_before_saving"));
    auto destination = n.metadata;
    if (saveAs || destination.empty())
        destination = fileDialog(true, tr("ui.application.echo_npc_studio_metadata_echo_npc"), L"echo-npc");
    if (destination.empty())
        return;
    if (!saveAs && fs::exists(destination) &&
        messageBox(window, (tr("ui.application.overwrite_metadata") + destination.wstring()).c_str(),
                   tr("ui.application.save_npc"), MB_YESNO | MB_ICONQUESTION) != IDYES)
        return;
    saveMetadata(n, destination);
    status = tr("ui.application.saved") + destination.wstring();
}
void Application::createNpc() {
    if (!validIdentifier(newId.value))
        throw std::runtime_error(
            tr8("error.application.identifier_must_use_namespace_path_with_lowercase_ascii_letters_numbers"));
    if (duplicateId(newId.value))
        throw std::runtime_error(tr8("error.application.npc_identifier_already_exists"));
    if (newName.value.empty())
        throw std::runtime_error(tr8("error.application.display_name_is_required"));
    auto destination = fs::path(newFolder.value);
    if (!fs::is_directory(destination))
        throw std::runtime_error(tr8("error.application.destination_folder_must_already_exist"));
    auto skin = loadSkin(fs::path(newSkin.value));
    auto n = npcFromSkin(std::move(skin));
    n.id = newId.value;
    n.name = newName.value;
    n.color = newNpcColor;
    n.slim = newSlim;
    auto slug = n.id.substr(n.id.find(L':') + 1);
    std::replace(slug.begin(), slug.end(), L'/', L'_');
    n.metadata = destination / (slug + L".echo-npc");
    if (fs::exists(n.metadata) &&
        messageBox(window, (tr("ui.application.overwrite_metadata") + n.metadata.wstring()).c_str(),
                   tr("ui.application.create_npc"), MB_YESNO | MB_ICONWARNING) != IDYES)
        return;
    saveMetadata(n, n.metadata);
    npcs.push_back(std::move(n));
    createDialog = false;
    select(int(npcs.size()) - 1);
    status = tr("ui.application.npc_created_original_skin_linked_without_changing_it");
}
void Application::removeNpc(int index) {
    if (index < 0 || index >= int(npcs.size()) || scanning)
        return;
    sync();
    const auto title = tr("ui.removal.title");
    const auto keep = tr("ui.removal.keep");
    const auto all = tr("ui.removal.all");
    const auto cancel = tr("ui.removal.cancel");
    const TASKDIALOG_BUTTON buttons[] = {{100, keep.c_str()}, {101, all.c_str()}, {102, cancel.c_str()}};
    TASKDIALOGCONFIG config{};
    config.cbSize = sizeof(config);
    config.hwndParent = window;
    config.dwFlags = TDF_USE_COMMAND_LINKS | TDF_ALLOW_DIALOG_CANCELLATION | TDF_POSITION_RELATIVE_TO_WINDOW;
    config.pszWindowTitle = title.c_str();
    config.pszMainInstruction = npcs[index].id.c_str();
    config.pszContent = tr("ui.removal.explain").c_str();
    config.pszMainIcon = TD_WARNING_ICON;
    config.cButtons = UINT(std::size(buttons));
    config.pButtons = buttons;
    config.nDefaultButton = 102;
    int choice = 102;
    check(TaskDialogIndirect(&config, &choice, nullptr, nullptr), tr8("error.application.exit_dialog"));
    if (choice != 100 && choice != 101)
        return;
    auto files = npcRemovalFiles(npcs, size_t(index), choice == 101);
    std::wstring preview = tr("ui.removal.files") + L"\n";
    for (const auto &file : files)
        preview += file.wstring() + L"\n";
    if (files.empty()) {
        status = tr("ui.removal.empty");
        return;
    }
    if (messageBox(window, preview, title, MB_OKCANCEL | MB_ICONWARNING) != IDOK)
        return;
    PWSTR localPath = nullptr;
    check(SHGetKnownFolderPath(FOLDERID_LocalAppData, 0, nullptr, &localPath), tr8("error.removal.archive"));
    auto archiveRoot = fs::path(localPath) / L"EchoNpcStudio" / L"DeletedNPCs";
    CoTaskMemFree(localPath);
    lastRemoval = archiveNpcFiles(files, archiveRoot);
    ui.clearFocus();
    graphics.invalidateImages();
    graphics.setSkin(nullptr);
    npcs.erase(npcs.begin() + index);
    selected = -1;
    select(npcs.empty() ? -1 : std::min(index, int(npcs.size()) - 1));
    contextIndex = -1;
    status = tr("ui.removal.done") + lastRemoval->folder.wstring();
}
void Application::restoreRemovedNpc() {
    if (!lastRemoval || scanning)
        return;
    restoreNpcFiles(*lastRemoval);
    lastRemoval.reset();
    openProject(project);
    status = tr("ui.removal.restored");
}
void Application::duplicateNpc(int index) {
    sync();
    auto n = npcs.at(index);
    auto original = n.id;
    int suffix = 1;
    do {
        n.id = original + L"_copy" + std::to_wstring(suffix++);
    } while (duplicateId(n.id));
    n.name += tr("ui.application.copy");
    auto slug = n.id.substr(n.id.find(L':') + 1);
    std::replace(slug.begin(), slug.end(), L'/', L'_');
    n.metadata = n.skin->path.parent_path() / (slug + L".echo-npc");
    n.dirty = true;
    npcs.push_back(std::move(n));
    select(int(npcs.size()) - 1);
}
void Application::exportPortrait() {
    if (selected < 0)
        return;
    auto path = fileDialog(true, tr("ui.application.png_image_png"), L"png");
    if (path.empty())
        return;
    for (auto &n : npcs)
        if (fs::exists(path) && fs::equivalent(path, n.skin->path))
            throw std::runtime_error(
                tr8("error.application.portrait_export_cannot_replace_an_original_skin"));
    auto portrait = makePortrait(npcs[selected].skin->image, npcs[selected].hat);
    savePng(path, portrait);
    status = tr("ui.application.portrait_png_exported");
}
void Application::openEyes() {
    if (selected < 0)
        return;
    face = makePortrait(npcs[selected].skin->image, npcs[selected].hat);
    if (face.width > 128)
        throw std::runtime_error(
            tr8("error.application.face_eye_editor_supports_skins_up_to_1024x1024_in_this"));
    eyes = detectEyes(face);
    eyeSide = 0;
    eyeDialog = true;
    graphics.invalidateImages();
    ui.clearFocus();
}
void Application::exportEyes() {
    auto folder = folderDialog();
    if (folder.empty())
        return;
    auto leftPath = folder / L"left-eye.png", rightPath = folder / L"right-eye.png";
    for (auto &n : npcs)
        for (auto &path : {leftPath, rightPath})
            if (fs::exists(path) && fs::equivalent(path, n.skin->path))
                throw std::runtime_error(
                    tr8("error.application.eye_mask_export_cannot_replace_an_original_skin"));
    if ((fs::exists(leftPath) || fs::exists(rightPath)) &&
        messageBox(window, tr("ui.application.overwrite_existing_eye_masks"),
                   tr("ui.application.export_masks"), MB_YESNO | MB_ICONWARNING) != IDYES)
        return;
    savePng(leftPath, maskImage(face, eyes.left));
    savePng(rightPath, maskImage(face, eyes.right));
    status = tr("ui.application.transparent_eye_masks_exported") + folder.wstring();
}
void Application::eyePaint(float x, float y, bool first) {
    float bx = (width - 610) / 2 + 30, by = (height - 540) / 2 + 90;
    float cell = 320.f / face.width;
    if (x < bx || y < by || x >= bx + 320 || y >= by + 320)
        return;
    unsigned px = std::min(face.width - 1, unsigned((x - bx) / cell)),
             py = std::min(face.height - 1, unsigned((y - by) / cell));
    auto &mask = eyeSide == 0 ? eyes.left : eyes.right;
    if (first) {
        paintValue = mask[py * face.width + px] ? 0 : 1;
        eyeX = px;
        eyeY = py;
        painting = true;
        SetCapture(window);
    } // interpolate drag strokes
    int steps = std::max(std::abs(int(px) - int(eyeX)), std::abs(int(py) - int(eyeY)));
    for (int i = 0; i <= steps; ++i) {
        unsigned xx = steps ? unsigned(std::lround(eyeX + (int(px) - int(eyeX)) * float(i) / steps)) : px,
                 yy = steps ? unsigned(std::lround(eyeY + (int(py) - int(eyeY)) * float(i) / steps)) : py;
        mask[yy * face.width + xx] = paintValue;
    }
    eyeX = px;
    eyeY = py;
}
void Application::draw() {
    sync();
    layout();
    graphics.begin();
    float centerRight = width - right, end = height - bottom;
    if (selected >= 0 && leftTab == 0) {
        auto &n = npcs[selected];
        ModelSettings s{n.slim, n.hat, n.jacket, n.sleeves, n.pants, 0};
        if (idle)
            s.animation =
                std::chrono::duration<float>(std::chrono::steady_clock::now() - start).count() * 1.5f;
        graphics.model(rect(left + 6, 112, centerRight - left - 12, end - 120), s, yaw, pitch, distance, head,
                       n.faceRig.get());
    }
    graphics.beginUi();
    ui.begin();
    toolbar();
    if (showProperties)
        properties();
    if (showLibrary)
        library();
    if (showBottom)
        bottomPanel();
    if (leftTab == 1)
        portraitPreview();
    else {
        float x = left + 16;
        const std::wstring titles[] = {tr("ui.application.front"), tr("ui.application.back"),
                                       tr("ui.application.left"),  tr("ui.application.right"),
                                       tr("ui.application.head"),  tr("ui.application.reset")};
        for (int i = 0; i < 6; ++i) {
            float buttonWidth = std::max(55.f, graphics.textWidth(titles[i]) + 20);
            ui.button(
                50 + i, titles[i], rect(x, 72, buttonWidth, 30),
                [this, i] {
                    head = i == 4;
                    pitch = .05f;
                    switch (i) {
                    case 0:
                        yaw = 0;
                        break;
                    case 1:
                        yaw = 3.14159265f;
                        break;
                    case 2:
                        yaw = -1.5707963f;
                        break;
                    case 3:
                        yaw = 1.5707963f;
                        break;
                    case 4:
                        yaw = 0;
                        break;
                    case 5:
                        yaw = 0;
                        distance = 60;
                        break;
                    }
                },
                selected >= 0);
            x += buttonWidth + 6;
        }
        float idleWidth = std::max(82.f, graphics.textWidth(tr("ui.application.idle")) + 48);
        ui.toggle(56, tr("ui.application.idle"), rect(left + 16, end - 42, idleWidth, 30), idle, [this] {
            idle = !idle;
            if (idle)
                SetTimer(window, animationTimer, 16, nullptr);
            else
                KillTimer(window, animationTimer);
        });
        if (selected < 0) {
            graphics.text(tr("ui.application.minecraft_3d_preview"),
                          rect(left + 40, 190, centerRight - left - 80, 40), 0xE6E8EC, 1);
            graphics.text(tr("ui.application.open_a_skin_folder_or_import_a_png_drag_a"),
                          rect(left + 40, 242, centerRight - left - 80, 85), 0x969BA6);
        }
    }
    graphics.line(left, 58, left, end, 0x343A46, 3);
    graphics.line(centerRight, 58, centerRight, end, 0x343A46, 3);
    graphics.line(0, end, width, end, 0x343A46, 3);
    dialogs();
    if (faceWizard)
        drawFaceWizard();
    if (nameColorPicker)
        drawNameColorPicker();
    ui.showTooltip();
    if (imageLoading) {
        for (auto &widget : ui.widgets)
            widget.enabled = false;
        graphics.fill(rect(width / 2 - 145, 60, 290, 34), 0x285783, 5);
        graphics.text(tr("ui.application.loading_skin_please_wait"), rect(width / 2 - 130, 68, 260, 24));
    }
    if (!graphics.end())
        recover();
    SetWindowTextW(window,
                   (std::wstring(tr("ui.application.echo_npc_studio")) + (unsaved() ? L" *" : L"")).c_str());
}
void Application::recover() {
    graphics = Graphics{};
    graphics.initialize(window, forceWarp);
    ui.initialize(window, graphics);
    if (selected >= 0)
        graphics.setSkin(skinForPreview(npcs[selected]));
    status = tr("ui.application.graphics_device_restored");
    InvalidateRect(window, nullptr, FALSE);
}
LRESULT Application::message(UINT msg, WPARAM w, LPARAM l) {
    switch (msg) {
    case WM_GETMINMAXINFO: {
        auto info = reinterpret_cast<MINMAXINFO *>(l);
        UINT dpi = GetDpiForWindow(window);
        RECT r{0, 0, MulDiv(1100, dpi, 96), MulDiv(700, dpi, 96)};
        AdjustWindowRectExForDpi(&r, WS_OVERLAPPEDWINDOW, FALSE, 0, dpi);
        info->ptMinTrackSize = {r.right - r.left, r.bottom - r.top};
        return 0;
    }
    case WM_SIZE:
        minimized = w == SIZE_MINIMIZED;
        if (ready && !minimized) {
            graphics.resize(LOWORD(l), HIWORD(l), static_cast<float>(GetDpiForWindow(window)));
            layout();
            InvalidateRect(window, nullptr, FALSE);
        }
        return 0;
    case WM_DPICHANGED: {
        auto r = reinterpret_cast<RECT *>(l);
        SetWindowPos(window, nullptr, r->left, r->top, r->right - r->left, r->bottom - r->top,
                     SWP_NOZORDER | SWP_NOACTIVATE);
        if (ready) {
            RECT client;
            GetClientRect(window, &client);
            graphics.resize(client.right, client.bottom, static_cast<float>(HIWORD(w)));
            InvalidateRect(window, nullptr, FALSE);
        }
        return 0;
    }
    case WM_ERASEBKGND:
        return 1;
    case WM_PAINT: {
        PAINTSTRUCT ps;
        BeginPaint(window, &ps);
        EndPaint(window, &ps);
        if (ready && !minimized)
            draw();
        return 0;
    }
    case WM_TIMER:
        if (w == 6 && portraitBlink && leftTab == 1 && selected >= 0 && npcs[selected].faceRig) {
            portraitClosure =
                blinkClosure(*npcs[selected].faceRig,
                             std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count());
            rebuildPortrait();
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (w == scanTimer)
            pollScan();
        else if (w == animationTimer && !minimized)
            InvalidateRect(window, nullptr, FALSE);
        else if (w == 3)
            smokeTest();
        else if (w == 4) {
            KillTimer(window, 4);
            InvalidateRect(window, nullptr, FALSE);
        } else if (w == 5)
            pollImage();
        return 0;
    case WM_MOUSEMOVE: {
        float x = GET_X_LPARAM(l) / scale, y = GET_Y_LPARAM(l) / scale;
        ui.move(x, y);
        SetTimer(window, 4, 800, nullptr);
        if (draftDragging)
            draftMove(x, y);
        else if (painting)
            eyePaint(x, y, false);
        else if (portraitDragging)
            portraitMove(x, y);
        else if (splitter) {
            if (splitter == 1)
                left = x;
            else if (splitter == 2)
                right = width - x;
            else
                bottom = height - y;
            layout();
        } else if (rotating) {
            yaw -= (x - lastX) * .012f;
            pitch = std::clamp(pitch - (y - lastY) * .008f, -1.4f, 1.4f);
        }
        lastX = x;
        lastY = y;
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    case WM_LBUTTONDOWN: {
        float x = GET_X_LPARAM(l) / scale, y = GET_Y_LPARAM(l) / scale;
        lastX = x;
        lastY = y;
        if (eyeDialog) {
            eyePaint(x, y, true);
            if (painting) {
                InvalidateRect(window, nullptr, FALSE);
                return 0;
            }
        }
        if (ui.down(x, y)) {
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (nameColorDown(x, y)) {
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (draftDown(x, y)) {
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (createDialog || eyeDialog || faceWizard || nameColorPicker)
            return 0;
        if (portraitDown(x, y)) {
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        contextIndex = -1;
        float end = height - bottom;
        if (showProperties && std::abs(x - left) < 6 && y > 58 && y < end)
            splitter = 1;
        else if (showLibrary && std::abs(x - (width - right)) < 6 && y > 58 && y < end)
            splitter = 2;
        else if (showBottom && std::abs(y - end) < 6)
            splitter = 3;
        else if (leftTab == 0 && x > left && x < width - right && y > 110 && y < end && selected >= 0)
            rotating = true;
        if (splitter || rotating)
            SetCapture(window);
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    case WM_LBUTTONUP: {
        finishDraftDrag();
        portraitDragging = false;
        painting = false;
        splitter = 0;
        rotating = false;
        ui.up(GET_X_LPARAM(l) / scale, GET_Y_LPARAM(l) / scale);
        ReleaseCapture();
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    case WM_CAPTURECHANGED:
        finishDraftDrag();
        portraitDragging = false;
        rotating = false;
        splitter = 0;
        painting = false;
        return 0;
    case WM_RBUTTONUP:
        if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker) {
            float x = GET_X_LPARAM(l) / scale, y = GET_Y_LPARAM(l) / scale;
            for (auto &widget : ui.widgets)
                if (widget.id >= 1000 && widget.id < 1000 + int(npcs.size()) &&
                    contains(widget.bounds, x, y)) {
                    contextIndex = widget.id - 1000;
                    contextX = x;
                    contextY = y;
                    break;
                }
            InvalidateRect(window, nullptr, FALSE);
        }
        return 0;
    case WM_MOUSEWHEEL: {
        if (createDialog || eyeDialog || faceWizard || nameColorPicker)
            return 0;
        POINT p{GET_X_LPARAM(l), GET_Y_LPARAM(l)};
        ScreenToClient(window, &p);
        float x = p.x / scale, y = p.y / scale, delta = GET_WHEEL_DELTA_WPARAM(w) / 120.f;
        if (showLibrary && x > width - right && y > 166 && y < height - bottom)
            libraryScroll = std::clamp(libraryScroll - delta * 45, 0.f,
                                       std::max(0.f, libraryContentHeight - (height - bottom - 176)));
        else if (leftTab == 1 && x < left && y > 110 && y < height - bottom)
            portraitScroll = std::clamp(portraitScroll - delta * 36, 0.f, portraitMaxScroll);
        else if (x < left && y > 110 && y < height - bottom)
            propertiesScroll =
                std::clamp(propertiesScroll - delta * 36, 0.f, std::max(0.f, 760.f - (height - bottom)));
        else if (tab == 2 && y > height - bottom + 48)
            diagnosticsScroll =
                std::clamp(diagnosticsScroll - delta * 40, 0.f,
                           std::max(0.f, float(diagnostics.size()) * 38 + 30 - (bottom - 85)));
        else if (x > left && x < width - right && y > 110 && y < height - bottom)
            distance = std::clamp(distance * std::pow(.9f, delta), 25.f, 180.f);
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    case WM_SETCURSOR:
        if (LOWORD(l) == HTCLIENT) {
            POINT p;
            GetCursorPos(&p);
            ScreenToClient(window, &p);
            float x = p.x / scale, y = p.y / scale;
            LPCWSTR cursor = IDC_ARROW;
            if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker) {
                if ((std::abs(x - left) < 6 || std::abs(x - (width - right)) < 6) && y > 58 &&
                    y < height - bottom)
                    cursor = IDC_SIZEWE;
                else if (std::abs(y - (height - bottom)) < 6)
                    cursor = IDC_SIZENS;
            }
            if (faceWizard) {
                int hit = draftHit(x, y);
                if (draftPickingSkin || draftNewRect)
                    cursor = IDC_CROSS;
                else if (hit == 0)
                    cursor = IDC_SIZEALL;
                else if (hit == 1 || hit == 2)
                    cursor = IDC_SIZEWE;
                else if (hit == 4 || hit == 8)
                    cursor = IDC_SIZENS;
                else if (hit == 5 || hit == 10)
                    cursor = IDC_SIZENWSE;
                else if (hit == 6 || hit == 9)
                    cursor = IDC_SIZENESW;
            }
            for (auto &widget : ui.widgets)
                if (widget.edit && widget.enabled && contains(widget.bounds, x, y))
                    cursor = IDC_IBEAM;
            SetCursor(LoadCursorW(nullptr, cursor));
            return TRUE;
        }
        break;
    case WM_KEYDOWN:
        if (imageLoading)
            return 0;
        if (w == VK_ESCAPE) {
            nameColorPicker = false;
            faceWizard = false;
            draftDragging = false;
            createDialog = false;
            eyeDialog = false;
            modelDropdown = false;
            contextIndex = -1;
            ui.clearFocus();
        } else if (faceWizard && !ui.hasFocus() && (GetKeyState(VK_CONTROL) & 0x8000) &&
                   (w == 'Z' || w == 'Y')) {
            draftUndo(w == 'Y' || (GetKeyState(VK_SHIFT) & 0x8000));
        } else if (faceWizard && w == VK_RETURN) {
            try {
                if (draftSection == 1)
                    applyLidFields();
                else
                    applyDraftFields();
                ui.clearFocus();
            } catch (const std::exception &e) {
                draftError = wide(e.what());
            }
        } else if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker &&
                   (GetKeyState(VK_CONTROL) & 0x8000) && w == 'S') {
            save((GetKeyState(VK_SHIFT) & 0x8000) != 0);
        } else if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker &&
                   (GetKeyState(VK_CONTROL) & 0x8000) && w == 'O') {
            openProject(folderDialog());
        } else if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker && leftTab == 1 &&
                   selected >= 0 && npcs[selected].faceRig && !ui.hasFocus() &&
                   (GetKeyState(VK_CONTROL) & 0x8000) && (w == 'Z' || w == 'Y')) {
            portraitUndo(w == 'Y' || (GetKeyState(VK_SHIFT) & 0x8000));
        } else if (!createDialog && !eyeDialog && !faceWizard && !nameColorPicker && leftTab == 1 &&
                   !ui.hasFocus() && (w == VK_LEFT || w == VK_RIGHT || w == VK_UP || w == VK_DOWN)) {
            applyPortraitPart();
            double step = portraitStep / 16 * ((GetKeyState(VK_SHIFT) & 0x8000) ? .1 : 1);
            movePortrait(w == VK_LEFT    ? -step
                         : w == VK_RIGHT ? step
                                         : 0,
                         w == VK_UP     ? -step
                         : w == VK_DOWN ? step
                                        : 0);
        } else
            ui.key(w);
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    case WM_CHAR:
        if (!imageLoading)
            ui.character(static_cast<wchar_t>(w));
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    case WM_UNICHAR:
        if (w == UNICODE_NOCHAR)
            return TRUE;
        if (w <= 0xffff)
            ui.character(wchar_t(w));
        else {
            ui.character(wchar_t(0xd800 + ((w - 0x10000) >> 10)));
            ui.character(wchar_t(0xdc00 + ((w - 0x10000) & 1023)));
        }
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    case WM_IME_COMPOSITION:
        if (ui.ime(l)) {
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        break;
    case WM_IME_ENDCOMPOSITION:
        ui.endComposition();
        InvalidateRect(window, nullptr, FALSE);
        break;
    case WM_DROPFILES: {
        HDROP drop = reinterpret_cast<HDROP>(w);
        wchar_t path[32768];
        POINT p;
        DragQueryPoint(drop, &p);
        DragQueryFileW(drop, 0, path, 32768);
        DragFinish(drop);
        if (faceWizard || nameColorPicker) {
            status = tr("ui.application.close_the_wizard_before_importing_another_skin");
        } else if (createDialog) {
            newSkin.set(path);
        } else if (!eyeDialog && !scanning && !imageLoading) {
            float x = p.x / scale, y = p.y / scale;
            if (x > left && x < width - right && y > 110 && y < height - bottom)
                loadSkinAsync(path, true);
            else
                status = tr("ui.application.drop_the_skin_directly_onto_the_3d_preview");
        }
        InvalidateRect(window, nullptr, FALSE);
        return 0;
    }
    case WM_CLOSE:
        if (confirmClose()) {
            stop = true;
            DestroyWindow(window);
        }
        return 0;
    case WM_DESTROY:
        KillTimer(window, scanTimer);
        KillTimer(window, animationTimer);
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(window, msg, w, l);
}
LRESULT CALLBACK Application::procedure(HWND hwnd, UINT msg, WPARAM w, LPARAM l) {
    auto app = reinterpret_cast<Application *>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    if (msg == WM_NCCREATE) {
        app = static_cast<Application *>(reinterpret_cast<CREATESTRUCTW *>(l)->lpCreateParams);
        app->window = hwnd;
        SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(app));
    }
    if (app)
        try {
            return app->message(msg, w, l);
        } catch (const DeviceLost &) {
            try {
                app->recover();
            } catch (const std::exception &e) {
                app->ready = false;
                app->report(e);
            }
            return 0;
        } catch (const std::exception &e) {
            app->report(e);
            return 0;
        }
    return DefWindowProcW(hwnd, msg, w, l);
}
int Application::run(HINSTANCE instance, int show, const std::vector<std::wstring> &arguments,
                     const std::vector<std::wstring> &localizationWarnings) {
    localizationIssues = localizationWarnings;
    diagnostics.insert(diagnostics.end(), localizationIssues.begin(), localizationIssues.end());
    status = tr("ui.application.open_a_project_folder_or_import_a_minecraft_png_skin");
    fs::path initial;
    for (size_t i = 0; i < arguments.size(); ++i) {
        auto &a = arguments[i];
        if (a == L"--warp")
            forceWarp = true;
        else if (a == L"--lang" && i + 1 < arguments.size())
            ++i;
        else if (a == L"--smoke-test" && i + 1 < arguments.size())
            smokeFolder = arguments[++i];
        else
            initial = a;
    }
    if (!smokeFolder.empty())
        fs::create_directories(smokeFolder);
    SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
    WNDCLASSEXW cls{};
    cls.cbSize = sizeof(cls);
    cls.hInstance = instance;
    cls.lpfnWndProc = procedure;
    cls.lpszClassName = L"EchoNpcStudioWindow";
    cls.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    cls.hIcon = LoadIconW(nullptr, IDI_APPLICATION);
    check(RegisterClassExW(&cls) ? S_OK : HRESULT_FROM_WIN32(GetLastError()),
          tr8("error.application.register_window"));
    RECT bounds{0, 0, 1440, 900};
    UINT dpi = GetDpiForSystem();
    bounds.right = MulDiv(bounds.right, dpi, 96);
    bounds.bottom = MulDiv(bounds.bottom, dpi, 96);
    AdjustWindowRectExForDpi(&bounds, WS_OVERLAPPEDWINDOW, FALSE, 0, dpi);
    window = CreateWindowExW(0, cls.lpszClassName, tr("ui.application.echo_npc_studio").c_str(),
                             WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, bounds.right - bounds.left,
                             bounds.bottom - bounds.top, nullptr, nullptr, instance, this);
    if (!window)
        throw std::runtime_error(tr8("error.application.cannot_create_main_window"));
    graphics.initialize(window, forceWarp);
    ui.initialize(window, graphics);
    ready = true;
    layout();
    DragAcceptFiles(window, TRUE);
    ShowWindow(window, smokeFolder.empty() ? show : SW_HIDE);
    UpdateWindow(window);
    if (initial.empty() && smokeFolder.empty()) {
        wchar_t appData[32768]{};
        GetEnvironmentVariableW(L"APPDATA", appData, 32768);
        auto knownPack = fs::path(appData) / L"PrismLauncher" / L"instances" / L"Форма Мысли" / L"minecraft" /
                         L"story" / L"content-pack" / L"jdh";
        auto skins = knownPack / L"assets" / L"jdh" / L"textures" / L"echo_npc" / L"skins";
        if (fs::is_directory(skins))
            initial = skins;
    }
    if (!initial.empty()) {
        if (fs::is_directory(initial))
            openProject(initial);
        else {
            updateSkin(loadSkin(initial));
            auto index = selected;
            readMetadata(npcs[index]);
            npcs[index].dirty = !fs::exists(npcs[index].metadata);
            selected = -1;
            select(index);
        }
    }
    if (!smokeFolder.empty())
        SetTimer(window, 3, 100, nullptr);
    MSG msg{};
    while (true) {
        int result = GetMessageW(&msg, nullptr, 0, 0);
        if (result == 0)
            return static_cast<int>(msg.wParam);
        if (result == -1)
            throw std::runtime_error(tr8("error.application.message_loop_failed"));
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
}
} // namespace echo
