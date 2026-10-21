package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationSubjectMappingTest {
    @Test
    void actorIdMapsToNeutralAnimationSubjectId() {
        AnimationSubjectMapping mapping = new AnimationSubjectMapping();
        Assertions.assertEquals("jdh:john_father", mapping.subjectId(new ActorId("jdh:john_father")).toString());
    }
}
