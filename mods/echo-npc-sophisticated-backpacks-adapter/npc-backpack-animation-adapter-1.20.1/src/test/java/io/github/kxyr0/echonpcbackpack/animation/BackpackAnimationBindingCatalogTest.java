package io.github.kxyr0.echonpcbackpack.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BackpackAnimationBindingCatalogTest {
    @Test
    void exactFormWinsAndWildcardIsFallback() {
        ActorId actor = new ActorId("jdh:molly");
        BackpackAnimationBindingCatalog catalog = new BackpackAnimationBindingCatalog();
        catalog.install(List.of(
                binding(actor, "*", "jdh:wild_equip", "jdh:wild_unequip"),
                binding(actor, "jdh:child", "jdh:child_equip", "jdh:child_unequip")));

        assertEquals(AnimationId.parse("jdh:child_equip"), catalog.resolve(actor,
                "jdh:child", NpcBackpackTransitionType.EQUIP).orElseThrow());
        assertEquals(AnimationId.parse("jdh:wild_unequip"), catalog.resolve(actor,
                "jdh:adult", NpcBackpackTransitionType.UNEQUIP).orElseThrow());
    }

    private NpcBackpackAnimationBinding binding(ActorId actor, String form,
            String equip, String unequip) {
        return new NpcBackpackAnimationBinding(actor, form,
                AnimationId.parse(equip), AnimationId.parse(unequip));
    }
}
