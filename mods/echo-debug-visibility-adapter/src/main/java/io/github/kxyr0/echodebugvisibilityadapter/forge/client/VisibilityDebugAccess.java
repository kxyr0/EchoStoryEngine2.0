package io.github.kxyr0.echodebugvisibilityadapter.forge.client;

import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import io.github.kxyr0.echovisibility.forge.api.debug.VisibilityClientDebugLookupEvent;
import io.github.kxyr0.echovisibility.forge.api.debug.VisibilityClientDebugView;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

public final class VisibilityDebugAccess {
    public Optional<VisibilityClientDebugView> view() {
        VisibilityClientDebugLookupEvent event = new VisibilityClientDebugLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        return event.view();
    }

    public List<VisibilityDebugSnapshot> watches(
            VisibilityClientDebugView view,
            VisibilityDebugSettings settings) {
        if (settings.all()) {
            return view.activeWatches();
        }
        return settings.watch().flatMap(view::watch).stream().toList();
    }
}
