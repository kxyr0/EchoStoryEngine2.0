package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep;

import static java.util.Map.entry;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SleepActionIntrinsics {
    private static final String SLEEP_BLINK = "Sleep.Blink";
    private static final Set<String> CONTEXT = Set.of("player", "scenario", "owner", "command");
    private static final Set<String> NODE_CONTROL = Set.of("next", "output");
    private static final Map<String, String> TYPES = Map.ofEntries(
            entry("Sleep.Arm", "echo_player_sleep:arm"),
            entry("Sleep.Disarm", "echo_player_sleep:disarm"),
            entry("Sleep.CloseEyes", "echo_player_sleep:close_eyes"),
            entry("Sleep.WaitUntilClosed", "echo_player_sleep:wait_until_closed"),
            entry("Sleep.EnterDream", "echo_player_sleep:enter_dream"),
            entry("Sleep.OpenEyes", "echo_player_sleep:open_eyes"),
            entry("Sleep.WaitUntilOpen", "echo_player_sleep:wait_until_open"),
            entry("Sleep.Wait", "echo_player_sleep:wait"),
            entry("Sleep.WaitSeconds", "echo_player_sleep:wait"),
            entry("Sleep.ReturnToBed", "echo_player_sleep:return_to_bed"),
            entry("Sleep.CompleteNight", "echo_player_sleep:complete_night"),
            entry("Sleep.Complete", "echo_player_sleep:complete"),
            entry("Sleep.Cancel", "echo_player_sleep:cancel"),
            entry("Sleep.IsActive", "echo_player_sleep:is_active"),
            entry("Sleep.IsArmed", "echo_player_sleep:is_armed"),
            entry("Sleep.IsEyesClosed", "echo_player_sleep:is_eyes_closed"));
    private static final Map<String, String> LINEAR_IDS = Map.ofEntries(
            entry("Sleep.Arm", "arm_sleep"),
            entry("Sleep.Disarm", "disarm_sleep"),
            entry("Sleep.CloseEyes", "close_eyes"),
            entry("Sleep.WaitUntilClosed", "wait_until_closed"),
            entry("Sleep.EnterDream", "enter_dream"),
            entry("Sleep.OpenEyes", "open_eyes"),
            entry("Sleep.WaitUntilOpen", "wait_until_open"),
            entry("Sleep.Wait", "wait_sleep"),
            entry("Sleep.WaitSeconds", "wait_sleep"),
            entry("Sleep.ReturnToBed", "return_to_bed"),
            entry("Sleep.CompleteNight", "complete_night"),
            entry("Sleep.Complete", "complete_sleep"),
            entry("Sleep.Cancel", "cancel_sleep"),
            entry("Sleep.IsActive", "is_sleep_active"),
            entry("Sleep.IsArmed", "is_sleep_armed"),
            entry("Sleep.IsEyesClosed", "is_eyes_closed"));
    public static final Set<String> TARGETS = TYPES.keySet();
    public static final Set<String> LINEAR_TARGETS = linearTargets();
    private final SourceNodeFactory nodeFactory;
    private final SleepArgumentReader arguments = new SleepArgumentReader();
    private final SleepBlinkIntrinsics blink = new SleepBlinkIntrinsics();

    public SleepActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public String linearId(String intrinsic) {
        return LINEAR_IDS.getOrDefault(intrinsic, "sleep_action");
    }

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        return linear(reader, span, Map.of());
    }

    public Optional<NodeSpec> linear(
            IntrinsicCallReader reader, SourceSpan span, Map<String, String> blackspaceVariables) {
        String intrinsic = reader.targetName();
        accept(reader, intrinsic, false);
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(
                Optional.empty(),
                Optional.of(TYPES.get(intrinsic)),
                reader.optionalNamedString("output", intrinsic),
                arguments.read(reader, intrinsic, 0, blackspaceVariables))));
    }

    public List<LinearAction> linearActions(
            IntrinsicCallReader reader,
            SourceSpan span,
            Map<String, String> blackspaceVariables) {
        String intrinsic = reader.targetName();
        if (SLEEP_BLINK.equals(intrinsic)) {
            return blink.linear(reader, span);
        }
        return linear(reader, span, blackspaceVariables)
                .map(spec -> List.of(new LinearAction(linearId(intrinsic), spec)))
                .orElseGet(List::of);
    }

    public Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        accept(reader, intrinsic, true);
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", intrinsic),
                Optional.of(TYPES.get(intrinsic)),
                reader.optionalNamedString("output", intrinsic),
                arguments.read(reader, intrinsic, 1, Map.of()))));
    }

    private void accept(IntrinsicCallReader reader, String intrinsic, boolean node) {
        reader.requirePositionalCount(intrinsic, counts(intrinsic, node));
        reader.rejectUnknownNamed(intrinsic, allowed(intrinsic, node));
    }

    private Set<Integer> counts(String intrinsic, boolean node) {
        return switch (intrinsic) {
            case "Sleep.Arm", "Sleep.CloseEyes", "Sleep.OpenEyes",
                    "Sleep.Wait", "Sleep.WaitSeconds" -> offset(node, 0, 1);
            case "Sleep.EnterDream" -> offset(node, 0, 1, 3, 4, 5, 6);
            default -> offset(node, 0);
        };
    }

    private static Set<String> linearTargets() {
        LinkedHashSet<String> targets = new LinkedHashSet<>(TARGETS);
        targets.add(SLEEP_BLINK);
        return Set.copyOf(targets);
    }

    private Set<Integer> offset(boolean node, int... counts) {
        int offset = node ? 1 : 0;
        return Arrays.stream(counts)
                .map(count -> count + offset)
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> allowed(String intrinsic, boolean node) {
        LinkedHashSet<String> allowed = new LinkedHashSet<>(node ? NODE_CONTROL : Set.of("output"));
        allowed.addAll(CONTEXT);
        switch (intrinsic) {
            case "Sleep.Arm" -> allowed.add("scenario");
            case "Sleep.CloseEyes" -> allowed.add("duration");
            case "Sleep.EnterDream" -> allowed.addAll(Set.of(
                    "target", "blackspaceInstanceId", "blackspaceId", "dimension", "worldId",
                    "x", "y", "z", "yaw", "pitch"));
            case "Sleep.OpenEyes" -> allowed.addAll(Set.of("duration", "sleepyBlink", "showHud"));
            case "Sleep.Wait", "Sleep.WaitSeconds" -> allowed.addAll(Set.of("seconds", "duration", "ticks"));
            case "Sleep.ReturnToBed" -> allowed.add("mode");
            default -> {
            }
        }
        return Set.copyOf(allowed);
    }

    public record LinearAction(String id, NodeSpec spec) {
    }
}
