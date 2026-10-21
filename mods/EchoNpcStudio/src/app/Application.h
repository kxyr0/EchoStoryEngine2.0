#pragma once
#include "npc/Removal.h"
#include "ui/UI.h"
#include "npc/Repository.h"
#include "npc/GameExport.h"
#include "portrait/FaceAuthoring.h"
#include <future>
#include <chrono>
namespace echo {
class Application {
    HWND window = nullptr;
    Graphics graphics;
    UI ui;
    bool ready = false, minimized = false, forceWarp = false;
    float width = 1440, height = 900, scale = 1, left = 300, right = 310, bottom = 235;
    int splitter = 0;
    bool rotating = false, head = false, idle = false;
    float yaw = 0, pitch = .05f, distance = 60, lastX = 0, lastY = 0, libraryScroll = 0;
    std::chrono::steady_clock::time_point start = std::chrono::steady_clock::now();
    std::vector<Npc> npcs;
    std::vector<std::wstring> diagnostics;
    std::vector<std::wstring> localizationIssues;
    int selected = -1, tab = 0;
    fs::path project;
    std::wstring status = tr("ui.application.open_a_project_folder_or_import_a_minecraft_png_skin");
    TextEdit identifier, display, color, search, skinPath;
    std::atomic_bool stop = false;
    std::future<ScanResult> scan;
    bool scanning = false;
    std::future<std::shared_ptr<Skin>> imageTask;
    bool imageLoading = false, imageReplace = false;
    int imageTarget = -1;
    bool createDialog = false, eyeDialog = false, modelDropdown = false;
    int contextIndex = -1;
    float contextX = 0, contextY = 0;
    std::map<std::wstring, bool> collapsedFolders;
    std::array<bool, 2> collapsedCategories{};
    bool exportAfterWizard = false;
    void saveEchoScript();
    TextEdit newId, newName, newSkin, newFolder;
    std::wstring newNpcColor = L"#DEC6B2", draftNpcColor;
    void applyBackgroundTemplate(bool forFace);
    bool newSlim = false;
    bool nameColorPicker = false;
    Image clothingFront, clothingBack;
    D2D1_RECT_F clothingFrontRect{}, clothingBackRect{}, colorAtlasRect{};
    void openNameColorPicker();
    void drawNameColorPicker();
    bool nameColorDown(float x, float y);
    bool showSkinLayers = false, editCurrentFace = false;
    void openCurrentFaceSettings();
    bool showProperties = true, showLibrary = true, showBottom = true;
    float savedLeft = 300, savedRight = 310, savedBottom = 235;
    int draftSection = 0, draftLid = 0;
    TextEdit lidOffset, lidHeight, lidTravel, lidClosure;
    double draftClosure = 0;
    std::array<Image, 4> eyelidMini;
    void refreshLidFields();
    void applyLidFields();
    bool faceWizard = false, draftPickingSkin = false, draftDragging = false;
    int draftCrop = 0, draftStartX = 0, draftStartY = 0;
    int draftDragEdges = 0; // L=1 R=2 T=4 B=8; 0 move, 16 draw
    float draftPointerX = 0, draftPointerY = 0, draftDragPower = 1;
    bool draftNewRect = false, draftAllBounds = true;
    TextEdit draftPower;
    FaceDraft draftDragBase;
    std::vector<FaceDraft> draftUndoStates, draftRedoStates;
    void finishDraftDrag();
    void draftUndo(bool redo = false);
    int draftHit(float x, float y) const;
    FaceDraft faceDraft;
    FacePackage facePackage;
    TextEdit draftId, draftName, draftPack, draftX, draftY, draftW, draftH;
    std::wstring draftError;
    D2D1_RECT_F draftCanvas{};
    void openFaceWizard();
    void drawFaceWizard();
    void refreshDraftFields();
    void applyDraftFields(bool record = true);
    void rebuildDraft();
    void commitFaceWizard();
    bool draftDown(float x, float y);
    void draftMove(float x, float y);
    Image face;
    EyeMask eyes;
    int eyeSide = 0;
    bool painting = false;
    uint8_t paintValue = 0;
    float propertiesScroll = 0, diagnosticsScroll = 0, libraryContentHeight = 0;
    fs::path smokeFolder;
    int smokeStage = 0;
    bool smokeFailed = false;
    unsigned eyeX = 0, eyeY = 0;
    int leftTab = 0, portraitPart = -1;
    int portraitGroup = 0;
    double portraitStep = .1;
    bool portraitDragRecorded = false;
    struct PortraitState {
        struct Part {
            std::array<double, 3> position;
            std::array<int, 4> area;
            bool visible;
        };
        std::vector<Part> parts;
        std::map<std::string, std::map<std::string, double>> layers;
    };
    struct PortraitHistory {
        std::vector<PortraitState> undo, redo;
    };
    std::map<FaceRig *, PortraitHistory> portraitHistories;
    PortraitState capturePortrait() const;
    void recordPortrait();
    void portraitUndo(bool redo = false);
    void restorePortrait(const PortraitState &state);
    void refreshPortraitFields();
    void movePortrait(double x, double y, bool record = true);
    bool portraitIncluded(const FaceQuad &quad, int index) const;
    float portraitScroll = 0, portraitMaxScroll = 0;
    bool portraitDragging = false, portraitGrid = true, portraitBlink = false;
    double portraitClosure = 0;
    Image portraitComposite;
    TextEdit partX, partY, partZ, areaX, areaY, areaW, areaH;
    std::wstring portraitSnapshot;
    std::wstring portraitFields() const;
    D2D1_RECT_F portraitCanvas{};
    void portraitPanel();
    void portraitPreview();
    void portraitBottom();
    void selectPortraitPart(int index);
    void applyPortraitPart();
    void rebuildPortrait();
    void savePortraitRig(bool saveAs = false);
    void exportComposedPortrait();
    bool portraitDown(float x, float y);
    void portraitMove(float x, float y);
    void layout();
    void draw();
    void toolbar();
    void properties();
    void library();
    void bottomPanel();
    void dialogs();
    void sync();
    void select(int index);
    void updateSkin(std::shared_ptr<Skin> skin);
    void openProject(const fs::path &root);
    void pollScan();
    void importSkin(bool replace);
    void save(bool saveAs = false);
    void createNpc();
    void removeNpc(int index);
    std::optional<RemovedNpcFiles> lastRemoval;
    void restoreRemovedNpc();
    void duplicateNpc(int index);
    void loadSkinAsync(const fs::path &path, bool replace);
    void pollImage();
    bool unsaved() const;
    bool discard();
    bool confirmClose();
    void saveAllChanges();
    int smokeCloseChoice = 0;
    bool duplicateId(const std::wstring &id, int except = -1) const;
    fs::path fileDialog(bool save, const std::wstring &filter, const wchar_t *extension = nullptr);
    fs::path folderDialog();
    void reveal(const fs::path &path);
    void exportPortrait();
    void openEyes();
    void exportEyes();
    void eyePaint(float x, float y, bool first);
    void clipboard(const std::wstring &text);
    void report(const std::exception &e);
    void recover();
    LRESULT message(UINT msg, WPARAM w, LPARAM l);
    void smokeTest();
    static LRESULT CALLBACK procedure(HWND hwnd, UINT msg, WPARAM w, LPARAM l);

  public:
    int run(HINSTANCE instance, int show, const std::vector<std::wstring> &arguments,
            const std::vector<std::wstring> &localizationWarnings = {});
    ~Application();
};
} // namespace echo
