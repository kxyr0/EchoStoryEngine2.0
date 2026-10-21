package io.github.kxyr0.echodialogue.forge.resource;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.echoscript.DialogueCompileResult;
import io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueCompiler;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compiles all discovered resources into one candidate catalog.
 */
public final class DialogueResourceCandidateCompiler {
    private final EchoScriptDialogueCompiler compiler;
    private final Map<String, CachedDialogueCompile> cache = new HashMap<>();

    public DialogueResourceCandidateCompiler(EchoScriptDialogueCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public synchronized DialogueCatalogCandidate compile(DialogueResourceReadResult read) {
        List<CompiledDialogue> dialogues = new ArrayList<>();
        List<DialogueDiagnostic> diagnostics = new ArrayList<>(read.diagnostics());
        Map<String, DialogueId> sourceDialogueIds = new LinkedHashMap<>();
        retain(read.resources());
        for (DialogueResourceText resource : read.resources()) {
            CachedDialogueCompile result = cached(resource);
            result.dialogue().ifPresent(dialogue -> {
                dialogues.add(dialogue);
                sourceDialogueIds.put(resource.sourceId(), dialogue.id());
            });
            diagnostics.addAll(result.diagnostics());
        }
        return new DialogueCatalogCandidate(dialogues, sourceDialogueIds, diagnostics);
    }

    private CachedDialogueCompile cached(DialogueResourceText resource) {
        CachedDialogueCompile current = cache.get(resource.sourceId());
        if (current != null && current.matches(resource.text())) {
            return current;
        }
        DialogueCompileResult result = compiler.compile(resource.sourceId(), resource.text());
        CachedDialogueCompile next = new CachedDialogueCompile(
                resource.text(),
                result.dialogue(),
                result.diagnostics());
        cache.put(resource.sourceId(), next);
        return next;
    }

    private void retain(List<DialogueResourceText> resources) {
        List<String> sourceIds = resources.stream()
                .map(DialogueResourceText::sourceId)
                .toList();
        cache.keySet().retainAll(sourceIds);
    }

    private record CachedDialogueCompile(
            String sourceText,
            Optional<CompiledDialogue> dialogue,
            List<DialogueDiagnostic> diagnostics) {
        private CachedDialogueCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            dialogue = Objects.requireNonNull(dialogue, "dialogue");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }
}
