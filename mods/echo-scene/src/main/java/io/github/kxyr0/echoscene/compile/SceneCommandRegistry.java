package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SceneCommandRegistry {
    private final List<SceneCommandRule> rules;

    public SceneCommandRegistry(List<SceneCommandRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<SceneCommandRule> find(SceneSymbol receiver, String method) {
        return rules.stream().filter(rule -> rule.receiverKind() == receiver.kind())
                .filter(rule -> rule.receiverName().isEmpty() || rule.receiverName().equals(receiver.name()))
                .filter(rule -> rule.method().equals(method)).findFirst();
    }

    public static SceneCommandRegistry standard() {
        List<SceneCommandRule> rules = new ArrayList<>();
        rules.add(new SceneCommandRule(SceneSymbolKind.SERVICE, "TeleportMagic", "To",
                List.of(SceneArgumentKind.ACTOR, SceneArgumentKind.LOCATION),
                BlockingMode.DURABLE_BLOCKING, symbol -> List.of()));
        rules.add(rule(SceneSymbolKind.NPC, "MoveTo", SceneArgumentKind.LOCATION,
                BlockingMode.DURABLE_BLOCKING, symbol -> capability(SceneCapabilityKind.NPC, symbol.resourceId())));
        rules.add(rule(SceneSymbolKind.PLAYER, "MoveTo", SceneArgumentKind.LOCATION,
                BlockingMode.DURABLE_BLOCKING, symbol -> capability(SceneCapabilityKind.PLAYER, symbol.resourceId())));
        rules.add(rule(SceneSymbolKind.NPC, "Play", SceneArgumentKind.ANIMATION,
                BlockingMode.DURABLE_BLOCKING, symbol -> capability(SceneCapabilityKind.NPC, symbol.resourceId())));
        rules.add(service("Camera", "Use", SceneArgumentKind.CAMERA, BlockingMode.INSTANT,
                capability(SceneCapabilityKind.CAMERA, "camera")));
        rules.add(service("Dialogue", "Play", SceneArgumentKind.DIALOGUE, BlockingMode.DURABLE_BLOCKING, List.of()));
        rules.add(service("Dialogue", "Start", SceneArgumentKind.DIALOGUE, BlockingMode.FIRE_AND_CONTINUE, List.of()));
        rules.add(service("Scene", "Play", SceneArgumentKind.SCENE, BlockingMode.DURABLE_BLOCKING, List.of()));
        rules.add(service("Time", "Set", SceneArgumentKind.VALUE, BlockingMode.INSTANT, List.of()));
        rules.add(service("Time", "Add", SceneArgumentKind.VALUE, BlockingMode.INSTANT, List.of()));
        rules.add(service("Time", "WaitUntil", SceneArgumentKind.VALUE, BlockingMode.DURABLE_BLOCKING, List.of()));
        return new SceneCommandRegistry(rules);
    }

    private static SceneCommandRule rule(SceneSymbolKind kind, String method, SceneArgumentKind argument,
            BlockingMode mode, java.util.function.Function<SceneSymbol, List<SceneCapability>> capabilities) {
        return new SceneCommandRule(kind, "", method, List.of(argument), mode, capabilities);
    }

    private static SceneCommandRule service(String name, String method, SceneArgumentKind argument,
            BlockingMode mode, List<SceneCapability> capabilities) {
        return new SceneCommandRule(SceneSymbolKind.SERVICE, name, method, List.of(argument), mode,
                symbol -> capabilities);
    }

    private static List<SceneCapability> capability(SceneCapabilityKind kind, String resource) {
        return List.of(new SceneCapability(kind, resource));
    }
}
