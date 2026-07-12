package io.github.kxyr0.echostoryengine.story.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class CompiledStoryFingerprintTest {
    @Test
    void fingerprintIsStableAcrossNodeMapOrder() {
        StoryDefinition first = story(Map.of("b", "2", "a", "1"));
        StoryDefinition second = story(reversedArguments());

        assertEquals(StoryDefinitionFingerprint.from(first), StoryDefinitionFingerprint.from(second));
    }

    @Test
    void fingerprintChangesWhenCompiledMeaningChanges() {
        StoryDefinition first = story(Map.of("a", "1"));
        StoryDefinition second = story(Map.of("a", "2"));

        assertNotEquals(StoryDefinitionFingerprint.from(first), StoryDefinitionFingerprint.from(second));
    }

    private StoryDefinition story(Map<String, String> arguments) {
        StoryId storyId = StoryId.of("test:fingerprint");
        NodeId start = NodeId.root();
        NodeId action = NodeId.of("action");
        NodeId finish = NodeId.of("finish");
        return new StoryDefinition(
                storyId,
                start,
                Map.of(
                        finish, StoryNode.finish(finish),
                        action, StoryNode.externalAction(
                                action,
                                NamespacedId.of("test:action"),
                                arguments,
                                finish),
                        start, StoryNode.start(start, action)));
    }

    private Map<String, String> reversedArguments() {
        LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
        arguments.put("b", "2");
        arguments.put("a", "1");
        return arguments;
    }
}
