package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgeActionDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgeExtensionDispatchDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgeExtensionRegistrationDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgeHostDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgePersistenceDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog.ForgeResourceDiagnostics;
import java.time.Instant;

/**
 * Facade for Forge runtime diagnostic catalogs.
 */
public final class ForgeRuntimeDiagnostics {
    private final ForgeResourceDiagnostics resources = new ForgeResourceDiagnostics();
    private final ForgeHostDiagnostics host = new ForgeHostDiagnostics();
    private final ForgePersistenceDiagnostics persistence = new ForgePersistenceDiagnostics();
    private final ForgeActionDiagnostics actions = new ForgeActionDiagnostics();
    private final ForgeExtensionRegistrationDiagnostics extensionRegistration =
            new ForgeExtensionRegistrationDiagnostics();
    private final ForgeExtensionDispatchDiagnostics extensionDispatch = new ForgeExtensionDispatchDiagnostics();

    public StoryDiagnostic catalogInstalled(int storyCount, int sourceCount, Instant occurredAt) {
        return resources.catalogInstalled(storyCount, sourceCount, occurredAt);
    }

    public StoryDiagnostic catalogRejected(int sourceCount, int errorCount, Instant occurredAt) {
        return resources.catalogRejected(sourceCount, errorCount, occurredAt);
    }

    public StoryDiagnostic duplicateStory(
            StoryId storyId,
            String firstSource,
            String secondSource,
            Instant occurredAt) {
        return resources.duplicateStory(storyId, firstSource, secondSource, occurredAt);
    }

    public StoryDiagnostic resourceReadFailed(String sourceId, String failureCategory, Instant occurredAt) {
        return resources.resourceReadFailed(sourceId, failureCategory, occurredAt);
    }

    public StoryDiagnostic scriptFolderLimitExceeded(
            String folderId,
            int scriptCount,
            int limit,
            Instant occurredAt) {
        return resources.scriptFolderLimitExceeded(folderId, scriptCount, limit, occurredAt);
    }

    public StoryDiagnostic hostStarted(Instant occurredAt) {
        return host.hostStarted(occurredAt);
    }

    public StoryDiagnostic hostStopped(Instant occurredAt) {
        return host.hostStopped(occurredAt);
    }

    public StoryDiagnostic missingStory(StoryId storyId, ExecutionId executionId, Instant occurredAt) {
        return host.missingStory(storyId, executionId, occurredAt);
    }

    public StoryDiagnostic persistenceQueueRejected(ExecutionId executionId, Instant occurredAt) {
        return persistence.persistenceQueueRejected(executionId, occurredAt);
    }

    public StoryDiagnostic persistenceWorkerFailed(String failureCategory, Instant occurredAt) {
        return persistence.persistenceWorkerFailed(failureCategory, occurredAt);
    }

    public StoryDiagnostic incompatibleDefinition(StoryExecutionState state, Instant occurredAt) {
        return persistence.incompatibleDefinition(state, occurredAt);
    }

    public StoryDiagnostic externalCommandSuppressed(StoryExecutionState state, Instant occurredAt) {
        return persistence.externalCommandSuppressed(state, occurredAt);
    }

    public StoryDiagnostic commandHandlerMissing(ExecutionId executionId, String actionType, Instant occurredAt) {
        return actions.commandHandlerMissing(executionId, actionType, occurredAt);
    }

    public StoryDiagnostic extensionRegistrationOpened(Instant occurredAt) {
        return extensionRegistration.registrationOpened(occurredAt);
    }

    public StoryDiagnostic extensionRegistrationClosed(int factoryCount, Instant occurredAt) {
        return extensionRegistration.registrationClosed(factoryCount, occurredAt);
    }

    public StoryDiagnostic extensionDescriptorFailed(String failureCategory, Instant occurredAt) {
        return extensionRegistration.descriptorFailed(failureCategory, occurredAt);
    }

    public StoryDiagnostic extensionAccepted(String extensionId, int actionCount, Instant occurredAt) {
        return extensionRegistration.accepted(extensionId, actionCount, occurredAt);
    }

    public StoryDiagnostic incompatibleExtension(String extensionId, String requiredApi, Instant occurredAt) {
        return extensionRegistration.incompatible(extensionId, requiredApi, occurredAt);
    }

    public StoryDiagnostic duplicateExtension(String extensionId, Instant occurredAt) {
        return extensionRegistration.duplicateExtension(extensionId, occurredAt);
    }

    public StoryDiagnostic duplicateAction(String actionType, String extensionId, Instant occurredAt) {
        return extensionRegistration.duplicateAction(actionType, extensionId, occurredAt);
    }

    public StoryDiagnostic extensionSessionFailed(
            String extensionId,
            String failureCategory,
            Instant occurredAt) {
        return extensionRegistration.sessionFailed(extensionId, failureCategory, occurredAt);
    }

    public StoryDiagnostic extensionStartRejected(
            ExecutionId executionId,
            String actionType,
            String status,
            Instant occurredAt) {
        return extensionDispatch.startRejected(executionId, actionType, status, occurredAt);
    }

    public StoryDiagnostic extensionStartFailed(
            ExecutionId executionId,
            String actionType,
            String failureCategory,
            Instant occurredAt) {
        return extensionDispatch.startFailed(executionId, actionType, failureCategory, occurredAt);
    }

    public StoryDiagnostic extensionRecoveryBlocked(
            ExecutionId executionId,
            String actionType,
            Instant occurredAt) {
        return extensionDispatch.recoveryBlocked(executionId, actionType, occurredAt);
    }

    public StoryDiagnostic extensionRecoveryRejected(
            ExecutionId executionId,
            String actionType,
            String status,
            Instant occurredAt) {
        return extensionDispatch.recoveryRejected(executionId, actionType, status, occurredAt);
    }

    public StoryDiagnostic extensionCompletionRejected(
            ExecutionId executionId,
            String actionType,
            String acceptance,
            Instant occurredAt) {
        return extensionDispatch.completionRejected(executionId, actionType, acceptance, occurredAt);
    }
}
