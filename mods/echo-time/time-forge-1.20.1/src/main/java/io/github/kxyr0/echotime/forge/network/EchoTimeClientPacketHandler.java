package io.github.kxyr0.echotime.forge.network;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.forge.api.client.EchoTimeClientSnapshotEvent;
import net.minecraftforge.common.MinecraftForge;

final class EchoTimeClientPacketHandler {
    private EchoTimeClientPacketHandler() { }

    static void accept(EchoTimeSnapshot snapshot) {
        MinecraftForge.EVENT_BUS.post(new EchoTimeClientSnapshotEvent(snapshot));
    }
}
