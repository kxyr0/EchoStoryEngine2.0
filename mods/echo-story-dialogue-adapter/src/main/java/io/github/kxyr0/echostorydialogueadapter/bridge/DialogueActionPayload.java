package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Exact D3 `echo_dialogue:play` argument schema.
 */
record DialogueActionPayload(
        DialogueSessionId sessionId,
        DialogueId dialogueId,
        AudienceId audienceId
) {
    private static final String DIALOGUE_ID = "dialogueId";
    private static final String AUDIENCE_ID = "audienceId";
    private static final Set<String> ALLOWED_KEYS = Set.of(DIALOGUE_ID, AUDIENCE_ID);
    private static final Set<String> LOCAL_AUDIENCES = Set.of("jdh:local_author", "local_author", "local", "self", "@s",
            "echo_content_player:self", "echo_content_player:player");

    static DialogueActionPayload decode(Map<String, String> payload, ActionExecutionId actionExecutionId) {
        return decode(payload, null, actionExecutionId, null);
    }

    static DialogueActionPayload decode(Map<String, String> payload, ActionExecutionId actionExecutionId,
            MinecraftServer server) {
        return decode(payload, null, actionExecutionId, server);
    }

    static DialogueActionPayload decode(Map<String, String> payload, ExecutionId executionId,
            ActionExecutionId actionExecutionId, MinecraftServer server) {
        Map<String, String> checked = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        if (!checked.containsKey(DIALOGUE_ID) || !ALLOWED_KEYS.containsAll(checked.keySet())) {
            throw new IllegalArgumentException("Expected dialogueId and optional audienceId arguments.");
        }
        return new DialogueActionPayload(
                DialogueBridgeIds.dialogueSessionId(actionExecutionId),
                DialogueId.parse(checked.get(DIALOGUE_ID)),
                audienceId(Optional.ofNullable(checked.get(AUDIENCE_ID)), executionId, server));
    }

    private static AudienceId audienceId(Optional<String> explicit, ExecutionId executionId,
            MinecraftServer server) {
        if (explicit.isPresent() && !LOCAL_AUDIENCES.contains(explicit.orElseThrow())) {
            return new AudienceId(explicit.orElseThrow());
        }
        Optional<UUID> inherited = executionPlayer(executionId);
        if (inherited.isPresent()) {
            return new AudienceId(inherited.orElseThrow().toString());
        }
        List<ServerPlayer> players = server == null ? List.of() : server.getPlayerList().getPlayers();
        if (players.size() != 1) {
            throw new IllegalArgumentException("Current dialogue player could not be resolved.");
        }
        return new AudienceId(players.get(0).getUUID().toString());
    }

    private static Optional<UUID> executionPlayer(ExecutionId executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        String value = executionId.asString();
        String marker = "/player/";
        int markerIndex = value.indexOf(marker);
        if (markerIndex < 0) {
            return Optional.empty();
        }
        int start = markerIndex + marker.length();
        int slash = value.indexOf('/', start);
        String raw = slash < 0 ? value.substring(start) : value.substring(start, slash);
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
