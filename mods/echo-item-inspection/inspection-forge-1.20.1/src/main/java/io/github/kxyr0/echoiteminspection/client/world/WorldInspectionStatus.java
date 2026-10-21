package io.github.kxyr0.echoiteminspection.client.world;

import io.github.kxyr0.echoiteminspection.client.input.WorldInspectionInput;
import net.minecraft.Util;

public final class WorldInspectionStatus {
    private WorldInspectionStatus() { }

    public static String describe() {
        var prompt = WorldInspectionRuntime.prompt();
        var target = WorldInspectionRuntime.lastTarget();
        String state = " key=" + WorldInspectionInput.INSPECT.getTranslatedKeyMessage().getString()
                + " prompt=" + (prompt.alpha(Util.getMillis()) > 0.02 ? "visible" : "hidden")
                + " active=" + (prompt.activeTarget() != null) + " diagnostic=" + WorldInspectionDiagnostics.status();
        return target == null ? "World target=none" + state : "World target=IPLA_WORLD lastIdentity="
                + target.identity() + " item=" + target.snapshot().itemId() + " inspectable=true distance="
                + target.distance() + " hit=" + target.hitPosition() + " fingerprint=" + target.snapshot().fingerprint() + state;
    }
}
