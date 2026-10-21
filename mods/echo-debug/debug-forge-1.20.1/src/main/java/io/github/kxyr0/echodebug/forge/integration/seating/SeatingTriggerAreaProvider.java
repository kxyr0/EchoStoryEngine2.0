package io.github.kxyr0.echodebug.forge.integration.seating;

import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneBox;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.forge.api.SeatingServiceLookupEvent;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class SeatingTriggerAreaProvider {
    private static final String SEATING_EVENTS = "echo_seating:events";

    private SeatingTriggerAreaProvider() {
    }

    public static List<TriggerAreaDefinitionView> areas(MinecraftServer server) {
        TriggerServiceLookupEvent triggers = new TriggerServiceLookupEvent(server);
        SeatingServiceLookupEvent seating = new SeatingServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(triggers);
        MinecraftForge.EVENT_BUS.post(seating);
        if (triggers.bridge().isEmpty() || seating.bridge().isEmpty()) {
            return List.of();
        }
        var subscriptions = triggers.bridge().orElseThrow().subscriptions();
        var zones = seating.bridge().orElseThrow().catalog();
        TriggerRevision revision = new TriggerRevision(Math.max(
                subscriptions.catalogRevision(), zones.catalogRevision()));
        return areas(subscriptions.storySubscriptions(), zones.zones(), revision);
    }

    static List<TriggerAreaDefinitionView> areas(
            List<TriggerStorySubscription> subscriptions,
            List<SeatZoneDefinition> zones,
            TriggerRevision revision
    ) {
        List<TriggerAreaDefinitionView> result = new ArrayList<>();
        subscriptions.stream()
                .filter(SeatingTriggerAreaProvider::isSpatialSeatingTrigger)
                .forEach(subscription -> append(subscription, zones, revision, result));
        return List.copyOf(result);
    }

    private static boolean isSpatialSeatingTrigger(TriggerStorySubscription subscription) {
        TriggerEventSelector selector = subscription.selector();
        return selector.providerId().equals(SEATING_EVENTS)
                && (selector.zoneId().isPresent() || selector.slotId().isPresent());
    }

    private static void append(
            TriggerStorySubscription subscription,
            List<SeatZoneDefinition> zones,
            TriggerRevision revision,
            List<TriggerAreaDefinitionView> result
    ) {
        TriggerEventSelector selector = subscription.selector();
        zones.stream().filter(zone -> matchesZone(selector, zone)).forEach(zone -> {
            if (selector.slotId().isPresent()) {
                zone.slots().stream().filter(slot -> matchesSlot(selector, slot))
                        .map(slot -> slotView(subscription, zone, slot, revision)).forEach(result::add);
            } else {
                result.add(zoneView(subscription, zone, revision));
            }
        });
    }

    private static boolean matchesZone(TriggerEventSelector selector, SeatZoneDefinition zone) {
        return selector.zoneId().isEmpty()
                || selector.zoneId().orElseThrow().equals(zone.id().value());
    }

    private static boolean matchesSlot(TriggerEventSelector selector, SeatSlotDefinition slot) {
        return selector.slotId().orElseThrow().equals(slot.id().value());
    }

    private static TriggerAreaDefinitionView slotView(
            TriggerStorySubscription subscription,
            SeatZoneDefinition zone,
            SeatSlotDefinition slot,
            TriggerRevision revision
    ) {
        BlockPosition block = BlockPosition.containing(slot.anchor());
        Vector3 min = new Vector3(block.x(), block.y() - 1.0D, block.z());
        Vector3 max = new Vector3(block.x() + 1.0D, block.y(), block.z() + 1.0D);
        return view(subscription, slot.id().value(), zone, revision, new Bounds(min, max));
    }

    private static TriggerAreaDefinitionView zoneView(
            TriggerStorySubscription subscription,
            SeatZoneDefinition zone,
            TriggerRevision revision
    ) {
        SeatZoneBox box = zone.area();
        Vector3 min = new Vector3(box.min().x(), box.min().y() - 1.0D, box.min().z());
        Vector3 max = new Vector3(box.max().x() + 1.0D, box.max().y() + 1.0D, box.max().z() + 1.0D);
        return view(subscription, zone.id().value(), zone, revision, new Bounds(min, max));
    }

    private static TriggerAreaDefinitionView view(
            TriggerStorySubscription subscription,
            String areaName,
            SeatZoneDefinition zone,
            TriggerRevision revision,
            Bounds bounds
    ) {
        Vector3 center = bounds.min().add(bounds.max()).divide(2.0D);
        AreaGeometrySnapshot geometry = new AreaGeometrySnapshot(AreaShapeKind.BOX,
                bounds.min(), bounds.max(), center,
                OptionalDouble.empty(), List.of());
        return new TriggerAreaDefinitionView(subscription.triggerId(), areaName, zone.dimension().value(),
                revision, geometry, Optional.empty(), List.of());
    }

    private record Bounds(Vector3 min, Vector3 max) {
    }
}
