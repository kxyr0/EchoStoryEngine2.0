package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentNode;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueCompiler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DialogueSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_dialogue/dialogues/") && value.endsWith(".dialogue.es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var result = new EchoScriptDialogueCompiler().compile(sourceId, sourceText);
        List<ContentSourceProblem> problems = result.diagnostics().stream()
                .filter(value -> value.severity() != DiagnosticSeverity.INFO)
                .map(value -> new ContentSourceProblem(
                        value.severity() == DiagnosticSeverity.ERROR ? ContentSeverity.ERROR : ContentSeverity.WARNING,
                        value.code(), SourceIdentities.inferred(sourceId), sourceId, sourceText,
                        value.subject().flatMap(subject -> subject.line()).orElse(1),
                        value.subject().flatMap(subject -> subject.column()).orElse(1), value.message())).toList();
        return result.dialogue().map(value -> new AdaptedSource(List.of(descriptor(sourceId, sourceText, value)),
                problems)).orElseGet(() -> new AdaptedSource(List.of(), problems));
    }

    private ContentDescriptor descriptor(String sourceId, String text, CompiledDialogue dialogue) {
        ContentResourceId id = new ContentResourceId(dialogue.id().toString());
        List<ContentReference> references = new ArrayList<>();
        List<ContentNode> nodes = dialogue.nodes().stream().map(node -> node(sourceId, text, dialogue, node,
                references)).toList();
        return ContentDescriptor.simple(id, ContentResourceKind.DIALOGUE, SourceSpans.file(sourceId, text), references)
                .withNodes(nodes);
    }

    private ContentNode node(String sourceId, String text, CompiledDialogue dialogue, CompiledDialogueNode node,
            List<ContentReference> references) {
        List<String> edges = new ArrayList<>(node.choices().stream()
                .map(value -> value.targetNodeId().value()).toList());
        node.transition().flatMap(CompiledDialogueNode.Transition::targetNodeId)
                .ifPresent(value -> edges.add(value.value()));
        node.transition().flatMap(CompiledDialogueNode.Transition::targetDialogueId).ifPresent(value ->
                references.add(new ContentReference(ContentReferenceKind.DIALOGUE,
                        new ContentResourceId(value.toString()), SourceSpans.find(sourceId, text, value.toString()))));
        var source = dialogue.sourceInfo().flatMap(info -> java.util.Optional.ofNullable(info.nodeSources().get(node.id())));
        var span = source.map(value -> SourceSpans.at(sourceId, text, value.line(), value.column()))
                .orElseGet(() -> SourceSpans.find(sourceId, text, node.id().value()));
        return new ContentNode(node.id().value(), node.id().equals(dialogue.startNodeId()), false, edges, span,
                node.choices().stream().map(choice -> choice.id().value()).toList());
    }
}
