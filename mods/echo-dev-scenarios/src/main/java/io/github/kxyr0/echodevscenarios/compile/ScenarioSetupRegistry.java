package io.github.kxyr0.echodevscenarios.compile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ScenarioSetupRegistry {
    private final Map<String, Rule> rules;

    public ScenarioSetupRegistry(List<Rule> rules) {
        this.rules = rules.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                value -> value.service() + "." + value.operation(), value -> value));
    }

    public Optional<Rule> find(String service, String operation) {
        return Optional.ofNullable(rules.get(service + "." + operation));
    }

    public static ScenarioSetupRegistry standard() {
        return new ScenarioSetupRegistry(List.of(
                new Rule("Time", "Set", List.of(Argument.VALUE, Argument.VALUE), true),
                new Rule("Player", "Place", List.of(Argument.LOCATION), true),
                new Rule("Npc", "Ensure", List.of(Argument.NPC), true),
                new Rule("Npc", "Form", List.of(Argument.NPC, Argument.FORM), true),
                new Rule("Wardrobe", "Set", List.of(Argument.VALUE, Argument.OUTFIT), true),
                new Rule("Observation", "Activate", List.of(Argument.OBSERVATION), true),
                new Rule("Observation", "Enable", List.of(Argument.OBSERVATION), true)));
    }

    public enum Argument {
        VALUE,
        NPC,
        LOCATION,
        FORM,
        OUTFIT,
        OBSERVATION
    }

    public record Rule(String service, String operation, List<Argument> arguments, boolean blocking) {
        public Rule { arguments = List.copyOf(arguments); }
    }
}
