package io.github.kxyr0.echotime.forge.api;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;

public interface EchoTimeQueryService {
    EchoTimeSnapshot current();

    long derivedVanillaDayTime();
}
