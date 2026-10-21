package io.github.kxyr0.echoevents.persistence.codec;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.payload.AnimationFact;
import io.github.kxyr0.echoevents.api.payload.CutsceneFact;
import io.github.kxyr0.echoevents.api.payload.DialogueFact;
import io.github.kxyr0.echoevents.api.payload.EventPayload;
import io.github.kxyr0.echoevents.api.payload.NpcFact;
import io.github.kxyr0.echoevents.api.payload.ObservationFact;
import io.github.kxyr0.echoevents.api.payload.SceneFact;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public final class EventPayloadCodec {
    public void write(DataOutput output, EventPayload payload) throws IOException {
        output.writeUTF(payload.kind().stableId());
        if (payload instanceof DialogueFact dialogue) {
            if (dialogue.choice().isPresent()) output.writeUTF(dialogue.choice().orElseThrow().value());
            if (dialogue.failure().isPresent()) output.writeUTF(dialogue.failure().orElseThrow().value());
        } else if (payload instanceof SceneFact scene) {
            if (scene.failure().isPresent()) output.writeUTF(scene.failure().orElseThrow().value());
        } else if (payload instanceof ObservationFact observation) output.writeUTF(observation.completion().value());
        else if (payload instanceof NpcFact npc) {
            output.writeUTF(npc.actor().value());
            if (npc.kind() == EventKind.NPC_ARRIVED) {
                output.writeBoolean(npc.destination().isPresent());
                if (npc.destination().isPresent()) output.writeUTF(npc.destination().orElseThrow().value());
                else {
                    var position = npc.position().orElseThrow();
                    output.writeUTF(position.dimension());
                    output.writeInt(position.x());
                    output.writeInt(position.y());
                    output.writeInt(position.z());
                }
            }
        } else if (payload instanceof EventPayload.TeleportFact teleport) {
            output.writeUTF(teleport.subject().value());
        } else if (payload instanceof AnimationFact animation) {
            output.writeUTF(animation.subject().value());
            output.writeUTF(animation.playback().value());
            if (animation.marker().isPresent()) output.writeUTF(animation.marker().orElseThrow().value());
        }
    }
    public EventPayload read(DataInput input) throws IOException {
        EventKind kind = EventKind.fromStableId(input.readUTF());
        return switch (kind) {
            case DIALOGUE_STARTED -> DialogueFact.lifecycle(DialogueFact.Phase.STARTED);
            case DIALOGUE_CHOICE -> DialogueFact.choice(new SymbolId(input.readUTF()));
            case DIALOGUE_COMPLETED -> DialogueFact.lifecycle(DialogueFact.Phase.COMPLETED);
            case DIALOGUE_CANCELLED -> DialogueFact.lifecycle(DialogueFact.Phase.CANCELLED);
            case DIALOGUE_FAILED -> DialogueFact.failed(new SymbolId(input.readUTF()));
            case SCENE_STARTED -> new SceneFact(SceneFact.Phase.STARTED, Optional.empty());
            case SCENE_COMPLETED -> new SceneFact(SceneFact.Phase.COMPLETED, Optional.empty());
            case SCENE_CANCELLED -> new SceneFact(SceneFact.Phase.CANCELLED, Optional.empty());
            case SCENE_FAILED -> new SceneFact(SceneFact.Phase.FAILED, Optional.of(new SymbolId(input.readUTF())));
            case CUTSCENE_STARTED -> new CutsceneFact(CutsceneFact.Phase.STARTED);
            case CUTSCENE_COMPLETED -> new CutsceneFact(CutsceneFact.Phase.COMPLETED);
            case CUTSCENE_CANCELLED -> new CutsceneFact(CutsceneFact.Phase.CANCELLED);
            case OBSERVATION_COMPLETED -> new ObservationFact(new SymbolId(input.readUTF()));
            case NPC_ARRIVED -> readArrival(input);
            case NPC_DIED -> new NpcFact(new SymbolId(input.readUTF()), Optional.empty());
            case ANIMATION_MARKER -> new AnimationFact(new SymbolId(input.readUTF()), new SymbolId(input.readUTF()),
                    Optional.of(new SymbolId(input.readUTF())));
            case ANIMATION_COMPLETED -> new AnimationFact(new SymbolId(input.readUTF()), new SymbolId(input.readUTF()),
                    Optional.empty());
            case TELEPORT_STARTED, TELEPORT_COMMITTED, TELEPORT_COMPLETED, TELEPORT_CANCELLED, TELEPORT_FAILED ->
                    new EventPayload.TeleportFact(kind, new SymbolId(input.readUTF()));
        };
    }
    private NpcFact readArrival(DataInput input) throws IOException {
        var actor = new SymbolId(input.readUTF());
        return input.readBoolean() ? new NpcFact(actor, Optional.of(new ResourceId(input.readUTF())))
                : new NpcFact(actor, Optional.empty(), Optional.of(new NpcFact.Position(input.readUTF(), input.readInt(),
                        input.readInt(), input.readInt())));
    }
}
