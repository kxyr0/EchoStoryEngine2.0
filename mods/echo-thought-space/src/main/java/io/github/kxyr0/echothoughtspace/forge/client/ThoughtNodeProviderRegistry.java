package io.github.kxyr0.echothoughtspace.forge.client;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProviderRegistrationEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.MinecraftForge;

public final class ThoughtNodeProviderRegistry {
    private List<ThoughtNodeProvider> providers = List.of();

    public void discover() {
        Map<String, ThoughtNodeProvider> values = new LinkedHashMap<>();
        MinecraftForge.EVENT_BUS.post(new ThoughtNodeProviderRegistrationEvent(provider ->
                values.putIfAbsent(provider.providerId(), provider)));
        providers = List.copyOf(values.values());
    }

    public List<ThoughtNodeProvider> providers() { return providers; }
}

