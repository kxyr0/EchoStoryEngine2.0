package io.github.kxyr0.echodamagenumbers.network;

import net.minecraftforge.eventbus.api.Event;

public final class DamagePopupReceivedEvent extends Event {
    private final DamagePopupPacket packet;

    public DamagePopupReceivedEvent(DamagePopupPacket packet) {
        this.packet = packet;
    }

    public DamagePopupPacket packet() {
        return packet;
    }
}

