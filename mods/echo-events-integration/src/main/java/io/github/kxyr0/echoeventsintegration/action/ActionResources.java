package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.forge.api.CanonicalResourceLookupEvent;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import net.minecraftforge.common.MinecraftForge;

final class ActionResources {
    private ActionResources() { }
    static String target(TriggerEventActionDispatchEvent action) {
        return canonical(action.action().target().value());
    }
    static String source(TriggerEventActionDispatchEvent action) {
        return canonical(action.delivery().event().source().resource().value());
    }
    static String canonical(String resource) {
        String[] parts = resource.split("#", 2);
        var lookup = new CanonicalResourceLookupEvent(new ResourceId(parts[0]));
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.canonical().value() + (parts.length == 2 ? "#" + parts[1] : "");
    }
}
