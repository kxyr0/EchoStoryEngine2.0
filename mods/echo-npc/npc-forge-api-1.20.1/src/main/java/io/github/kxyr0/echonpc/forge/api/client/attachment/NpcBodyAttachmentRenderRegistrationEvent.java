package io.github.kxyr0.echonpc.forge.api.client.attachment;

import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-only Forge event for body attachment render registration.
 */
public final class NpcBodyAttachmentRenderRegistrationEvent extends Event {
    private static final Map<NpcClientRenderExtensionId, Registration> GLOBAL = new LinkedHashMap<>();

    private final Map<NpcClientRenderExtensionId, Registration> registrations = new LinkedHashMap<>();
    private boolean closed;

    public static void registerGlobal(NpcClientRenderExtensionDescriptor descriptor,
            Supplier<NpcBodyAttachmentRenderExtension> factory) {
        synchronized (GLOBAL) {
            put(GLOBAL, new Registration(descriptor, factory));
        }
    }

    public static List<Registration> globalRegistrations() {
        synchronized (GLOBAL) {
            return snapshot(GLOBAL);
        }
    }

    public void register(NpcClientRenderExtensionDescriptor descriptor,
            Supplier<NpcBodyAttachmentRenderExtension> factory) {
        if (closed) {
            throw new IllegalStateException("NPC body attachment registration is already closed.");
        }
        put(registrations, new Registration(descriptor, factory));
    }

    public List<Registration> closeAndSnapshot() {
        closed = true;
        return snapshot(registrations);
    }

    private static void put(Map<NpcClientRenderExtensionId, Registration> target, Registration registration) {
        if (target.putIfAbsent(registration.descriptor().id(), registration) != null) {
            throw new IllegalArgumentException("Duplicate NPC body attachment id: " + registration.descriptor().id());
        }
    }

    private static List<Registration> snapshot(Map<NpcClientRenderExtensionId, Registration> source) {
        List<Registration> sorted = new ArrayList<>(source.values());
        sorted.sort(Comparator.comparing(registration -> registration.descriptor().id()));
        return List.copyOf(sorted);
    }

    public record Registration(NpcClientRenderExtensionDescriptor descriptor,
            Supplier<NpcBodyAttachmentRenderExtension> factory) {
        public Registration {
            descriptor = Objects.requireNonNull(descriptor, "descriptor");
            factory = Objects.requireNonNull(factory, "factory");
        }
    }
}
