package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotriggertimeadapter.forge.TimeAdapterServices;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeDeliveryStatus;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeTriggerDelivery;
import io.github.kxyr0.echotriggertimeadapter.forge.persistence.TimeTriggerDeliveryStore;
import io.github.kxyr0.echotriggertimeadapter.forge.schedule.TimeSubscriptionIndex;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class TimeOccurrenceRouter {
    public static final String REPLAY_SUPPRESSED_CODE = "ESL-439439234342";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer server;
    private final TimeTriggerDeliveryStore store;
    private final TimeDeliveryFactory factory = new TimeDeliveryFactory();
    private final TimeTriggerClaim claim = new TimeTriggerClaim();
    private final TimeTargetLauncher targets = new TimeTargetLauncher();

    public TimeOccurrenceRouter(MinecraftServer server) {
        this.server = server;
        store = new TimeTriggerDeliveryStore(server);
    }

    public void route(TimeOccurrenceCrossed occurrence, TriggerStorySubscription subscription) {
        audiences(subscription).forEach(audience -> deliver(factory.create(occurrence, subscription, audience)));
    }

    public void routeAll(List<TimeOccurrenceCrossed> occurrences, TimeSubscriptionIndex index) {
        LinkedHashMap<String, TimeTriggerDelivery> candidates = new LinkedHashMap<>();
        occurrences.forEach(occurrence -> index.find(occurrence.entry().selectorId()).ifPresent(subscription ->
                audiences(subscription).forEach(audience -> {
                    TimeTriggerDelivery candidate = factory.create(occurrence, subscription, audience);
                    candidates.putIfAbsent(candidate.deliveryId(), candidate);
                })));
        List<TimeTriggerDelivery> missing = candidates.values().stream()
                .filter(value -> store.find(value.deliveryId()).isEmpty()).toList();
        store.pendingAll(missing);
        candidates.values().forEach(candidate -> {
            TimeTriggerDelivery delivery = store.find(candidate.deliveryId()).orElse(candidate);
            if (delivery.status() == TimeDeliveryStatus.DELIVERED) {
                LOGGER.warn("[{}] Time.At occurrence replay suppressed: trigger={} target={} day={} time={} "
                                + "delivery={}. Use /echo_time reset to begin a new replay generation.",
                        REPLAY_SUPPRESSED_CODE, delivery.triggerId(), delivery.targetId(), delivery.day(),
                        delivery.time(), delivery.deliveryId());
                return;
            }
            deliver(delivery);
        });
    }

    public void recoverPending() { store.pending().forEach(this::deliver); }
    public long occurrenceGeneration() { return store.occurrenceGeneration(); }
    public long advanceOccurrenceGeneration() { return store.advanceOccurrenceGeneration(); }

    private List<Optional<String>> audiences(TriggerStorySubscription subscription) {
        if (subscription.startsStory()) return List.of(Optional.empty());
        return server.getPlayerList().getPlayers().stream()
                .sorted(Comparator.comparing(player -> player.getUUID().toString()))
                .map(player -> Optional.of(player.getUUID().toString())).toList();
    }

    private void deliver(TimeTriggerDelivery candidate) {
        Optional<TimeTriggerDelivery> existing = store.find(candidate.deliveryId());
        if (existing.filter(value -> value.status() == TimeDeliveryStatus.DELIVERED).isPresent()) return;
        TimeTriggerDelivery delivery = existing.orElse(candidate);
        if (existing.isEmpty()) store.pending(delivery);
        var trigger = TimeAdapterServices.trigger(server);
        if (trigger.isEmpty() || !claim.claim(delivery, trigger.orElseThrow(), server.getTickCount())) return;
        if (targets.launch(delivery, server)) store.delivered(delivery);
    }
}
