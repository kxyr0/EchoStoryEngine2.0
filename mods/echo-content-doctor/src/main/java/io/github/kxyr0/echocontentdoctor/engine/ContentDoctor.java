package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraphBuilder;
import io.github.kxyr0.echocontentdoctor.validation.reference.ReferenceValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.AliasValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.MigrationValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.ObservationTriggerValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.ScheduleValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.VoiceValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.WaitLeaseValidator;
import io.github.kxyr0.echocontentdoctor.validation.semantic.InspectionBindingValidator;
import io.github.kxyr0.echocontentdoctor.validation.structural.CollisionValidator;
import io.github.kxyr0.echocontentdoctor.validation.structural.ReachabilityValidator;
import io.github.kxyr0.echocontentdoctor.validation.structural.SceneRecursionValidator;
import java.util.ArrayList;
import java.util.List;

public final class ContentDoctor {
    public ContentDoctorResult validate(ContentDoctorInput input) {
        ContentGraph graph = new ContentGraphBuilder().build(input.catalog());
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(new CollisionValidator().validate(graph));
        diagnostics.addAll(new ReferenceValidator().validate(graph));
        diagnostics.addAll(new io.github.kxyr0.echocontentdoctor.validation.events.EventSubscriptionValidator().validate(graph));
        diagnostics.addAll(new InspectionBindingValidator().validate(graph));
        diagnostics.addAll(new ReachabilityValidator().validate(graph));
        diagnostics.addAll(new SceneRecursionValidator().validate(graph));
        diagnostics.addAll(new ScheduleValidator().validate(graph));
        diagnostics.addAll(new WaitLeaseValidator().validate(graph));
        diagnostics.addAll(new ObservationTriggerValidator().validate(graph));
        diagnostics.addAll(new VoiceValidator().validate(graph));
        diagnostics.addAll(new AliasValidator().validate(graph));
        diagnostics.addAll(new MigrationValidator().validate(input));
        int references = input.catalog().resources().stream().mapToInt(value -> value.references().size()).sum();
        return new ContentDoctorResult(input.catalog().revision(), input.catalog().files(),
                input.catalog().resources().size(), references, diagnostics);
    }
}
