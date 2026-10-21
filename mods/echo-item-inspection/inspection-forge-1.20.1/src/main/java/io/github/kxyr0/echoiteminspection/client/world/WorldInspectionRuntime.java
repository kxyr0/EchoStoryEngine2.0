package io.github.kxyr0.echoiteminspection.client.world;

import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.client.integration.foreground.InspectionForegroundAccess;
import io.github.kxyr0.echoiteminspection.world.WorldInspectionPromptState;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.Util;

/** Coordinates fresh hits and presentation grace; activation always performs a second raycast. */
public final class WorldInspectionRuntime {
    private static final WorldInspectionPromptState<ItemInspectionWorldTarget> PROMPT = new WorldInspectionPromptState<>();
    private static Supplier<Optional<ItemInspectionWorldTarget>> resolver;
    private static Supplier<Optional<ItemInspectionWorldTarget>> activationResolver;
    private static Runnable resetResolver = () -> { };
    private static ItemInspectionWorldTarget lastTarget;

    private WorldInspectionRuntime() { }

    public static void install(Supplier<Optional<ItemInspectionWorldTarget>> targetResolver,
            Supplier<Optional<ItemInspectionWorldTarget>> freshResolver, Runnable reset) {
        resolver = targetResolver;
        activationResolver = freshResolver;
        resetResolver = reset;
    }

    public static void refresh() {
        if (resolver == null || !InspectionForegroundAccess.allowed()) {
            PROMPT.clear();
            return;
        }
        var target = resolver.get().orElse(null);
        if (target != null) lastTarget = target;
        PROMPT.update(target, Util.getMillis());
    }

    public static boolean activate() {
        if (resolver == null || !InspectionForegroundAccess.allowed()) {
            PROMPT.clear();
            return false;
        }
        var previous = PROMPT.activeTarget();
        var fresh = activationResolver.get().orElse(null);
        if (previous == null || !previous.sameRevision(fresh)) {
            PROMPT.update(null, Util.getMillis());
            WorldInspectionDiagnostics.record(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_TARGET_STALE,
                    "Activation rejected: placed target or stack revision changed");
            return false;
        }
        boolean opened = ItemInspectionClient.openWorldInspection(fresh);
        if (opened) PROMPT.clear();
        return opened;
    }

    public static WorldInspectionPromptState<ItemInspectionWorldTarget> prompt() { return PROMPT; }
    public static ItemInspectionWorldTarget lastTarget() { return lastTarget; }
    public static void clear() {
        PROMPT.clear();
        lastTarget = null;
        resetResolver.run();
    }
}
