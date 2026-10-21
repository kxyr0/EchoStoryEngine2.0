package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.MigrationReceipt;
import io.github.kxyr0.echosavemigration.data.MutableSaveCandidate;
import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import io.github.kxyr0.echosavemigration.graph.MigrationGraph;
import io.github.kxyr0.echosavemigration.graph.MigrationPlan;
import io.github.kxyr0.echosavemigration.persistence.codec.SaveCodec;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournal;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalState;
import io.github.kxyr0.echosavemigration.persistence.journal.MigrationJournalStore;
import io.github.kxyr0.echosavemigration.transaction.error.MigrationException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public final class MigrationCoordinator {
    private final MigrationGraph graph;
    private final SaveCodec codec;
    private final CandidateValidator validator;
    private final MigrationJournalStore journals;
    private final BackupService backups;
    private final MigrationFailureHook failureHook;
    private final FileHash hashes = new FileHash();
    private final Clock clock;

    public MigrationCoordinator(MigrationGraph graph, SaveCodec codec, CandidateValidator validator,
            MigrationJournalStore journals, Clock clock, MigrationFailureHook failureHook) {
        this.graph = graph;
        this.codec = codec;
        this.validator = validator;
        this.journals = journals;
        this.clock = clock;
        this.failureHook = failureHook;
        backups = new BackupService(clock, 5);
    }

    public MigrationReport migrate(MigrationRequest request) {
        try {
            SaveSnapshot source = codec.read(request.source());
            MigrationPlan plan = graph.plan(source.manifest().schema(), request.runtimeSchema());
            if (plan.steps().isEmpty()) return report(plan, Optional.empty(), MigrationReport.Status.CURRENT);
            SaveSnapshot candidate = apply(source, plan);
            if (request.dryRun()) {
                validate(candidate, request.runtimeSchema());
                return report(plan, Optional.empty(), MigrationReport.Status.DRY_RUN);
            }
            return commit(request, plan, candidate);
        } catch (MigrationException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new MigrationException("SAVE_MIGRATION_STEP_FAILED", failure.getMessage(), failure);
        }
    }

    public Optional<MigrationReport> recover(Path journalPath) {
        try {
            Optional<MigrationJournal> loaded = journals.read(journalPath);
            if (loaded.isEmpty() || loaded.orElseThrow().state() == MigrationJournalState.COMMITTED) {
                return Optional.empty();
            }
            MigrationJournal journal = loaded.orElseThrow();
            SaveSnapshot source = codec.read(journal.source());
            if (source.manifest().schema().equals(journal.to())) {
                write(journalPath, journal.transition(MigrationJournalState.COMMITTED, journal.candidateHash()));
                return Optional.of(recoveryReport(journal));
            }
            if (!Files.exists(journal.candidate())) {
                throw new MigrationException("SAVE_MIGRATION_RECOVERY_FAILED", "Migration candidate is missing.");
            }
            SaveSnapshot candidate = codec.read(journal.candidate());
            validate(candidate, journal.to());
            if (!hashes.sha256(journal.candidate()).equals(journal.candidateHash())) {
                throw new MigrationException("SAVE_MIGRATION_RECOVERY_FAILED", "Candidate hash mismatch.");
            }
            atomicReplace(journal.candidate(), journal.source());
            write(journalPath, journal.transition(MigrationJournalState.COMMITTED, journal.candidateHash()));
            return Optional.of(recoveryReport(journal));
        } catch (MigrationException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new MigrationException("SAVE_MIGRATION_RECOVERY_FAILED", failure.getMessage(), failure);
        }
    }

    private MigrationReport commit(MigrationRequest request, MigrationPlan plan, SaveSnapshot candidate)
            throws IOException {
        Path candidatePath = request.source().resolveSibling(request.source().getFileName() + ".echo-candidate");
        Files.createDirectories(request.journal().getParent());
        Path backup = backups.create(request.source(), request.backupRoot(), plan.source());
        MigrationJournal journal = new MigrationJournal(MigrationJournalState.PLANNED, request.source(), candidatePath,
                backup, plan.source(), plan.target(), plan.steps().stream().map(MigrationDefinition::id).toList(), "",
                Instant.now(clock));
        write(request.journal(), journal);
        journal = transition(request.journal(), journal, MigrationJournalState.BACKED_UP, "");
        journal = transition(request.journal(), journal, MigrationJournalState.APPLYING, "");
        validate(candidate, request.runtimeSchema());
        codec.write(candidatePath, candidate);
        String hash = hashes.sha256(candidatePath);
        journal = transition(request.journal(), journal, MigrationJournalState.CANDIDATE_WRITTEN, hash);
        SaveSnapshot verified = codec.read(candidatePath);
        if (!verified.equals(candidate)) {
            throw new MigrationException("SAVE_MIGRATION_VALIDATION_FAILED", "Candidate round-trip mismatch.");
        }
        journal = transition(request.journal(), journal, MigrationJournalState.VERIFIED, hash);
        atomicReplace(candidatePath, request.source());
        transition(request.journal(), journal, MigrationJournalState.COMMITTED, hash);
        return report(plan, Optional.of(backup), MigrationReport.Status.COMMITTED);
    }

    private SaveSnapshot apply(SaveSnapshot source, MigrationPlan plan) {
        MutableSaveCandidate candidate = new MutableSaveCandidate(source);
        for (MigrationDefinition step : plan.steps()) {
            boolean applied = source.receipts().stream().anyMatch(value -> value.migrationId().equals(step.id()));
            if (applied) continue;
            for (MigrationOperation operation : step.operations()) operation.apply(candidate);
            candidate.manifest(candidate.manifest().migrated(step.to(), step.revision()));
            candidate.receipt(new MigrationReceipt(step.id(), step.from(), step.to(),
                    source.manifest().contentFingerprint(), step.revision(), Instant.now(clock)));
        }
        return candidate.freeze();
    }

    private void validate(SaveSnapshot candidate, io.github.kxyr0.echosavemigration.api.SaveSchemaVersion expected) {
        CandidateValidator.ValidationResult result = validator.validate(candidate, expected);
        if (!result.valid() || !candidate.manifest().schema().equals(expected)) {
            throw new MigrationException("SAVE_MIGRATION_VALIDATION_FAILED", result.detail());
        }
    }

    private MigrationJournal transition(Path path, MigrationJournal journal, MigrationJournalState state, String hash)
            throws IOException {
        MigrationJournal next = journal.transition(state, hash);
        write(path, next);
        failureHook.after(state);
        return next;
    }

    private void write(Path path, MigrationJournal journal) throws IOException {
        journals.write(path, journal);
    }

    private void atomicReplace(Path candidate, Path source) throws IOException {
        try {
            Files.move(candidate, source, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException failure) {
            throw new MigrationException("SAVE_MIGRATION_COMMIT_FAILED", "Atomic replace is unavailable.", failure);
        }
    }

    private MigrationReport report(MigrationPlan plan, Optional<Path> backup, MigrationReport.Status status) {
        return new MigrationReport(plan.source(), plan.target(),
                plan.steps().stream().map(MigrationDefinition::id).toList(), plan.operationCount(), backup, status);
    }

    private MigrationReport recoveryReport(MigrationJournal journal) {
        return new MigrationReport(journal.from(), journal.to(), journal.migrations(), 0,
                Optional.of(journal.backup()), MigrationReport.Status.RECOVERED);
    }
}
