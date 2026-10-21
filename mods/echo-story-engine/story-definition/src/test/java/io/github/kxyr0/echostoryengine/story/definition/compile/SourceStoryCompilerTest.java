package io.github.kxyr0.echostoryengine.story.definition.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SourceStoryCompilerTest {
    @Test
    void sourceCollectionMutationAfterCompilationCannotAlterCompiledOutput() {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("message", "ready");
        List<SourceNode> nodes = new ArrayList<>(SourcePipelineFixture.validNodes());
        nodes.set(3, SourcePipelineFixture.actionNode("confirm", "sample:confirm", "finish", arguments));
        SourceStory source = SourcePipelineFixture.source(
                java.util.Optional.of(1),
                java.util.Optional.of("sample:hello_story"),
                java.util.Optional.of("start"),
                nodes);

        StoryDefinition compiled = new SourceStoryCompiler().compile(source, SourcePipelineFixture.TIME)
                .compiledStory()
                .orElseThrow();
        arguments.put("message", "changed");
        nodes.clear();

        StoryNode action = compiled.find(NodeId.of("confirm")).orElseThrow();
        assertEquals("ready", action.actionArguments().get("message"));
        assertEquals(5, compiled.nodes().size());
    }

    @Test
    void externalActionOutputBindingCompilesIntoRuntimeNode() {
        List<SourceNode> nodes = new ArrayList<>(SourcePipelineFixture.validNodes());
        nodes.set(3, SourcePipelineFixture.actionNodeWithOutput(
                "confirm",
                "sample:confirm",
                "external.result",
                "finish"));
        SourceStory source = SourcePipelineFixture.source(
                java.util.Optional.of(1),
                java.util.Optional.of("sample:hello_story"),
                java.util.Optional.of("start"),
                nodes);

        StoryDefinition compiled = new SourceStoryCompiler().compile(source, SourcePipelineFixture.TIME)
                .compiledStory()
                .orElseThrow();

        assertEquals("external.result", compiled.find(NodeId.of("confirm")).orElseThrow().outputName());
    }
}
