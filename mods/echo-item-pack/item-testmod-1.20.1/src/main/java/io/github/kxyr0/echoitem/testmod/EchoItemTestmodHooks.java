package io.github.kxyr0.echoitem.testmod;

import io.github.kxyr0.echoitem.api.ItemKindId;

/**
 * Development-only marker proving the testmod is not embedded in the production JAR.
 */
public final class EchoItemTestmodHooks {
    private EchoItemTestmodHooks() {
    }

    public static ItemKindId weaponKind() {
        return ItemKindId.WEAPON;
    }
}
