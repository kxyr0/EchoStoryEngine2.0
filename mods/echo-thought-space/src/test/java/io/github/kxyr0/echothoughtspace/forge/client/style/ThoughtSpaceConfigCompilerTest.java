package io.github.kxyr0.echothoughtspace.forge.client.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import org.junit.jupiter.api.Test;

final class ThoughtSpaceConfigCompilerTest {
    private final ThoughtSpaceConfigCompiler compiler = new ThoughtSpaceConfigCompiler();

    @Test
    void es2ResourceBindsStyleAndInferredCombinationIdentity() {
        var document = compile("""
                class Thoughts : ThoughtSpaceResource
                {
                    Style(circleRadius: 44, nodeFontSize: 1.3, titleText: "ФОРМА МЫСЛИ");

                    Thought BrokenGlass()
                    {
                        Text("Стекло на полу.");
                    }

                    Combination WindowBrokenInside()
                    {
                        From("jdh:house#window", "jdh:house#mud");
                        Thought("Окно разбили изнутри.");
                    }
                }
                """);
        assertEquals(44.0D, document.style().orElseThrow().circleRadius());
        assertEquals(1.3D, document.style().orElseThrow().nodeFontSize());
        assertEquals(1, document.thoughts().size());
        assertEquals(new ThoughtNodeId("jdh:thought_space#broken_glass"), document.thoughts().get(0).id());
        assertEquals(1, document.recipes().size());
        assertEquals(new ThoughtNodeId("jdh:thought_space#window_broken_inside"),
                document.recipes().get(0).result());
    }

    @Test
    void sparseIdOverrideSupportsStableChainingIdentity() {
        var recipe = compile("""
                class Thoughts : ThoughtSpaceResource
                {
                    Combination Conclusion()
                    {
                        From("a", "b");
                        Thought("Result");
                        Id("jdh:deductions#stable");
                    }
                }
                """).recipes().get(0);
        assertEquals(new ThoughtNodeId("jdh:deductions#stable"), recipe.result());
    }

    @Test
    void legacyBuildAndMalformedHighLevelRecipesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> compile("""
                class Legacy : ThoughtSpaceStyleConfig
                {
                    public override ThoughtSpaceStyleDef Build() { return ThoughtSpaceStyle(); }
                }
                """));
        assertThrows(IllegalArgumentException.class, () -> compile("""
                class Thoughts : ThoughtSpaceResource
                {
                    Combination Broken() { From("a"); Thought("Result"); }
                }
                """));
    }

    private ThoughtSpaceConfigCompiler.Document compile(String source) {
        return compiler.compile("jdh/echo_thought_space/thought_space.thought-space.es", source,
                ThoughtSpaceStyleConfig.defaults(), "jdh", "thought_space");
    }
}
