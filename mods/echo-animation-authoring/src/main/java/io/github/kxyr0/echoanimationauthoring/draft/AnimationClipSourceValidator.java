package io.github.kxyr0.echoanimationauthoring.draft;

final class AnimationClipSourceValidator {
    String structuralError(String source) {
        if (!source.contains("AnimationClipResource")) {
            return "AnimationClipResource base type is missing.";
        }
        if (!source.contains("Id(") || !source.contains("Rig(") || !source.contains("Duration(")) {
            return "Id/Rig/Duration declaration is missing.";
        }
        if (!source.contains("Track(") || !source.contains("Key(")) {
            return "Track/Key intrinsic call is missing.";
        }
        if (source.contains("return AnimationClip(")) {
            return "Legacy return-style AnimationClip is not accepted for new editor saves.";
        }
        return "";
    }
}
