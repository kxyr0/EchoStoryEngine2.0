package io.github.kxyr0.echonpcinteraction.forge.consequence;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionConsequenceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NpcInteractionConsequenceClassifier {
    public List<NpcInteractionConsequenceKind> classify(
            InteractionEffectSnapshot before,
            InteractionEffectSnapshot after) {
        List<NpcInteractionConsequenceKind> kinds = new ArrayList<>();
        addWorldChanges(before, after, kinds);
        if (!after.spawnedSince(before).isEmpty()) {
            kinds.add(NpcInteractionConsequenceKind.ENTITY_SPAWNED);
        }
        if (before.proxyVehicleId().isEmpty() && after.proxyVehicleId().isPresent()) {
            kinds.add(NpcInteractionConsequenceKind.PROXY_STARTED_RIDING);
        }
        if (after.menuOpen()) {
            kinds.add(NpcInteractionConsequenceKind.MENU_OPENED);
        }
        if (!Objects.equals(before.inventoryFingerprint(), after.inventoryFingerprint())) {
            kinds.add(NpcInteractionConsequenceKind.PROXY_RECEIVED_ITEM);
        }
        addFallback(before, after, kinds);
        if (usefulCount(kinds) > 1) {
            kinds.add(NpcInteractionConsequenceKind.MULTIPLE_EFFECTS);
        }
        return List.copyOf(kinds);
    }

    private void addWorldChanges(InteractionEffectSnapshot before, InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> kinds) {
        boolean targetChanged = !before.targetStateFingerprint().equals(after.targetStateFingerprint());
        boolean blockEntityChanged = !before.targetBlockEntityFingerprint().equals(after.targetBlockEntityFingerprint());
        if (targetChanged) {
            kinds.add(NpcInteractionConsequenceKind.BLOCK_STATE_CHANGED);
        }
        if (blockEntityChanged) {
            kinds.add(NpcInteractionConsequenceKind.BLOCK_ENTITY_CHANGED);
        }
        if (!targetChanged && !blockEntityChanged
                && !before.nearbyStateFingerprint().equals(after.nearbyStateFingerprint())) {
            kinds.add(NpcInteractionConsequenceKind.NEARBY_BLOCK_STATE_CHANGED);
        }
    }

    private void addFallback(InteractionEffectSnapshot before, InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> kinds) {
        if (!kinds.isEmpty()) {
            return;
        }
        if (!before.proxyPosition().equals(after.proxyPosition()) || !before.proxyPose().equals(after.proxyPose())) {
            kinds.add(NpcInteractionConsequenceKind.PROXY_STATE_ONLY);
        } else {
            kinds.add(NpcInteractionConsequenceKind.NO_OBSERVABLE_EFFECT);
        }
    }

    private long usefulCount(List<NpcInteractionConsequenceKind> kinds) {
        return kinds.stream().filter(kind -> kind != NpcInteractionConsequenceKind.PROXY_STATE_ONLY
                && kind != NpcInteractionConsequenceKind.NO_OBSERVABLE_EFFECT).count();
    }
}
