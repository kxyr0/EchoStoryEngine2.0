# NEW UI Project Map

Реальные production файлы после реализации:

- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/UiReferenceCanvas.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/UiViewport.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/UiLayoutConfig.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/JournalBookLayout.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/JournalBookLayoutResult.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/PhysicalToGuiMapper.java`
- `new-ui-core/src/main/java/io/github/kxyr0/echonewui/core/layout/UiRectangle.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/EchoNewUserInterfaceMod.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/client/NewUiClientBootstrap.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/client/JournalBookPreviewState.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/command/NewUiClientCommands.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/viewport/UiViewportSnapshot.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/overlay/JournalBookOverlay.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/render/JournalBookRenderer.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/hud/CleanHudSuppressionPolicy.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/config/NewUiClientConfig.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/diagnostic/NewUiDiagnosticCode.java`
- `new-ui-forge-1.20.1/src/main/java/io/github/kxyr0/echonewui/forge/diagnostic/NewUiDiagnostics.java`

Runtime flow:

`client command -> JournalBookPreviewState -> JournalBookOverlay -> UiViewportSnapshot -> JournalBookLayout -> physical rectangle -> PhysicalToGuiMapper -> JournalBookRenderer -> GuiGraphics`

Clean HUD flow:

`RenderGuiOverlayEvent.Pre -> CleanHudSuppressionPolicy -> cancel HUD overlay`

Отдельных зависимостей на Echo Dialogue нет.
