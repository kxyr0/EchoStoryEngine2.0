package io.github.kxyr0.echovisibility.forge.api.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class VisibilityStoryContextBridgeTest {
    @Test
    void inheritedContextsAreBounded() {
        List<String> ids = new ArrayList<>();
        try {
            for (int index = 0; index < 257; index++) {
                String id = "test:visibility_context/" + index;
                ids.add(id);
                VisibilityStoryContextBridge.install(id, Map.of("index", Integer.toString(index)));
            }

            assertTrue(VisibilityStoryContextBridge.find(ids.get(0)).isEmpty());
            assertTrue(VisibilityStoryContextBridge.find(ids.get(256)).isPresent());
        } finally {
            ids.forEach(VisibilityStoryContextBridge::clear);
        }
    }
}
