package io.github.kxyr0.echoanimationauthoring.draft;
import io.github.kxyr0.echoanimationauthoring.api.discovery.AuthoringDiscoveryResult;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import io.github.kxyr0.echoanimationauthoring.api.service.AnimationAuthoringService;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftMutationResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenRequest;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftSaveResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringHotReloadResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringOperationResult;
import io.github.kxyr0.echoanimationauthoring.draft.discovery.AuthoringDiscoveryCatalog;
import io.github.kxyr0.echoanimationauthoring.draft.io.AtomicDraftWriter;
import io.github.kxyr0.echoanimationauthoring.draft.preview.AuthoringPreviewShelf;
import io.github.kxyr0.echoanimationauthoring.draft.recovery.DraftAutosaveStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
public final class DefaultAnimationAuthoringService implements AnimationAuthoringService {
    private final Supplier<Optional<Path>> workspace;
    private final AnimationClipDraftShelf drafts = new AnimationClipDraftShelf();
    private final AnimationClipSourceRenderer renderer = new AnimationClipSourceRenderer();
    private final AnimationClipSourceValidator validator = new AnimationClipSourceValidator();
    private final EchoAnimationCompilerProbe compiler = new EchoAnimationCompilerProbe();
    private final AtomicDraftWriter writer = new AtomicDraftWriter();
    private final DraftAutosaveStore autosaves = new DraftAutosaveStore();
    private final AuthoringPreviewShelf previews = new AuthoringPreviewShelf();
    private final AuthoringDiscoveryCatalog discovery = new AuthoringDiscoveryCatalog();
    public DefaultAnimationAuthoringService(Supplier<Optional<Path>> workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace, "workspace");
    }
    @Override
    public AuthoringDraftOpenResult openDraft(AuthoringDraftOpenRequest request) {
        Optional<Path> root = workspace.get();
        if (root.isEmpty()) {
            return new AuthoringDraftOpenResult(AuthoringDraftOpenResult.Status.WORKSPACE_NOT_CONFIGURED,
                    Optional.empty(), "Workspace не настроен.");
        }
        try {
            Path sourcePath = new DraftSourcePath().clipPath(root.orElseThrow(), request.clipId());
            AnimationClipDraft draft = drafts.open(sourcePath, request.clipId(), request.rigId());
            return new AuthoringDraftOpenResult(AuthoringDraftOpenResult.Status.OPENED,
                    Optional.of(draft.view()), "Authoring draft opened.");
        } catch (RuntimeException exception) {
            return new AuthoringDraftOpenResult(AuthoringDraftOpenResult.Status.INVALID_REQUEST,
                    Optional.empty(), exception.getMessage());
        }
    }
    @Override
    public AuthoringDraftMutationResult applyKeyframe(AuthoringDraftId id, AuthoringKeyframeMutation mutation) {
        return drafts.edit(id, draft -> draft.apply(mutation)).map(changed ->
                new AuthoringDraftMutationResult(AuthoringDraftMutationResult.Status.APPLIED,
                        Optional.of(changed.view()), "Keyframe mutation applied."))
                .orElseGet(() -> new AuthoringDraftMutationResult(AuthoringDraftMutationResult.Status.DRAFT_NOT_FOUND,
                        Optional.empty(), "Draft not found."));
    }
    @Override
    public AuthoringOperationResult undo(AuthoringDraftId id) {
        if (drafts.get(id).isEmpty()) {
            return draftNotFound();
        }
        return drafts.undo(id).map(changed -> operation(AuthoringOperationResult.Status.UNDONE,
                Optional.of(changed.view()), Optional.empty(), "Draft edit undone."))
                .orElseGet(() -> operation(AuthoringOperationResult.Status.NOTHING_TO_UNDO,
                        drafts.get(id).map(AnimationClipDraft::view), Optional.empty(), "Nothing to undo."));
    }
    @Override
    public AuthoringOperationResult redo(AuthoringDraftId id) {
        if (drafts.get(id).isEmpty()) {
            return draftNotFound();
        }
        return drafts.redo(id).map(changed -> operation(AuthoringOperationResult.Status.REDONE,
                Optional.of(changed.view()), Optional.empty(), "Draft edit redone."))
                .orElseGet(() -> operation(AuthoringOperationResult.Status.NOTHING_TO_REDO,
                        drafts.get(id).map(AnimationClipDraft::view), Optional.empty(), "Nothing to redo."));
    }
    @Override
    public AuthoringOperationResult autosave(AuthoringDraftId id) {
        return drafts.get(id).map(this::autosaveDraft).orElseGet(DefaultAnimationAuthoringService::draftNotFound);
    }
    @Override
    public AuthoringOperationResult recoverDraft(AuthoringDraftOpenRequest request) {
        Optional<Path> root = workspace.get();
        if (root.isEmpty()) {
            return operation(AuthoringOperationResult.Status.FAILURE, Optional.empty(), Optional.empty(),
                    "Workspace is not configured.");
        }
        try {
            return recover(root.orElseThrow(), request);
        } catch (java.io.IOException | RuntimeException exception) {
            return operation(AuthoringOperationResult.Status.FAILURE, Optional.empty(), Optional.empty(),
                    exception.getMessage());
        }
    }
    @Override
    public AuthoringOperationResult applyPreview(AuthoringDraftId id) {
        return drafts.get(id).map(draft -> {
            previews.apply(id, draft.snapshot());
            return operation(AuthoringOperationResult.Status.PREVIEW_APPLIED, Optional.of(draft.view()),
                    Optional.empty(), "Preview override applied.");
        }).orElseGet(DefaultAnimationAuthoringService::draftNotFound);
    }
    @Override
    public AuthoringOperationResult clearPreview(AuthoringDraftId id) {
        boolean cleared = previews.clear(id);
        return drafts.get(id).map(draft -> operation(AuthoringOperationResult.Status.PREVIEW_CLEARED,
                Optional.of(draft.view()), Optional.empty(), cleared ? "Preview override cleared."
                        : "Preview override was not active."))
                .orElseGet(DefaultAnimationAuthoringService::draftNotFound);
    }
    @Override
    public AuthoringDiscoveryResult discover(AuthoringDraftOpenRequest request) {
        return discovery.discover(request);
    }
    @Override
    public AuthoringDraftSaveResult save(AuthoringDraftId id) {
        return drafts.get(id).map(this::saveDraft).orElseGet(() ->
                saveFailure(AuthoringDraftSaveResult.Status.DRAFT_NOT_FOUND, Optional.empty(), "Draft not found."));
    }
    @Override
    public AuthoringHotReloadResult hotReload(AuthoringDraftId id) {
        return drafts.get(id)
                .map(this::hotReloadDraft)
                .orElseGet(() -> new AuthoringHotReloadResult(AuthoringHotReloadResult.Status.DRAFT_NOT_FOUND,
                        "Draft not found."));
    }
    @Override
    public Optional<AuthoringDraftView> draft(AuthoringDraftId draftId) {
        return drafts.get(draftId).map(AnimationClipDraft::view);
    }
    private AuthoringOperationResult autosaveDraft(AnimationClipDraft draft) {
        try {
            Path autosave = autosaves.write(draft.sourcePath(), draft.keyframes());
            return operation(AuthoringOperationResult.Status.AUTOSAVED, Optional.of(draft.view()),
                    Optional.of(autosave), "Draft autosaved.");
        } catch (java.io.IOException exception) {
            return operation(AuthoringOperationResult.Status.FAILURE, Optional.of(draft.view()),
                    Optional.empty(), exception.getMessage());
        }
    }
    private AuthoringOperationResult recover(Path root, AuthoringDraftOpenRequest request) throws java.io.IOException {
        Path sourcePath = new DraftSourcePath().clipPath(root, request.clipId());
        Optional<List<AuthoringKeyframeMutation>> keyframes = autosaves.read(sourcePath);
        if (keyframes.isEmpty()) {
            return operation(AuthoringOperationResult.Status.FAILURE, Optional.empty(),
                    Optional.of(autosaves.autosavePath(sourcePath)), "Autosave not found.");
        }
        AnimationClipDraft draft = drafts.open(sourcePath, request.clipId(), request.rigId())
                .replaceKeyframes(keyframes.orElseThrow());
        AnimationClipDraft recovered = drafts.replace(draft);
        return operation(AuthoringOperationResult.Status.RECOVERED, Optional.of(recovered.view()),
                Optional.of(autosaves.autosavePath(sourcePath)), "Draft recovered from autosave.");
    }
    private AuthoringDraftSaveResult saveDraft(AnimationClipDraft draft) {
        if (stale(draft)) {
            return saveFailure(AuthoringDraftSaveResult.Status.STALE_SOURCE, Optional.of(draft.view()),
                    "Source changed since draft open.");
        }
        String source = renderer.render(draft);
        String structuralError = validator.structuralError(source);
        if (!structuralError.isEmpty()) {
            return saveFailure(AuthoringDraftSaveResult.Status.VALIDATION_FAILED, Optional.of(draft.view()),
                    structuralError);
        }
        EchoAnimationCompilerProbe.ValidationResult validation = compiler.validate(draft.sourcePath().toString(),
                source);
        return validation.passed() ? writeDraft(draft, source, validation)
                : saveFailure(AuthoringDraftSaveResult.Status.VALIDATION_FAILED,
                        Optional.of(draft.view()), validation.message());
    }
    private AuthoringDraftSaveResult writeDraft(AnimationClipDraft draft, String source,
                                                EchoAnimationCompilerProbe.ValidationResult validation) {
        try {
            long sourceRevision = writer.write(draft.sourcePath(), source);
            AnimationClipDraft saved = drafts.replace(draft.saved(sourceRevision));
            return new AuthoringDraftSaveResult(AuthoringDraftSaveResult.Status.SAVED, Optional.of(saved.view()),
                    Optional.of(saved.sourcePath()), validation.parserPass(), validation.binderPass(),
                    validation.compilerPass(), "Draft saved; Echo Animation watcher will hot reload the file.");
        } catch (java.io.IOException exception) {
            return saveFailure(AuthoringDraftSaveResult.Status.FAILURE, Optional.of(draft.view()),
                    exception.getMessage());
        }
    }
    private AuthoringHotReloadResult hotReloadDraft(AnimationClipDraft draft) {
        if (draft.dirty() || !Files.exists(draft.sourcePath())) {
            return new AuthoringHotReloadResult(AuthoringHotReloadResult.Status.NOT_SAVED,
                    "Save the draft first; hot reload is file-change driven.");
        }
        return new AuthoringHotReloadResult(AuthoringHotReloadResult.Status.SCHEDULED_BY_FILE_CHANGE,
                "Echo Animation hot reload is scheduled by the saved .animation-clip.es file change.");
    }
    private static boolean stale(AnimationClipDraft draft) {
        try {
            return Files.exists(draft.sourcePath()) && draft.sourceRevision() >= 0L
                    && Files.getLastModifiedTime(draft.sourcePath()).toMillis() != draft.sourceRevision();
        } catch (java.io.IOException exception) {
            return true;
        }
    }
    private static AuthoringDraftSaveResult saveFailure(AuthoringDraftSaveResult.Status status,
                                                        Optional<AuthoringDraftView> draft, String message) {
        return new AuthoringDraftSaveResult(status, draft, Optional.empty(), false, false, false, message);
    }
    private static AuthoringOperationResult operation(AuthoringOperationResult.Status status,
                                                      Optional<AuthoringDraftView> draft,
                                                      Optional<Path> path, String message) {
        return new AuthoringOperationResult(status, draft, path, message);
    }
    private static AuthoringOperationResult draftNotFound() {
        return operation(AuthoringOperationResult.Status.DRAFT_NOT_FOUND, Optional.empty(),
                Optional.empty(), "Draft not found.");
    }
}
