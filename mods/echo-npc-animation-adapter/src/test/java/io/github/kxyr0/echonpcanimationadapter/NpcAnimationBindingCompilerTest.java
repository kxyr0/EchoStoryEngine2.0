package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCompiler;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingResourceText;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class NpcAnimationBindingCompilerTest {
    private final NpcAnimationBindingCompiler compiler = new NpcAnimationBindingCompiler();

    @Test
    void actorOverrideWinsOverDefinitionBinding() {
        var catalog = compiler.compile(List.of(
                binding("definition.npc-animation-binding.es", "jdh:john_by_definition",
                        "npc: \"jdh:john_father\"", "jdh:standard_humanoid"),
                binding("actor.npc-animation-binding.es", "jdh:john_actor_override",
                        "actor: \"jdh:john_father\"", "jdh:wounded_humanoid")));

        var resolved = catalog.resolve(new ActorId("jdh:john_father"), new NpcDefinitionId("jdh:john_father"));

        Assertions.assertTrue(catalog.valid(), catalog.diagnostics().toString());
        Assertions.assertEquals("jdh:wounded_humanoid", resolved.orElseThrow().profileId().toString());
    }

    @Test
    void duplicateDefinitionBindingRejectsCandidate() {
        var catalog = compiler.compile(List.of(
                binding("left.npc-animation-binding.es", "jdh:left",
                        "npc: \"jdh:john_father\"", "jdh:standard_humanoid"),
                binding("right.npc-animation-binding.es", "jdh:right",
                        "npc: \"jdh:john_father\"", "jdh:wounded_humanoid")));

        Assertions.assertFalse(catalog.valid());
        Assertions.assertTrue(catalog.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.contains("Duplicate definition binding")));
    }

    private NpcAnimationBindingResourceText binding(
            String sourceId,
            String id,
            String target,
            String profile
    ) {
        return new NpcAnimationBindingResourceText(sourceId, """
                namespace JDH.NpcAnimation;

                class Binding : NpcAnimationBindingResource
                {
                    public override NpcAnimationBindingDef Build()
                    {
                        return NpcAnimationBinding("%s",
                            %s,
                            profile: "%s");
                    }
                }
                """.formatted(id, target, profile));
    }
}
