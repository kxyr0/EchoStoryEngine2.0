package io.github.kxyr0.echodebug.forge.integration.seating;

import io.github.kxyr0.echodebug.core.path.TriggerIdPlan;
import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echoseating.forge.api.SeatingServiceLookupEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public final class SeatTriggerSourceFactory {
    private SeatTriggerSourceFactory() {
    }

    public static String create(MinecraftServer server, TriggerIdPlan id, TriggerBlockPoint point, String story) {
        if (!ModList.get().isLoaded("echo_trigger_seating_adapter")) {
            throw new IllegalArgumentException("Для триггера посадки нужен Echo Trigger Seating Adapter.");
        }
        SeatingServiceLookupEvent lookup = new SeatingServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        var bridge = lookup.bridge().orElseThrow(() ->
                new IllegalArgumentException("Сервис Echo Seating недоступен."));
        var seat = SeatSelectionResolver.resolve(bridge.catalog().zones(), point);
        return render(id, seat, story);
    }

    static String render(TriggerIdPlan id, SeatSelectionResolver.SeatReference seat, String story) {
        return """
                class %s : TriggerResource {
                    private SeatZoneId Zone = "%s";
                    private SeatSlotId Seat = "%s";
                    private void Init() {
                        Id("%s");
                        Subscribe(Player.SeatedAt(Zone, Seat));
                        StartStory("%s");
                        FireOnlyOncePerPlayer();
                    }
                }
                """.formatted(id.className(), identifier(seat.zoneId()), identifier(seat.slotId()),
                id.triggerId().value(), identifier(story));
    }

    private static String identifier(String value) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Укажите ID истории в формате namespace:path.");
        }
        return value;
    }
}
