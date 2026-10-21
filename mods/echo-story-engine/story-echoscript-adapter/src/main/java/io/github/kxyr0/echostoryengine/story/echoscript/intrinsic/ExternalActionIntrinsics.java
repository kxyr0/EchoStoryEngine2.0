package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ExternalActionIntrinsics {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String STORY_START = "echo_story:start";
    private static final String VISIBILITY = "echo_visibility:";

    private final SourceNodeFactory nodeFactory;

    public ExternalActionIntrinsics(SourceNodeFactory nodeFactory) { this.nodeFactory = nodeFactory; }

    public Optional<NodeSpec> linearNpcSpawnAt(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Npc.SpawnAt", 4, Set.of("yaw", "pitch", "worldId"));
        Map<String, String> arguments = positionalArgs(reader, "Npc.SpawnAt", "npcId", "x", "y", "z");
        put(arguments, "yaw", reader.requiredNamedScalar("yaw", "Npc.SpawnAt"));
        put(arguments, "pitch", reader.requiredNamedScalar("pitch", "Npc.SpawnAt"));
        put(arguments, "worldId", defaulted(reader, "worldId", "Npc.SpawnAt", OVERWORLD));
        return actionSpec(span, "echo_npc:spawn_at", arguments);
    }

    public Optional<NodeSpec> linearNpcSleepAt(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Npc.SleepAt", 4, Set.of("expectedBlock", "worldId"));
        Map<String, String> arguments = positionalArgs(reader, "Npc.SleepAt", "npcId", "bedX", "bedY", "bedZ");
        put(arguments, "expectedBlock", reader.optionalNamedScalar("expectedBlock", "Npc.SleepAt"));
        put(arguments, "worldId", defaulted(reader, "worldId", "Npc.SleepAt", OVERWORLD));
        return actionSpec(span, "echo_npc:sleep_at", arguments);
    }

    public Optional<NodeSpec> linearNpcWakeUp(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Npc.WakeUp", 1, Set.of());
        return actionSpec(span, "echo_npc:wake_up", positionalArgs(reader, "Npc.WakeUp", "npcId"));
    }

    Optional<NodeSpec> linearDialogueLoad(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Dialogue.Load", 1, Set.of("audienceId", "output"));
        Map<String, String> arguments = positionalArgs(reader, "Dialogue.Load", "dialogueId");
        put(arguments, "audienceId", reader.optionalNamedScalar("audienceId", "Dialogue.Load"));
        var fields = new NodeSpec.ActionFields(Optional.empty(), Optional.of("echo_dialogue:play"),
                reader.optionalNamedString("output", "Dialogue.Load"), arguments);
        return Optional.of(NodeSpec.action(span, fields));
    }

    Optional<NodeSpec> linearStoryLoad(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Story.Load", 1, Set.of("executionId"));
        Map<String, String> arguments = positionalArgs(reader, "Story.Load", "storyId");
        put(arguments, "executionId", reader.optionalNamedScalar("executionId", "Story.Load"));
        return actionSpec(span, STORY_START, arguments);
    }

    Optional<NodeSpec> linearScreenSetBlack(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Screen.SetBlack", 2, Set.of("canvasGroup", "alpha", "time", "durationMillis"));
        Map<String, String> arguments = positionalArgs(reader, "Screen.SetBlack", "canvasGroup", "time");
        put(arguments, "canvasGroup", reader.optionalNamedScalar("alpha", "Screen.SetBlack"));
        put(arguments, "time", reader.optionalNamedScalar("durationMillis", "Screen.SetBlack"));
        return actionSpec(span, "echo_screen:set_black", arguments);
    }

    Optional<NodeSpec> linearEnvironmentSet(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Environment.Set", 2, Set.of());
        return actionSpec(span, "echo_story_engine:environment_set",
                positionalArgs(reader, "Environment.Set", "environmentId", "stateId"));
    }

    Optional<NodeSpec> linearVisibility(IntrinsicCallReader reader, SourceSpan span) {
        String name = reader.targetName();
        accept(reader, name, 1, Set.of("player", "output"));
        Map<String, String> arguments = new LinkedHashMap<>();
        put(arguments, "watchId", reader.positionalScalar(0, name, "watchId"));
        put(arguments, "player", reader.optionalNamedScalar("player", name));
        Optional<String> output = visibilityOutput(reader, name);
        put(arguments, "outputKey", output);
        return Optional.of(NodeSpec.action(span, new NodeSpec.ActionFields(Optional.empty(),
                Optional.of(VISIBILITY + visibilityAction(name)), output, arguments)));
    }

    private Optional<String> visibilityOutput(IntrinsicCallReader reader, String name) {
        if (!name.equals("View.IsOutOfView") && !name.equals("View.IsVisible")) {
            return Optional.empty();
        }
        String fallback = name.equals("View.IsVisible") ? "visibility.isVisible" : "visibility.isOutOfView";
        return reader.optionalNamedString("output", name).or(() -> Optional.of(fallback));
    }

    private String visibilityAction(String name) {
        return switch (name) {
            case "View.Arm" -> "arm"; case "View.Disarm" -> "disarm";
            case "View.WaitUntilOutOfView" -> "wait_until_out_of_view";
            case "View.WaitUntilVisible" -> "wait_until_visible";
            case "View.IsOutOfView" -> "is_out_of_view"; case "View.IsVisible" -> "is_visible";
            case "View.AssertStillOutOfView" -> "assert_still_out_of_view";
            default -> throw new IllegalArgumentException("Unknown View intrinsic: " + name);
        };
    }

    Optional<SourceNode> dialogueLoad(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Dialogue.Load", Set.of(1, 2));
        reader.rejectUnknownNamed("Dialogue.Load", Set.of("dialogueId", "audienceId", "output", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 2) {
            put(arguments, "dialogueId", reader.positionalScalar(1, "Dialogue.Load", "dialogueId"));
        } else {
            put(arguments, "dialogueId", reader.requiredNamedScalar("dialogueId", "Dialogue.Load"));
        }
        put(arguments, "audienceId", reader.optionalNamedScalar("audienceId", "Dialogue.Load"));
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Dialogue.Load"),
                Optional.of("echo_dialogue:play"),
                reader.optionalNamedString("output", "Dialogue.Load"),
                arguments)));
    }

    Optional<SourceNode> storyLoad(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Story.Load", Set.of(1, 2));
        reader.rejectUnknownNamed("Story.Load", Set.of("storyId", "executionId", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 2) {
            put(arguments, "storyId", reader.positionalScalar(1, "Story.Load", "storyId"));
        } else {
            put(arguments, "storyId", reader.requiredNamedScalar("storyId", "Story.Load"));
        }
        put(arguments, "executionId", reader.optionalNamedScalar("executionId", "Story.Load"));
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Story.Load"),
                Optional.of(STORY_START),
                Optional.empty(),
                arguments)));
    }

    Optional<SourceNode> screenSetBlack(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Screen.SetBlack", Set.of(1, 3));
        reader.rejectUnknownNamed("Screen.SetBlack", Set.of("canvasGroup", "alpha", "time", "durationMillis",
                "next", "output"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 3) {
            put(arguments, "canvasGroup", reader.positionalScalar(1, "Screen.SetBlack", "canvasGroup"));
            put(arguments, "time", reader.positionalScalar(2, "Screen.SetBlack", "time"));
        } else {
            put(arguments, "canvasGroup", namedScalarEither(reader, "Screen.SetBlack", "canvasGroup", "alpha"));
            put(arguments, "time", namedScalarEither(reader, "Screen.SetBlack", "time", "durationMillis"));
        }
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Screen.SetBlack"),
                Optional.of("echo_screen:set_black"),
                reader.optionalNamedString("output", "Screen.SetBlack"),
                arguments)));
    }

    Optional<SourceNode> environmentSet(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Environment.Set", Set.of(1, 3));
        reader.rejectUnknownNamed("Environment.Set", Set.of("environmentId", "stateId", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 3) {
            put(arguments, "environmentId", reader.positionalScalar(1, "Environment.Set", "environmentId"));
            put(arguments, "stateId", reader.positionalScalar(2, "Environment.Set", "stateId"));
        } else {
            put(arguments, "environmentId", reader.requiredNamedScalar("environmentId", "Environment.Set"));
            put(arguments, "stateId", reader.requiredNamedScalar("stateId", "Environment.Set"));
        }
        return nodeFactory.node(reader, NodeSpec.action(span, new NodeSpec.ActionFields(
                reader.requiredNamedString("next", "Environment.Set"),
                Optional.of("echo_story_engine:environment_set"),
                Optional.empty(),
                arguments)));
    }

    private Map<String, String> positionalArgs(IntrinsicCallReader reader, String intrinsic, String... names) {
        Map<String, String> arguments = new LinkedHashMap<>();
        for (int index = 0; index < names.length; index++) {
            put(arguments, names[index], reader.positionalScalar(index, intrinsic, names[index]));
        }
        return arguments;
    }

    private Optional<NodeSpec> actionSpec(SourceSpan span, String type, Map<String, String> arguments) {
        return Optional.of(NodeSpec.action(
                span, new NodeSpec.ActionFields(Optional.empty(), Optional.of(type), Optional.empty(), arguments)));
    }

    private void accept(IntrinsicCallReader reader, String intrinsic, int positionalCount, Set<String> named) {
        reader.requirePositionalCount(intrinsic, positionalCount);
        reader.rejectUnknownNamed(intrinsic, named);
    }

    private Optional<String> defaulted(IntrinsicCallReader reader, String name, String intrinsic, String fallback) {
        return reader.optionalNamedScalar(name, intrinsic).or(() -> Optional.of(fallback));
    }

    private Optional<String> namedScalarEither(
            IntrinsicCallReader reader, String intrinsic, String primary, String alias) {
        return reader.optionalNamedScalar(primary, intrinsic)
                .or(() -> reader.optionalNamedScalar(alias, intrinsic))
                .or(() -> reader.requiredNamedScalar(primary, intrinsic));
    }

    private void put(Map<String, String> map, String k, Optional<String> v) { v.ifPresent(value -> map.put(k, value)); }

}
