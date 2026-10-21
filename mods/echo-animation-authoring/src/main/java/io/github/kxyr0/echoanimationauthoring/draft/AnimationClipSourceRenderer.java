package io.github.kxyr0.echoanimationauthoring.draft;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AnimationClipSourceRenderer {
    String render(AnimationClipDraft draft) {
        int duration = draft.keyframes().stream().mapToInt(AuthoringKeyframeMutation::frame).max().orElse(1);
        return """
                namespace JDH.Animation;

                class AnimatorEditorClip : AnimationClipResource
                {
                    public override void Build()
                    {
                        Id("%s");
                        Rig("%s");
                        Duration(%d);

                %s
                    }
                }
                """.formatted(draft.clipId(), draft.rigId(), Math.max(1, duration), tracks(draft.keyframes()));
    }

    private String tracks(List<AuthoringKeyframeMutation> keyframes) {
        Map<String, List<AuthoringKeyframeMutation>> byBone = keyframes.stream()
                .collect(Collectors.groupingBy(AuthoringKeyframeMutation::boneId, LinkedHashMap::new,
                        Collectors.toList()));
        return byBone.entrySet().stream()
                .map(entry -> track(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n\n"));
    }

    private String track(String boneId, List<AuthoringKeyframeMutation> keyframes) {
        String variable = localIdentifier(boneId);
        String keys = keyframes.stream().map(frame -> key(variable, frame)).collect(Collectors.joining("\n"));
        return "        var " + variable + " = Track(\"" + boneId + "\");\n" + keys;
    }

    private String key(String variable, AuthoringKeyframeMutation mutation) {
        var transform = mutation.transform();
        String interpolation = mutation.interpolation().toLowerCase(java.util.Locale.ROOT);
        interpolation = interpolation.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + interpolation.substring(1);
        return "        Key(" + variable + ", " + mutation.frame() + ", " + interpolation + ")"
                + ".Position(" + number(transform.x()) + ", " + number(transform.y()) + ", "
                + number(transform.z()) + ")"
                + ".Rotation(" + number(transform.rotX()) + ", " + number(transform.rotY()) + ", "
                + number(transform.rotZ()) + ")"
                + ".Scale(" + number(transform.scaleX()) + ", " + number(transform.scaleY()) + ", "
                + number(transform.scaleZ()) + ");";
    }

    private static String localIdentifier(String value) {
        String[] pieces = value.split("_+");
        StringBuilder result = new StringBuilder(pieces[0]);
        for (int index = 1; index < pieces.length; index++) {
            result.append(Character.toUpperCase(pieces[index].charAt(0))).append(pieces[index].substring(1));
        }
        return result.toString();
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }
}
