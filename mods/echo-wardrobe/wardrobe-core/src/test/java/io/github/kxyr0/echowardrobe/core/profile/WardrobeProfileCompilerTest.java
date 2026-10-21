package io.github.kxyr0.echowardrobe.core.profile;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.profile.CoveragePolicy;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WardrobeProfileCompilerTest {
    private final WardrobeProfileCompiler compiler =
            new WardrobeProfileCompiler(EchoScriptLanguage.parser());

    @Test
    void bindsWardrobeProfileResource() {
        WardrobeResult<List<WardrobeProfileDefinition>> result = compiler.compile("john.wardrobe-profile.es",
                source(), "sha256:test");

        Assertions.assertTrue(result.successful());
        WardrobeProfileDefinition profile = result.value().orElseThrow().get(0);
        Assertions.assertEquals("jdh:john", profile.id().value());
        Assertions.assertEquals("echo_content_player:player/base", profile.baseSkin());
        Assertions.assertTrue(profile.slots().contains(WardrobeSlot.FACE));
        Assertions.assertEquals(CoveragePolicy.REPORT, profile.coveragePolicy());
    }

    @Test
    void missingBaseSkinIsInvalid() {
        WardrobeResult<List<WardrobeProfileDefinition>> result = compiler.compile("bad.wardrobe-profile.es",
                source().replace("BaseSkin(\"echo_content_player:player/base\");", ""), "sha256:test");

        Assertions.assertFalse(result.successful());
    }

    private String source() {
        return """
                namespace jdh.wardrobe.profiles;

                class JohnProfile : WardrobeProfileResource
                {
                    private void Init()
                    {
                        Id("jdh:john");
                        BaseSkin("echo_content_player:player/base");
                        Slots(FACE, TOP, BOTTOM, FEET);
                        Coverage(REPORT);
                    }
                }
                """;
    }
}
