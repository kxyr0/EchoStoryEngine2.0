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
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StorySourceAdapter implements SourceAdapter {
    private final io.github.kxyr0.echoscene.compile.SceneSymbolCatalog symbols;
    public StorySourceAdapter() { this(io.github.kxyr0.echoscene.compile.SceneSymbolCatalog.of(List.of())); }
    public StorySourceAdapter(io.github.kxyr0.echoscene.compile.SceneSymbolCatalog symbols) { this.symbols = symbols; }
    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_story/stories/") && value.endsWith(".es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var result = new EchoScriptStorySourceCompiler().compile(new StorySourceInput(sourceId, sourceText),
                Instant.EPOCH);
        List<ContentSourceProblem> problems = result.diagnostics().stream()
                .filter(value -> value.severity() == DiagnosticSeverity.ERROR
                        || value.severity() == DiagnosticSeverity.WARN)
                .map(value -> new ContentSourceProblem(
                        value.severity() == DiagnosticSeverity.ERROR ? ContentSeverity.ERROR : ContentSeverity.WARNING,
                        value.code(), SourceIdentities.inferred(sourceId), sourceId, sourceText,
                        Integer.parseInt(value.context().getOrDefault("line", "1")),
                        Integer.parseInt(value.context().getOrDefault("column", "1")),
                        value.message())).toList();
        return result.compiledStory().map(value -> new AdaptedSource(List.of(descriptor(sourceId, sourceText, value)),
                problems)).orElseGet(() -> new AdaptedSource(List.of(), problems));
    }

    private ContentDescriptor descriptor(String sourceId, String text, StoryDefinition story) {
        ContentResourceId id = new ContentResourceId(story.storyId().asString());
        List<ContentNode> nodes = story.nodes().values().stream().map(node -> node(sourceId, text, story, node))
                .toList();
        List<ContentReference> references = new ArrayList<>();
        story.nodes().values().forEach(node -> reference(sourceId, text, node).ifPresent(references::add));
        story.nodes().values().stream().filter(n -> n.actionType().asString().equals("echo_audio:cue"))
                .map(n -> n.actionArguments().getOrDefault("location", "")).filter(s -> !s.isEmpty()).forEach(location -> {
                    String target = location.contains(":") ? location : symbols.resolve(location).stream()
                            .filter(s -> s.kind() == io.github.kxyr0.echoscene.compile.SceneSymbolKind.LOCATION)
                            .map(io.github.kxyr0.echoscene.compile.SceneSymbol::resourceId).findFirst()
                            .orElse("unknown:" + location.toLowerCase(java.util.Locale.ROOT));
                    ref(ContentReferenceKind.LOCATION, target, sourceId, text).ifPresent(references::add);
                });
        return ContentDescriptor.simple(id, ContentResourceKind.STORY, SourceSpans.file(sourceId, text), references)
                .withNodes(nodes);
    }

    private ContentNode node(String sourceId, String text, StoryDefinition story, StoryNode node) {
        List<String> edges = story.nodes().containsKey(node.nextNode()) ? List.of(node.nextNode().value()) : List.of();
        return new ContentNode(node.id().value(), node.id().equals(story.startNode()), false, edges,
                SourceSpans.find(sourceId, text, node.id().value()));
    }

    private java.util.Optional<ContentReference> reference(String sourceId, String text, StoryNode node) {
        String action = node.actionType().asString();
        Map<String, String> args = node.actionArguments();
        if (action.equals("echo_audio:cue") && !args.getOrDefault("resource", "").isEmpty()) {
            return ref(ContentReferenceKind.AUDIO, args.get("resource"), sourceId, text);
        }
        if (action.equals("echo_dialogue:play") && args.containsKey("dialogueId")) {
            return ref(ContentReferenceKind.DIALOGUE, args.get("dialogueId"), sourceId, text);
        }
        if (action.startsWith("echo_npc:") && (args.containsKey("definitionId") || args.containsKey("npcId"))) {
            return ref(ContentReferenceKind.NPC, args.getOrDefault("definitionId", args.get("npcId")), sourceId, text);
        }
        if (action.equals("echo_story_engine:load") && args.containsKey("storyId")) {
            return ref(ContentReferenceKind.STORY, args.get("storyId"), sourceId, text);
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<ContentReference> ref(ContentReferenceKind kind, String target, String sourceId,
            String text) {
        try {
            return java.util.Optional.of(new ContentReference(kind, new ContentResourceId(target),
                    SourceSpans.find(sourceId, text, target)));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}
