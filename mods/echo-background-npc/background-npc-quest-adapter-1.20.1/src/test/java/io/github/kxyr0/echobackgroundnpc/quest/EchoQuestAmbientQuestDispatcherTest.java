package io.github.kxyr0.echobackgroundnpc.quest;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientQuestOffer;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundQuestId;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EchoQuestAmbientQuestDispatcherTest {
    @Test
    void convertsOfferToQuestIdentity() {
        var offer = new AmbientQuestOffer(new BackgroundQuestId("jdh:find_notebook"), 8.0D,
                new BackgroundSourceRef("talk.es", 1, 1));
        assertEquals("jdh:find_notebook", EchoQuestAmbientQuestDispatcher.questId(offer).value());
    }
}
