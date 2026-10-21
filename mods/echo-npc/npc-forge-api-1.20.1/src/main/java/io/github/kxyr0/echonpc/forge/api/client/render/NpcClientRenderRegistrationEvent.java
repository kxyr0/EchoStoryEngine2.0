package io.github.kxyr0.echonpc.forge.api.client.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-only Forge event for explicit NPC render extension registration.
 */
public final class NpcClientRenderRegistrationEvent extends Event {
    private static final Map<NpcClientRenderExtensionId, Registration> GLOBAL_REGISTRATIONS =
            new LinkedHashMap<>();

    private final Map<NpcClientRenderExtensionId, Registration> registrations = new LinkedHashMap<>();
    private boolean closed;

    public static void registerGlobal(
            NpcClientRenderExtensionDescriptor descriptor,
            NpcClientRenderExtensionFactory factory
    ) {
        synchronized (GLOBAL_REGISTRATIONS) {
            putRegistration(GLOBAL_REGISTRATIONS, new Registration(descriptor, factory));
        }
    }

    public static List<Registration> globalRegistrations() {
        synchronized (GLOBAL_REGISTRATIONS) {
            return snapshot(GLOBAL_REGISTRATIONS);
        }
    }

    public void register(
            NpcClientRenderExtensionDescriptor descriptor,
            NpcClientRenderExtensionFactory factory
    ) {
        requireOpen();
        putRegistration(registrations, new Registration(descriptor, factory));
    }

    public List<Registration> closeAndSnapshot() {
        closed = true;
        return snapshot(registrations);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("NPC render registration is already closed.");
        }
    }

    private static void putRegistration(
            Map<NpcClientRenderExtensionId, Registration> registrations,
            Registration registration
    ) {
        if (registrations.putIfAbsent(registration.descriptor().id(), registration) != null) {
            throw new IllegalArgumentException("Duplicate NPC render extension id: " + registration.descriptor().id());
        }
    }

    private static List<Registration> snapshot(Map<NpcClientRenderExtensionId, Registration> registrations) {
        List<Registration> sorted = new ArrayList<>(registrations.values());
        sorted.sort(Comparator.comparing(registration -> registration.descriptor().id()));
        return List.copyOf(sorted);
    }

    public record Registration(
            NpcClientRenderExtensionDescriptor descriptor,
            NpcClientRenderExtensionFactory factory
    ) {
        public Registration {
            descriptor = Objects.requireNonNull(descriptor, "descriptor");
            factory = Objects.requireNonNull(factory, "factory");
        }
    }
}
