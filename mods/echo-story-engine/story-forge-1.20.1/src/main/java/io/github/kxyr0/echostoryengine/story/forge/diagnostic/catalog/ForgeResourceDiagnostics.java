package io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticParts;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted while discovering and installing story resources.
 */
public final class ForgeResourceDiagnostics {
    public StoryDiagnostic catalogInstalled(int storyCount, int sourceCount, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-RESOURCE-001",
                DiagnosticSeverity.INFO,
                "forge-resource",
                "Story resource catalog installed.",
                emptySubject(),
                Map.of("stories", Integer.toString(storyCount), "sources", Integer.toString(sourceCount)),
                "catalog-installed",
                occurredAt,
                "No operator action required."));
    }

    public StoryDiagnostic catalogRejected(int sourceCount, int errorCount, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-RESOURCE-002",
                DiagnosticSeverity.ERROR,
                "forge-resource",
                "Story resource catalog candidate was rejected.",
                emptySubject(),
                Map.of("sources", Integer.toString(sourceCount), "errors", Integer.toString(errorCount)),
                "catalog-rejected",
                occurredAt,
                "Fix datapack .es resources and run reload again."));
    }

    public StoryDiagnostic duplicateStory(
            StoryId storyId,
            String firstSource,
            String secondSource,
            Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-RESOURCE-003",
                DiagnosticSeverity.ERROR,
                "forge-resource",
                "Duplicate Story ID with a different fingerprint was found.",
                new DiagnosticSubject(storyId.asString(), "", "", "", storyId.asString(), secondSource),
                Map.of("firstSource", firstSource, "secondSource", secondSource),
                "duplicate-story-id",
                occurredAt,
                "Keep one StoryResource source or make duplicate definitions identical."));
    }

    public StoryDiagnostic resourceReadFailed(String sourceId, String failureCategory, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-RESOURCE-004",
                DiagnosticSeverity.ERROR,
                "forge-resource",
                "Unable to read .es resource.",
                new DiagnosticSubject("", "", "", "", sourceId, sourceId),
                Map.of("failureCategory", failureCategory),
                "resource-read-failed",
                occurredAt,
                "Check the datapack resource and UTF-8 encoding."));
    }

    public StoryDiagnostic scriptFolderLimitExceeded(
            String folderId,
            int scriptCount,
            int limit,
            Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-RESOURCE-005",
                DiagnosticSeverity.ERROR,
                "forge-resource",
                "Story script folder contains too many .es scripts.",
                new DiagnosticSubject("", "", "", "", folderId, folderId),
                Map.of("folder", folderId, "scripts", Integer.toString(scriptCount), "limit", Integer.toString(limit)),
                "script-folder-limit",
                occurredAt,
                "Move scripts into additional folders so each folder has at most the configured limit."));
    }

    private DiagnosticSubject emptySubject() {
        return new DiagnosticSubject("", "", "", "", "", "");
    }
}
