package io.github.kxyr0.echoteleportmagic.forge.client;

import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "echo_teleport_magic", value = Dist.CLIENT)
public final class TeleportClient {
    private static final Map<Key, ClientTeleportVisual> ACTIVE = new LinkedHashMap<>();
    private static final java.util.concurrent.atomic.AtomicReference<net.minecraft.client.multiplayer.ClientLevel> LEVEL = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicBoolean DEBUG = new java.util.concurrent.atomic.AtomicBoolean();
    public static boolean debug() { return DEBUG.get(); }
    private TeleportClient() { }
    public record Key(UUID execution, boolean arrival) { }

    public static Iterable<ClientTeleportVisual> active() { return ACTIVE.values(); }
    public static boolean hasVisuals() { return !ACTIVE.isEmpty(); }
    public static void receive(TeleportVisualPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        syncLevel();
        var key = new Key(packet.execution(), packet.arrival());
        switch (packet.kind()) {
            case START -> {
                if (ACTIVE.size() < 32) ACTIVE.putIfAbsent(key, new ClientTeleportVisual(packet));
            }
            case COMMIT -> { var visual = ACTIVE.get(key); if (visual != null) visual.commit(packet.anchor()); }
            case CANCEL -> ACTIVE.keySet().removeIf(k -> k.execution().equals(packet.execution()));
            case COMPLETE -> { /* Tail expires on its own clock; no early truncation on network correction. */ }
            case DEBUG -> {
                DEBUG.set(packet.freeze() != -2);
                for (var visual : ACTIVE.values()) if (visual.packet.preview()) visual.freeze = packet.freeze();
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        syncLevel();
        if (Minecraft.getInstance().level == null) { ACTIVE.clear(); DEBUG.set(false); return; }
        ACTIVE.values().removeIf(v -> v.freeze < 0 && v.time(0) >= (v.packet.arrival() ? 1.7 : 4));
        ACTIVE.values().removeIf(v -> v.freeze < 0 && Minecraft.getInstance().level.getGameTime() - v.packet.startTick() > 240);
        for (var visual : ACTIVE.values()) visual.update();
    }
    public static ClientTeleportVisual subject(UUID subject) {
        ClientTeleportVisual result = null;
        for (var visual : ACTIVE.values()) {
            if (visual.packet.subject().equals(subject) && (result == null || visual.packet.arrival())) result = visual;
        }
        return result;
    }
    public static void clear() { ACTIVE.clear(); DEBUG.set(false); }
    private static void syncLevel() {
        var level = Minecraft.getInstance().level;
        if (LEVEL.getAndSet(level) != level) clear();
    }
}

