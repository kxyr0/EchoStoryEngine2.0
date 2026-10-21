package io.github.kxyr0.echobackgroundnpc.forge.api.location;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.eventbus.api.Event;

public final class BackgroundLocationResolverEvent extends Event {
    private final List<BackgroundLocationResolver> resolvers = new ArrayList<>();
    public void register(BackgroundLocationResolver resolver) { resolvers.add(resolver); }
    public List<BackgroundLocationResolver> resolvers() { return List.copyOf(resolvers); }
}
