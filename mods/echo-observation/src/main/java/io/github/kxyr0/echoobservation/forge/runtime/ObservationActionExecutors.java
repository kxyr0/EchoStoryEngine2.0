package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoobservation.api.action.ObservationActionRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

final class ObservationActionExecutors {
    private ObservationActionExecutors() {
    }

    static void register(ObservationActionRegistry registry, MinecraftServer server) {
        if (ModList.get().isLoaded("echo_audio")) registry.register(new ObservationAudioExecutor(server));
        if (ModList.get().isLoaded("echo_story_engine")) {
            registry.register(new ObservationStoryStartExecutor(server));
        }
        if (ModList.get().isLoaded("echo_dialogue")) {
            registry.register(new ObservationDialogueLoadExecutor(server));
        }
    }
}
