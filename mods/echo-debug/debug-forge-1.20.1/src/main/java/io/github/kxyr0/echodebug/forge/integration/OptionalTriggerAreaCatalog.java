package io.github.kxyr0.echodebug.forge.integration;

import io.github.kxyr0.echodebug.forge.EchoDebugMod;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

public final class OptionalTriggerAreaCatalog {
    private static final String SEATING_PROVIDER =
            "io.github.kxyr0.echodebug.forge.integration.seating.SeatingTriggerAreaProvider";

    private OptionalTriggerAreaCatalog() {
    }

    public static List<TriggerAreaDefinitionView> append(
            MinecraftServer server,
            List<TriggerAreaDefinitionView> triggerAreas
    ) {
        List<TriggerAreaDefinitionView> combined = new ArrayList<>(triggerAreas);
        if (ModList.get().isLoaded("echo_seating")) {
            combined.addAll(seatingAreas(server));
        }
        return List.copyOf(combined);
    }

    private static List<TriggerAreaDefinitionView> seatingAreas(MinecraftServer server) {
        try {
            Class<?> provider = Class.forName(SEATING_PROVIDER);
            Method areas = provider.getMethod("areas", MinecraftServer.class);
            Object result = areas.invoke(null, server);
            if (result instanceof List<?> list) {
                return list.stream().filter(TriggerAreaDefinitionView.class::isInstance)
                        .map(TriggerAreaDefinitionView.class::cast).toList();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoDebugMod.LOGGER.warn("Echo Debug could not resolve spatial Echo Seating triggers.", exception);
        }
        return List.of();
    }
}
