package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionResult;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.core.script.EchoScriptWeaponCompiler;
import io.github.kxyr0.echoitem.core.source.ItemSourceCandidate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compiles changed weapon candidates and records parser/binder call counts.
 */
public final class ItemCandidateCompiler {
    private final EchoScriptWeaponCompiler compiler;

    public ItemCandidateCompiler(EchoScriptWeaponCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public CompiledCandidates compile(List<ItemSourceCandidate> sourceCandidates, ItemDefinitionRevision revision) {
        List<CompiledWeaponCandidate> compiledCandidates = new ArrayList<>();
        List<ItemDefinitionDiagnostic> diagnostics = new ArrayList<>();
        for (ItemSourceCandidate candidate : sourceCandidates) {
            ItemDefinitionResult<WeaponDefinition> result = compiler.compile(
                    candidate.sourceId(), candidate.sourceText(), revision);
            result.definition().map(definition -> new CompiledWeaponCandidate(
                    candidate.sourceId(), candidate.origin(), definition)).ifPresent(compiledCandidates::add);
            diagnostics.addAll(result.diagnostics());
        }
        return new CompiledCandidates(compiledCandidates, diagnostics, ReloadCounters.weapon(sourceCandidates.size()));
    }

    public record CompiledCandidates(
            List<CompiledWeaponCandidate> candidates,
            List<ItemDefinitionDiagnostic> diagnostics,
            ReloadCounters counters
    ) {
        public CompiledCandidates {
            candidates = List.copyOf(candidates);
            diagnostics = List.copyOf(diagnostics);
            counters = Objects.requireNonNull(counters, "counters");
        }
    }
}
