package io.github.kxyr0.echostorynpcadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostorynpcadapter.mutation.StoryMutationMapper;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class StoryMutationMapperTest {
    @Test
    void sameActionExecutionIdProducesSameMutationId() {
        var mapper = new StoryMutationMapper();
        var id = ActionExecutionId.of("sample:execution/action");
        assertEquals(mapper.mutationId(id), mapper.mutationId(id));
    }

    @Test
    void differentActionExecutionIdsProduceDifferentMutationIds() {
        var mapper = new StoryMutationMapper();
        assertNotEquals(mapper.mutationId(ActionExecutionId.of("sample:first")),
                mapper.mutationId(ActionExecutionId.of("sample:second")));
    }

    @Test
    void mappingSurvivesFreshInstancesAndLocaleChange() {
        Locale previous = Locale.getDefault();
        try {
            var id = ActionExecutionId.of("sample:stable");
            String before = new StoryMutationMapper().mutationId(id).value();
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            String after = new StoryMutationMapper().mutationId(id).value();
            assertEquals(before, after);
            assertTrue(after.startsWith("echo_story_npc_adapter:"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
