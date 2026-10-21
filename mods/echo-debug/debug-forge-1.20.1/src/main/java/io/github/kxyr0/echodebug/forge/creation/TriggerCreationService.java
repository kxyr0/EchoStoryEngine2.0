package io.github.kxyr0.echodebug.forge.creation;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.path.TriggerIdPlan;
import io.github.kxyr0.echodebug.core.path.TriggerIdPlanner;
import io.github.kxyr0.echodebug.core.path.TriggerTargetPath;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.core.source.TriggerSourceModel;
import io.github.kxyr0.echodebug.core.source.TriggerSourceRenderer;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echodebug.forge.integration.seating.SeatTriggerSourceFactory;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceValidationRequest;
import io.github.kxyr0.echotrigger.forge.api.authoring.TriggerAuthoringService;
import io.github.kxyr0.echotrigger.forge.api.authoring.TriggerAuthoringServiceBridge;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;

public final class TriggerCreationService {
    private final TriggerCreationLedger ledger = new TriggerCreationLedger();
    private final TriggerIdPlanner ids = new TriggerIdPlanner();
    private final TriggerSourceRenderer renderer = new TriggerSourceRenderer();
    private final AtomicTriggerSourceWriter writer = new AtomicTriggerSourceWriter();

    public TriggerCreationResult create(
            MinecraftServer server,
            TriggerSelectionSnapshot selection,
            String requestId,
            Target target
    ) {
        String arguments = selection.revision() + ":" + target;
        return ledger.apply(requestId, arguments,
                () -> createOnce(server, selection, target.namespace(), target.path(), target.story()));
    }

    private TriggerCreationResult createOnce(
            MinecraftServer server,
            TriggerSelectionSnapshot selection,
            String namespace,
            String path,
            String story
    ) {
        if (!selection.complete() || selection.dimension().isEmpty()) {
            return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_SELECTION_INCOMPLETE,
                    "Selection is incomplete.");
        }
        var idResult = ids.plan(namespace, path);
        if (!idResult.successful()) {
            return TriggerCreationResult.failure(idResult.code(), idResult.message());
        }
        TriggerIdPlan id = idResult.value().orElseThrow();
        final String source;
        try {
            source = selection.shape() == TriggerWandShapeMode.SEAT
                    ? seatSource(server, id, selection, story) : source(id, selection);
        } catch (IllegalArgumentException exception) {
            return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_VALIDATION_FAILED,
                    exception.getMessage());
        }
        return authoring(server).map(service -> writeValidated(service, id, source))
                .orElseGet(() -> TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_VALIDATION_FAILED,
                        "Echo Trigger authoring service is unavailable."));
    }

    private TriggerCreationResult writeValidated(
            TriggerAuthoringService service,
            TriggerIdPlan id,
            String source
    ) {
        var validation = service.validate(new TriggerSourceValidationRequest(id.triggerId(),
                id.path() + ".trigger.es", source));
        if (!validation.successful()) {
            return validationFailure(validation.diagnostics().toString(), validation.parserCalls(),
                    validation.binderCalls(), validation.sourceFingerprint());
        }
        return writeAndReload(service, id, source, new ValidationEvidence(validation.sourceFingerprint(),
                validation.parserCalls(), validation.binderCalls()));
    }

    private TriggerCreationResult writeAndReload(
            TriggerAuthoringService service,
            TriggerIdPlan id,
            String source,
            ValidationEvidence evidence
    ) {
        var target = TriggerTargetPath.plan(FMLPaths.GAMEDIR.get(), id);
        if (!target.successful()) {
            return TriggerCreationResult.failure(target.code(), target.message());
        }
        try {
            return reloadAfterWrite(service, id, source, evidence, target.value().orElseThrow());
        } catch (IOException exception) {
            return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_FILE_WRITE_FAILED,
                    exception.getMessage());
        }
    }

    private TriggerCreationResult reloadAfterWrite(
            TriggerAuthoringService service,
            TriggerIdPlan id,
            String source,
            ValidationEvidence evidence,
            TriggerTargetPath target
    ) throws IOException {
        AtomicTriggerSourceWriter.WriteResult write = writer.write(target.finalPath(), source);
        var reload = service.reload(id.triggerId());
        if (!reload.successful()) {
            writer.rollback(write.finalPath());
            return TriggerCreationResult.failure(DebugDiagnosticCode.DEBUG_TRIGGER_RELOAD_FAILED,
                    reload.diagnostics().toString());
        }
        return success(id, write.finalPath(), evidence, reload.catalogRevisionBefore(),
                reload.catalogRevisionAfter());
    }

    private String source(TriggerIdPlan id, TriggerSelectionSnapshot selection) {
        TriggerSourceModel model = TriggerSourceModel.player(id, selection.dimension().orElseThrow(),
                selection.shape(), selection.geometry().orElseThrow());
        return renderer.render(model);
    }

    private String seatSource(MinecraftServer server, TriggerIdPlan id,
                              TriggerSelectionSnapshot selection, String story) {
        if (!ModList.get().isLoaded("echo_seating")) {
            throw new IllegalArgumentException("Для выбора сиденья нужен Echo Seating.");
        }
        return SeatTriggerSourceFactory.create(server, id, selection.points().get(0), story);
    }

    private Optional<TriggerAuthoringService> authoring(MinecraftServer server) {
        var lookup = TriggerAuthoringServiceBridge.lookup(server);
        return lookup.resolved() ? lookup.service() : Optional.empty();
    }

    private static TriggerCreationResult validationFailure(
            String message,
            int parserCalls,
            int binderCalls,
            String fingerprint
    ) {
        return new TriggerCreationResult(false, DebugDiagnosticCode.DEBUG_TRIGGER_VALIDATION_FAILED, message,
                Optional.empty(), Optional.empty(), Optional.of(fingerprint), parserCalls, binderCalls, 0, 0);
    }

    private static TriggerCreationResult success(
            TriggerIdPlan id,
            Path path,
            ValidationEvidence evidence,
            long before,
            long after
    ) {
        return new TriggerCreationResult(true, DebugDiagnosticCode.OK, "Trigger created.", Optional.of(id.triggerId()
                .value()), Optional.of(path), Optional.of(evidence.fingerprint()), evidence.parserCalls(),
                evidence.binderCalls(), before, after);
    }

    private record ValidationEvidence(String fingerprint, int parserCalls, int binderCalls) {
    }

    public record Target(String namespace, String path, String story) {
    }
}
