package io.github.kxyr0.echostoryengine.story.forge.environment.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.time.Instant;
import java.util.Map;

final class ForgeEnvironmentResourceDiagnostics {
    StoryDiagnostic catalogInstalled(int environmentCount, int sourceCount, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-001", DiagnosticSeverity.INFO,
                "Environment resource catalog installed.",
                Map.of("environments", Integer.toString(environmentCount), "sources", Integer.toString(sourceCount)),
                "environment-catalog-installed", "No operator action required."), SourceLocation.unknown(),
                occurredAt);
    }

    StoryDiagnostic catalogRejected(int sourceCount, int errorCount, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-002", DiagnosticSeverity.ERROR,
                "Environment resource catalog candidate was rejected.",
                Map.of("sources", Integer.toString(sourceCount), "errors", Integer.toString(errorCount)),
                "environment-catalog-rejected", "Fix environment .es resources and run reload again."),
                SourceLocation.unknown(), occurredAt);
    }

    StoryDiagnostic duplicateEnvironment(DuplicateEnvironment duplicate, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-003", DiagnosticSeverity.ERROR,
                "Duplicate Environment ID was found.",
                Map.of("environmentId", duplicate.environmentId(), "firstSource", duplicate.firstSource(),
                        "secondSource", duplicate.secondSource()),
                "duplicate-environment-id", "Keep one EnvironmentResource source for this id."),
                duplicate.location(), occurredAt);
    }

    StoryDiagnostic unknownBlock(BlockFailure failure, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-004", DiagnosticSeverity.ERROR,
                "Environment references unknown block '" + failure.blockId() + "'.",
                Map.of("environmentId", failure.environmentId(), "blockId", failure.blockId()),
                "unknown-block", "Use a block registry id available in the current mod set."),
                failure.location(), occurredAt);
    }

    StoryDiagnostic unknownProperty(PropertyFailure failure, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-005", DiagnosticSeverity.ERROR,
                "Block '" + failure.blockId() + "' does not contain property '" + failure.property() + "'.",
                propertyContext(failure), "unknown-block-property",
                "Use an existing block state property for this block."), failure.location(), occurredAt);
    }

    StoryDiagnostic invalidPropertyValue(PropertyFailure failure, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-006", DiagnosticSeverity.ERROR,
                "Property '" + failure.property() + "' does not accept value '" + failure.value() + "'.",
                propertyContext(failure), "invalid-block-property-value",
                "Use one of the values accepted by the target block property."), failure.location(), occurredAt);
    }

    StoryDiagnostic invalidNbt(NbtFailure failure, Instant occurredAt) {
        return create(new Seed("ESE-FORGE-ENV-007", DiagnosticSeverity.ERROR,
                "Environment contains invalid block entity NBT.",
                Map.of("environmentId", failure.environmentId(), "blockId", failure.blockId(),
                        "failureCategory", failure.failureCategory()),
                "invalid-block-entity-nbt", "Fix the SNBT string or remove nbt from SetBlock."),
                failure.location(), occurredAt);
    }

    private StoryDiagnostic create(Seed seed, SourceLocation location, Instant occurredAt) {
        return new StoryDiagnostic(seed.code(), seed.severity(), "forge-environment", seed.message(),
                subject(location), seed.context(), seed.cause(), occurredAt, seed.hint());
    }

    private DiagnosticSubject subject(SourceLocation location) {
        return new DiagnosticSubject("", "", "", "", "", location.isKnown() ? location.asSubjectValue() : "");
    }

    private Map<String, String> propertyContext(PropertyFailure failure) {
        return Map.of("environmentId", failure.environmentId(), "blockId", failure.blockId(),
                "property", failure.property(), "value", failure.value());
    }

    record DuplicateEnvironment(
            String environmentId,
            String firstSource,
            String secondSource,
            SourceLocation location) {
    }

    record BlockFailure(String environmentId, String blockId, SourceLocation location) {
    }

    record PropertyFailure(
            String environmentId,
            String blockId,
            String property,
            String value,
            SourceLocation location) {
    }

    record NbtFailure(String environmentId, String blockId, String failureCategory, SourceLocation location) {
    }

    private record Seed(
            String code,
            DiagnosticSeverity severity,
            String message,
            Map<String, String> context,
            String cause,
            String hint) {
    }
}
