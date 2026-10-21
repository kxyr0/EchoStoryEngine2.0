from pathlib import Path
import sys

ROOT = Path.cwd()
errors = []
notes = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"НЕ НАЙДЕН: {rel}")
        return ""
    return path.read_text(encoding="utf-8", errors="replace")

def require(rel, needle, label=None):
    text = read(rel)
    if needle not in text:
        errors.append(f"{label or rel}: нет ожидаемого фрагмента {needle!r}")

def forbid(rel, needle, label=None):
    text = read(rel)
    if needle in text:
        errors.append(f"{label or rel}: найден запрещённый старый фрагмент {needle!r}")

# User's new split-body/skinning source must survive the overlay.
for rel in (
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/SegmentedHumanoidGeometry.java",
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/skinning/model/SkinnedAttachmentBindings.java",
):
    if not (ROOT / rel).exists():
        errors.append(f"Похоже, потеряна новая пользовательская часть: {rel}")

require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/HumanoidRenderDispatcher.java",
    "HumanoidPoseResolver",
    "HumanoidRenderDispatcher"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidRigResolver.java",
    "activeClipRig",
    "active clip rig fallback"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidPoseLayers.java",
    "clip.schemaVersion() < 3",
    "legacy split-body compatibility"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidPoseLayers.java",
    "BODY_LOWER",
    "body_lower overlay"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidPoseResolver.java",
    "if (context.sitting())",
    "exclusive sitting state"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidSystemPoseLibrary.java",
    '"jdh:idle_main"',
    "canonical idle"
)
forbid(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/client/humanoid/state/HumanoidSystemPoseLibrary.java",
    "modern_idle_relaxed",
    "stale hardcoded modern idle"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/playback/AnimationRuntimeNbtCodec.java",
    "AnimationRuntimeTransientSanitizer.snapshotForPersistence(data)",
    "rejoin persistence"
)
require(
    "animation-forge-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/playback/AnimationPlaybackHost.java",
    "AnimationRuntimeTransientSanitizer.sanitize(data)",
    "startup transient sanitation"
)
require(
    "animation-forge-api-1.20.1/src/main/java/io/github/kxyr0/echoanimation/forge/api/AnimationService.java",
    "setBodyPostureLocked",
    "BODY posture API"
)
require("gradle.properties", "mod_version=0.8.0-SNAPSHOT", "mod version")

if errors:
    print("\nMERGE CHECK: FAIL\n")
    for e in errors:
        print(" -", e)
    sys.exit(1)

print("MERGE CHECK: OK")
print("- user split-body/skinning files на месте")
print("- state machine на месте")
print("- canonical idle на месте")
print("- rejoin persistence patch на месте")
print("- legacy body -> body_lower compatibility на месте")
