package io.github.kxyr0.echoplayervitals.forge.command;

import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;

/**
 * Formats concise command output for Vitals inspection.
 */
final class PlayerVitalsCommandText {
    String inspect(PlayerVitalsSnapshot snapshot) {
        return "fatigue=" + snapshot.fatigue().fatigue().current() + "/"
                + snapshot.fatigue().fatigue().maximum()
                + " reserve=" + snapshot.fatigue().reserve().current() + "/"
                + snapshot.fatigue().reserve().maximum()
                + " normalizedReserve=" + snapshot.fatigue().reserve().normalized()
                + " stage=" + snapshot.fatigue().stage()
                + " movement=" + snapshot.fatigue().movementMultiplier()
                + " nausea=" + snapshot.fatigue().nauseaIntensity()
                + " health=" + snapshot.health().current() + "/" + snapshot.health().maximum()
                + " hungerFood=" + snapshot.hunger().foodLevel()
                + " foodRegen=" + snapshot.hunger().foodBasedRegeneration()
                + " sprintFoodGate=" + snapshot.hunger().sprintRestrictedByFood()
                + " revision=" + snapshot.revision().value();
    }
}
