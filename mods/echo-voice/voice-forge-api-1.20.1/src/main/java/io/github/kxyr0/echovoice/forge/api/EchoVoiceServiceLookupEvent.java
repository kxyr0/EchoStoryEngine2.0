package io.github.kxyr0.echovoice.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;
import io.github.kxyr0.echovoice.forge.api.ambient.AmbientVoiceService;

/** Optional runtime lookup; posting this event does not require a static service global. */
public final class EchoVoiceServiceLookupEvent extends Event {
    private final List<Entry> providers = new ArrayList<>();
    private final List<AmbientVoiceService> ambientProviders = new ArrayList<>();

    public void provide(VoiceService service, VoiceQueryService query) {
        providers.add(new Entry(service, query));
    }

    public List<Entry> providers() {
        return List.copyOf(providers);
    }

    public void provideAmbient(AmbientVoiceService service) {
        ambientProviders.add(Objects.requireNonNull(service, "service"));
    }

    public List<AmbientVoiceService> ambientProviders() { return List.copyOf(ambientProviders); }

    public record Entry(VoiceService service, VoiceQueryService query) {
        public Entry {
            service = Objects.requireNonNull(service, "service");
            query = Objects.requireNonNull(query, "query");
        }
    }
}
