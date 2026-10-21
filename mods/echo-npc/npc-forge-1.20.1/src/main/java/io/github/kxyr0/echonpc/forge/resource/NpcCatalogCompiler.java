package io.github.kxyr0.echonpc.forge.resource;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.definition.NpcDefinitionCatalog;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.EchoScriptNpcFormCompiler;
import io.github.kxyr0.echonpc.echoscript.EchoScriptNpcCompiler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compiles reload candidates into one immutable candidate catalog.
 */
public final class NpcCatalogCompiler {
    private final EchoScriptNpcCompiler compiler;
    private final EchoScriptNpcFormCompiler formCompiler;
    private final Map<String, CachedNpcCompile> cache = new HashMap<>();
    private final Map<String, CachedFormCompile> formCache = new HashMap<>();

    public NpcCatalogCompiler(EchoScriptNpcCompiler compiler) {
        this(compiler, new EchoScriptNpcFormCompiler());
    }

    public NpcCatalogCompiler(EchoScriptNpcCompiler compiler, EchoScriptNpcFormCompiler formCompiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.formCompiler = Objects.requireNonNull(formCompiler, "formCompiler");
    }

    public synchronized Result compile(List<NpcResourceCandidate> candidates, long revision) {
        return compile(candidates, List.of(), revision);
    }

    public synchronized Result compile(
            List<NpcResourceCandidate> candidates,
            List<NpcResourceCandidate> formCandidates,
            long revision
    ) {
        List<NpcDefinition> definitions = new ArrayList<>();
        List<NpcFormDefinition> forms = new ArrayList<>();
        List<NpcDiagnostic> diagnostics = new ArrayList<>();
        Set<NpcDefinitionId> seen = new HashSet<>();
        Set<NpcFormId> seenForms = new HashSet<>();
        retain(candidates, formCandidates);
        for (NpcResourceCandidate candidate : candidates) {
            CachedNpcCompile result = cached(candidate);
            diagnostics.addAll(result.diagnostics());
            result.definition().ifPresent(definition -> add(definition, seen, definitions, diagnostics));
        }
        for (NpcResourceCandidate candidate : formCandidates) {
            CachedFormCompile result = cachedForm(candidate, revision);
            diagnostics.addAll(result.diagnostics());
            result.form().ifPresent(form -> addForm(form, seenForms, forms, diagnostics));
        }
        validateDefaultForms(definitions, seenForms, diagnostics);
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            return new Result(java.util.Optional.empty(), diagnostics);
        }
        return new Result(java.util.Optional.of(NpcDefinitionCatalog.of(revision, definitions, forms)), diagnostics);
    }

    private void add(NpcDefinition definition, Set<NpcDefinitionId> seen, List<NpcDefinition> definitions,
            List<NpcDiagnostic> diagnostics) {
        if (!seen.add(definition.id())) {
            diagnostics.add(NpcDiagnostic.error("ENPC-RESOURCE-DUPLICATE-ID", "NpcDefinitionId повторяется.",
                    definition.id().value()));
            return;
        }
        definitions.add(definition);
    }

    private void addForm(NpcFormDefinition form, Set<NpcFormId> seen, List<NpcFormDefinition> forms,
            List<NpcDiagnostic> diagnostics) {
        if (!seen.add(form.id())) {
            diagnostics.add(NpcDiagnostic.error("NPC_FORM_ID_CONFLICT", "NpcFormId повторяется.",
                    form.id().value()));
            return;
        }
        forms.add(form);
    }

    private void validateDefaultForms(List<NpcDefinition> definitions, Set<NpcFormId> forms,
            List<NpcDiagnostic> diagnostics) {
        for (NpcDefinition definition : definitions) {
            definition.defaultFormId().filter(formId -> !forms.contains(formId)).ifPresent(formId ->
                    diagnostics.add(NpcDiagnostic.error("NPC_FORM_NOT_FOUND",
                            "DefaultForm указывает на отсутствующую форму.", formId.value())));
        }
    }

    private CachedNpcCompile cached(NpcResourceCandidate candidate) {
        CachedNpcCompile current = cache.get(candidate.sourceId());
        if (current != null && current.matches(candidate.sourceText())) {
            return current;
        }
        var result = compiler.compile(candidate.sourceId(), candidate.sourceText());
        CachedNpcCompile next = new CachedNpcCompile(
                candidate.sourceText(),
                result.definition(),
                result.diagnostics());
        cache.put(candidate.sourceId(), next);
        return next;
    }

    private CachedFormCompile cachedForm(NpcResourceCandidate candidate, long revision) {
        CachedFormCompile current = formCache.get(candidate.sourceId());
        if (current != null && current.matches(candidate.sourceText())) {
            return current;
        }
        var result = formCompiler.compile(candidate.sourceId(), candidate.sourceText(),
                new NpcFormRevision(revision));
        CachedFormCompile next = new CachedFormCompile(candidate.sourceText(), revision, result.form(),
                result.diagnostics());
        formCache.put(candidate.sourceId(), next);
        return next;
    }

    private void retain(List<NpcResourceCandidate> candidates, List<NpcResourceCandidate> formCandidates) {
        Set<String> sourceIds = new HashSet<>();
        for (NpcResourceCandidate candidate : candidates) {
            sourceIds.add(candidate.sourceId());
        }
        cache.keySet().retainAll(sourceIds);
        Set<String> formSourceIds = new HashSet<>();
        for (NpcResourceCandidate candidate : formCandidates) {
            formSourceIds.add(candidate.sourceId());
        }
        formCache.keySet().retainAll(formSourceIds);
    }

    public record Result(Optional<NpcDefinitionCatalog> catalog, List<NpcDiagnostic> diagnostics) {
        public Result {
            catalog = Objects.requireNonNull(catalog, "catalog");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }
    }

    private record CachedNpcCompile(
            String sourceText,
            Optional<NpcDefinition> definition,
            List<NpcDiagnostic> diagnostics) {
        private CachedNpcCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            definition = Objects.requireNonNull(definition, "definition");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }

    private record CachedFormCompile(
            String sourceText,
            long revision,
            Optional<NpcFormDefinition> form,
            List<NpcDiagnostic> diagnostics) {
        private CachedFormCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            form = Objects.requireNonNull(form, "form");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }
}
