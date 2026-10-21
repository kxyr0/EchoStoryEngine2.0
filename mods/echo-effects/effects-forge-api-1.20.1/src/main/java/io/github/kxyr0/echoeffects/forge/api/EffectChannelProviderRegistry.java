package io.github.kxyr0.echoeffects.forge.api;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import java.util.List;
import java.util.Optional;

public interface EffectChannelProviderRegistry {
    void register(EffectChannelProvider provider);

    Optional<EffectChannelProvider> provider(EffectChannelId channelId);

    List<EffectChannelProvider> providers();
}
