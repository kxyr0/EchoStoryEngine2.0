package io.github.kxyr0.echostoryengine.story.forge;

import io.github.kxyr0.echostoryengine.story.api.StoryEngineApi;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.forge.composition.ForgeStoryCompositionRoot;
import java.util.Map;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entrypoint for the story-only scaffold.
 */
@Mod(EchoStoryEngineMod.MOD_ID)
public final class EchoStoryEngineMod {
    public static final String MOD_ID = "echo_story_engine";

    private final StoryEngineApi storyEngine;

    public EchoStoryEngineMod(FMLJavaModLoadingContext context) {
        ForgeStoryCompositionRoot compositionRoot = ForgeStoryCompositionRoot.create();
        this.storyEngine = compositionRoot.storyEngine();
        context.getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        StoryRuntimeInput input = StoryRuntimeInput.of(
                StoryId.of("echo_story_engine:bootstrap"),
                ExecutionId.of("echo_story_engine:bootstrap"),
                NamespacedId.of("echo_story_engine:forge_bootstrap"),
                Map.of("source", "forge-common-setup"));
        storyEngine.advance(input);
    }
}
