package io.github.kxyr0.echoplayeractor.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerActorScriptBinder {
    public Optional<PlayerActorScriptCommand> bind(String scriptName, Map<String, String> arguments) {
        Optional<String> waitType = PlayerActorScriptCommands.waitType(scriptName);
        if (waitType.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>(arguments);
        normalizePositionals(waitType.orElseThrow(), payload);
        payload.keySet().removeIf(key -> key.startsWith("arg"));
        boolean blocking = !PlayerActorScriptCommands.predicate(waitType.orElseThrow());
        return Optional.of(new PlayerActorScriptCommand(waitType.orElseThrow(),
                PlayerActorScriptPayloads.encode(payload), blocking));
    }

    private void normalizePositionals(String waitType, Map<String, String> payload) {
        if (PlayerActorScriptCommands.SPAWN_AT.equals(waitType)) {
            copy(payload, "arg0", "x");
            copy(payload, "arg1", "y");
            copy(payload, "arg2", "z");
            copy(payload, "arg3", "yaw");
            copy(payload, "arg4", "pitch");
        }
        if (PlayerActorScriptCommands.SET_HOTBAR_ITEM.equals(waitType)) {
            copy(payload, "arg0", "slot");
            copy(payload, "arg1", "itemId");
        }
        if (PlayerActorScriptCommands.SELECT_HOTBAR_SLOT.equals(waitType)) {
            copy(payload, "arg0", "slot");
        }
        if (PlayerActorScriptCommands.SIT_AT.equals(waitType)) {
            copy(payload, "arg0", "x");
            copy(payload, "arg1", "y");
            copy(payload, "arg2", "z");
        }
        if (PlayerActorScriptCommands.ROTATE.equals(waitType)) {
            copy(payload, "arg0", "yaw");
            copy(payload, "arg1", "pitch");
        }
        if (PlayerActorScriptCommands.LOOK_AT.equals(waitType)
                || PlayerActorScriptCommands.MOVE.equals(waitType)) {
            copy(payload, "arg0", "x");
            copy(payload, "arg1", "y");
            copy(payload, "arg2", "z");
        }
        if (PlayerActorScriptCommands.INTERACT.equals(waitType)) {
            copy(payload, "arg0", "x");
            copy(payload, "arg1", "y");
            copy(payload, "arg2", "z");
        }
    }

    private void copy(Map<String, String> values, String from, String to) {
        if (!values.containsKey(to) && values.containsKey(from)) {
            values.put(to, values.get(from));
        }
    }
}
