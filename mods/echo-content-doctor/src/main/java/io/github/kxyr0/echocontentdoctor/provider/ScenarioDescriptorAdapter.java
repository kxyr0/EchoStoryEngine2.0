package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDescriptor;
import java.util.ArrayList;
import java.util.List;

public final class ScenarioDescriptorAdapter {
    public ContentDescriptor adapt(ScenarioDefinition definition, ScenarioDescriptor scenario) {
        ContentReferenceKind kind = scenario.target().kind() == ScenarioTarget.Kind.SCENE
                ? ContentReferenceKind.SCENE : ContentReferenceKind.STORY;
        List<ContentReference> references = new ArrayList<>();
        references.add(new ContentReference(kind, new ContentResourceId(scenario.target().resourceId()),
                scenario.span()));
        definition.setup().forEach(step -> setupReference(step.service(), step.operation(), step.arguments())
                .ifPresent(referenceKind -> references.add(new ContentReference(referenceKind,
                        new ContentResourceId(step.arguments().get(0)), step.span()))));
        String id = "echo_scenario:" + scenario.id().value().replace('.', '/');
        return ContentDescriptor.simple(new ContentResourceId(id), ContentResourceKind.SCENARIO,
                scenario.span(), references);
    }

    private java.util.Optional<ContentReferenceKind> setupReference(String service, String operation,
            List<String> arguments) {
        if (arguments.isEmpty()) return java.util.Optional.empty();
        if (service.equals("Player") && operation.equals("Place")) {
            return java.util.Optional.of(ContentReferenceKind.LOCATION);
        }
        if (service.equals("Npc")) return java.util.Optional.of(ContentReferenceKind.NPC);
        if (service.equals("Observation")) return java.util.Optional.of(ContentReferenceKind.OBSERVATION);
        return java.util.Optional.empty();
    }
}
