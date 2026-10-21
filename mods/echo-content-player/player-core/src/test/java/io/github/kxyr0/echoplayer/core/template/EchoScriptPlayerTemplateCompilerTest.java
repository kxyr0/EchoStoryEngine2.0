package io.github.kxyr0.echoplayer.core.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import org.junit.jupiter.api.Test;

final class EchoScriptPlayerTemplateCompilerTest {
    private final EchoScriptPlayerTemplateCompiler compiler = new EchoScriptPlayerTemplateCompiler();

    @Test
    void validMinimalTemplateBindsThroughRealParser() {
        var result = compiler.compile("default.player.es", source("""
                namespace EchoContentPlayer.Test;

                class DefaultPlayer : PlayerProfileTemplate
                {
                    public override PlayerProfile Build()
                    {
                        return PlayerProfile(
                            id: "jdh:player/default",
                            nameMode: ACCOUNT,
                            spark: 100,
                            maxSpark: 100,
                            skinMode: RESOURCE,
                            skinId: "echo_content_player:player/base",
                            skinModel: CLASSIC
                        );
                    }
                }
                """));

        assertTrue(result.successful());
        PlayerProfileTemplate template = result.value().orElseThrow();
        assertEquals("jdh:player/default", template.id().value());
        assertEquals(PlayerNameMode.ACCOUNT, template.nameMode());
        assertEquals(100, template.spark().current());
        assertEquals(PlayerSkinMode.RESOURCE, template.appearance().skinMode());
        assertEquals(PlayerBaseSkin.ID, template.appearance().skinId().orElseThrow());
    }

    @Test
    void customResourceSkinTemplateBinds() {
        var result = compiler.compile("custom.player.es", source("""
                namespace EchoContentPlayer.Test;

                class CustomPlayer : PlayerProfileTemplate
                {
                    public override PlayerProfile Build()
                    {
                        return PlayerProfile(
                            id: "jdh:player/default",
                            nameMode: CUSTOM,
                            displayName: "Искра",
                            spark: 40,
                            maxSpark: 80,
                            skinMode: RESOURCE,
                            skinId: "jdh:player/default",
                            skinModel: SLIM
                        );
                    }
                }
                """));

        assertTrue(result.successful());
        assertEquals(PlayerNameMode.CUSTOM, result.value().orElseThrow().nameMode());
        assertEquals(PlayerBaseSkin.ID, result.value().orElseThrow().appearance().skinId().orElseThrow());
    }

    @Test
    void brokenScriptReturnsDiagnostics() {
        var result = compiler.compile("broken.player.es", "class Broken");

        assertFalse(result.successful());
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test
    void invalidTemplateValuesReturnDiagnostics() {
        var result = compiler.compile("invalid.player.es", source("""
                namespace EchoContentPlayer.Test;

                class InvalidPlayer : PlayerProfileTemplate
                {
                    public override PlayerProfile Build()
                    {
                        return PlayerProfile(
                            id: "jdh:player/default",
                            nameMode: CUSTOM,
                            displayName: "",
                            spark: 120,
                            maxSpark: 100,
                            skinMode: RESOURCE,
                            skinId: "jdh:../bad",
                            skinModel: CLASSIC
                        );
                    }
                }
                """));

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()));
    }

    private String source(String text) {
        return text;
    }
}
