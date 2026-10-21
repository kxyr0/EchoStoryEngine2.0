# Echo UI Animations

Standalone Java 17 library with deterministic UI animation presets for Forge or any other Java mod.

The library has no Minecraft or Forge dependency. A mod samples a preset and applies the returned values to its own renderer.

## Build

```powershell
.\gradlew.bat check build publishToMavenLocal --console=plain
```

## Dependency

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'io.github.kxyr0.echo:echo-ui-animations:0.1.0-SNAPSHOT'
}
```

## Basic Usage

```java
AnimationPreset preset = AnimationPresetCatalog.PANEL_OPEN;
AnimationFrame frame = AnimationTimeline.sample(preset, elapsedMs, text.length());

int alpha = Math.round(255 * frame.alpha());
float scale = frame.scale();
int visibleChars = frame.visibleCharacters();
```

Useful presets:

- `FADE_IN`
- `FADE_OUT`
- `PANEL_OPEN`
- `PANEL_CLOSE`
- `PANEL_RESIZE`
- `TYPEWRITER`
- `CHOICE_HOVER`

Shape rules:

- max 200 logical lines per Java source file;
- max 7 Java files per package folder.
