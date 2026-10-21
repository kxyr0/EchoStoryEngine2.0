package io.github.kxyr0.echonpcbackpack.animation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import java.util.List;
import java.util.Optional;

public final class BackpackAnimationBindingCatalog {
    private volatile List<NpcBackpackAnimationBinding> bindings = List.of();

    public void install(List<NpcBackpackAnimationBinding> values) {
        bindings = List.copyOf(values);
    }

    public Optional<io.github.kxyr0.echoanimation.api.animation.AnimationId> resolve(
            ActorId actor, String activeForm, NpcBackpackTransitionType type) {
        Optional<NpcBackpackAnimationBinding> exact = bindings.stream()
                .filter(binding -> binding.actorId().equals(actor) && binding.formId().equals(activeForm))
                .findFirst();
        return exact.or(() -> bindings.stream()
                        .filter(binding -> binding.actorId().equals(actor) && binding.formId().equals("*"))
                        .findFirst())
                .map(binding -> type == NpcBackpackTransitionType.EQUIP
                         ? binding.equipAnimation() : binding.unequipAnimation());
    }
}
