package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.actions;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerActionIntrinsics {
    private final SourceNodeFactory nodeFactory;

    public PlayerActionIntrinsics(SourceNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Optional<NodeSpec> linearSpawnAt(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Player.SpawnAt", 3, Set.of("yaw", "pitch"));
        Map<String, String> arguments = positionalArgs(reader, "Player.SpawnAt", "x", "y", "z");
        put(arguments, "yaw", reader.requiredNamedScalar("yaw", "Player.SpawnAt"));
        put(arguments, "pitch", reader.requiredNamedScalar("pitch", "Player.SpawnAt"));
        return actionSpec(span, "echo_player_actor:spawn_at", arguments);
    }

    public Optional<NodeSpec> linearSetHotbarItem(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Player.SetHotbarItem", 2, Set.of("count", "replace"));
        Map<String, String> arguments = positionalArgs(reader, "Player.SetHotbarItem", "slot", "itemId");
        put(arguments, "count", defaulted(reader, "count", "Player.SetHotbarItem", "1"));
        put(arguments, "replace", reader.optionalNamedScalar("replace", "Player.SetHotbarItem"));
        return actionSpec(span, "echo_player_actor:set_hotbar_item", arguments);
    }

    public Optional<NodeSpec> linearSelectHotbarSlot(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Player.SelectHotbarSlot", 1, Set.of());
        return actionSpec(span, "echo_player_actor:select_hotbar_slot",
                positionalArgs(reader, "Player.SelectHotbarSlot", "slot"));
    }

    public Optional<NodeSpec> linearSitAt(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Player.SitAt", 3, Set.of("timeoutTicks"));
        Map<String, String> arguments = positionalArgs(reader, "Player.SitAt", "x", "y", "z");
        put(arguments, "timeoutTicks", defaulted(reader, "timeoutTicks", "Player.SitAt", "40"));
        return actionSpec(span, "echo_player_actor:sit_at", arguments);
    }

    public Optional<NodeSpec> linearStandUp(IntrinsicCallReader reader, SourceSpan span) {
        accept(reader, "Player.StandUp", 0, Set.of());
        return actionSpec(span, "echo_player_actor:stand_up", Map.of());
    }

    public Optional<SourceNode> spawnAt(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.SpawnAt", Set.of(1, 4, 6));
        reader.rejectUnknownNamed("Player.SpawnAt", Set.of("x", "y", "z", "yaw", "pitch", "next", "output"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() >= 4) {
            put(arguments, "x", reader.positionalScalar(1, "Player.SpawnAt", "x"));
            put(arguments, "y", reader.positionalScalar(2, "Player.SpawnAt", "y"));
            put(arguments, "z", reader.positionalScalar(3, "Player.SpawnAt", "z"));
        } else {
            put(arguments, "x", reader.requiredNamedScalar("x", "Player.SpawnAt"));
            put(arguments, "y", reader.requiredNamedScalar("y", "Player.SpawnAt"));
            put(arguments, "z", reader.requiredNamedScalar("z", "Player.SpawnAt"));
        }
        putRotation(arguments, reader);
        return action(reader, new ActionCall(span, "Player.SpawnAt", "echo_player_actor:spawn_at", arguments));
    }

    public Optional<SourceNode> setHotbarItem(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.SetHotbarItem", Set.of(1, 3, 4));
        reader.rejectUnknownNamed("Player.SetHotbarItem", Set.of("slot", "itemId", "count", "replace", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() >= 3) {
            put(arguments, "slot", reader.positionalScalar(1, "Player.SetHotbarItem", "slot"));
            put(arguments, "itemId", reader.positionalScalar(2, "Player.SetHotbarItem", "itemId"));
        } else {
            put(arguments, "slot", reader.requiredNamedScalar("slot", "Player.SetHotbarItem"));
            put(arguments, "itemId", reader.requiredNamedScalar("itemId", "Player.SetHotbarItem"));
        }
        putCount(arguments, reader);
        put(arguments, "replace", reader.optionalNamedScalar("replace", "Player.SetHotbarItem"));
        return action(reader, new ActionCall(
                span, "Player.SetHotbarItem", "echo_player_actor:set_hotbar_item", arguments));
    }

    public Optional<SourceNode> selectHotbarSlot(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.SelectHotbarSlot", Set.of(1, 2));
        reader.rejectUnknownNamed("Player.SelectHotbarSlot", Set.of("slot", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 2) {
            put(arguments, "slot", reader.positionalScalar(1, "Player.SelectHotbarSlot", "slot"));
        } else {
            put(arguments, "slot", reader.requiredNamedScalar("slot", "Player.SelectHotbarSlot"));
        }
        return action(reader, new ActionCall(
                span, "Player.SelectHotbarSlot", "echo_player_actor:select_hotbar_slot", arguments));
    }

    public Optional<SourceNode> sitAt(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.SitAt", Set.of(1, 4));
        reader.rejectUnknownNamed("Player.SitAt", Set.of("x", "y", "z", "timeoutTicks", "next"));
        Map<String, String> arguments = new LinkedHashMap<>();
        if (reader.positionalCount() == 4) {
            put(arguments, "x", reader.positionalScalar(1, "Player.SitAt", "x"));
            put(arguments, "y", reader.positionalScalar(2, "Player.SitAt", "y"));
            put(arguments, "z", reader.positionalScalar(3, "Player.SitAt", "z"));
        } else {
            put(arguments, "x", reader.requiredNamedScalar("x", "Player.SitAt"));
            put(arguments, "y", reader.requiredNamedScalar("y", "Player.SitAt"));
            put(arguments, "z", reader.requiredNamedScalar("z", "Player.SitAt"));
        }
        put(arguments, "timeoutTicks", defaulted(reader, "timeoutTicks", "Player.SitAt", "40"));
        return action(reader, new ActionCall(span, "Player.SitAt", "echo_player_actor:sit_at", arguments));
    }

    public Optional<SourceNode> standUp(IntrinsicCallReader reader, SourceSpan span) {
        reader.requirePositionalCount("Player.StandUp", 1);
        reader.rejectUnknownNamed("Player.StandUp", Set.of("next", "output"));
        return action(reader, new ActionCall(span, "Player.StandUp", "echo_player_actor:stand_up", Map.of()));
    }

    private void putRotation(Map<String, String> arguments, IntrinsicCallReader reader) {
        if (reader.positionalCount() == 6) {
            put(arguments, "yaw", reader.positionalScalar(4, "Player.SpawnAt", "yaw"));
            put(arguments, "pitch", reader.positionalScalar(5, "Player.SpawnAt", "pitch"));
        } else {
            put(arguments, "yaw", reader.requiredNamedScalar("yaw", "Player.SpawnAt"));
            put(arguments, "pitch", reader.requiredNamedScalar("pitch", "Player.SpawnAt"));
        }
    }

    private void putCount(Map<String, String> arguments, IntrinsicCallReader reader) {
        Optional<String> count = reader.positionalCount() == 4
                ? reader.positionalScalar(3, "Player.SetHotbarItem", "count")
                : defaulted(reader, "count", "Player.SetHotbarItem", "1");
        put(arguments, "count", count);
    }

    private Optional<SourceNode> action(IntrinsicCallReader reader, ActionCall call) {
        return nodeFactory.node(reader, NodeSpec.action(call.span(), new NodeSpec.ActionFields(
                reader.requiredNamedString("next", call.intrinsic()), Optional.of(call.actionType()),
                reader.optionalNamedString("output", call.intrinsic()), call.arguments())));
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

    private void put(Map<String, String> map, String key, Optional<String> value) {
        value.ifPresent(present -> map.put(key, present));
    }

    private record ActionCall(SourceSpan span, String intrinsic, String actionType, Map<String, String> arguments) { }
}
