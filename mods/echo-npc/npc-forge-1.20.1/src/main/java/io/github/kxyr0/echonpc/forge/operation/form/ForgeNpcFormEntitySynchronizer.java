package io.github.kxyr0.echonpc.forge.operation.form;

import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.host.ForgeActorMaterializer;
import java.util.Optional;

final class ForgeNpcFormEntitySynchronizer {
    private final ForgeActorMaterializer materializer;

    ForgeNpcFormEntitySynchronizer(ForgeActorMaterializer materializer) {
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
    }

    Result sync(ActiveActor actor, NpcFormDefinition form) {
        Optional<NpcDefinition> definition = actor.definition();
        if (definition.isEmpty()) {
            return new Result(false, "definition=false");
        }
        Optional<PlatformActorHandle> handle = actor.handle();
        if (handle.isEmpty()) {
            return new Result(false, "handle=false");
        }
        Optional<EchoNpcEntity> entity = handle.flatMap(materializer::findEntity);
        if (entity.isEmpty()) {
            return new Result(false, "entity=false handle=" + handle.orElseThrow().value());
        }
        EchoNpcEntity live = entity.orElseThrow();
        live.configure(actor.desired().actorId(), actor.desired().entityBindingId(),
                definition.orElseThrow(), Optional.of(form), actor.desired().activeSkinId());
        if (actor.desired().restAttachment().isEmpty()) {
            live.clearSleeping();
        }
        return new Result(true, debug(live));
    }

    private String debug(EchoNpcEntity entity) {
        return "entity=true uuid=" + entity.getUUID()
                + " entityForm=" + blank(entity.formIdValue())
                + " entityScale=" + entity.bodyScaleValue()
                + " entitySize=" + entity.getBbWidth() + "x" + entity.getBbHeight()
                + " entitySkin=" + blank(entity.skinIdValue())
                + " sleeping=" + entity.sleepingValue();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    record Result(boolean applied, String message) {
        Result {
            message = java.util.Objects.requireNonNull(message, "message");
        }
    }
}
