package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.fact.AliasFact;
import io.github.kxyr0.echocontentdoctor.fact.LeaseFact;
import io.github.kxyr0.echocontentdoctor.fact.ScheduleCategory;
import io.github.kxyr0.echocontentdoctor.fact.ScheduleFact;
import io.github.kxyr0.echocontentdoctor.fact.VoiceFact;
import io.github.kxyr0.echocontentdoctor.fact.WaitFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentNode;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentDoctorTest {
    @Test
    void missingReferenceHasStableDiagnosticAndDeterministicSuggestion() {
        ContentDescriptor scene = simple("jdh:chemistry_scene", ContentResourceKind.SCENE, List.of(
                new ContentReference(ContentReferenceKind.DIALOGUE, id("jdh:chemistry_intro"), span(73))));
        ContentDescriptor dialogue = simple("jdh:chemistry_class_intro", ContentResourceKind.DIALOGUE, List.of());

        ContentDoctorResult result = doctor(scene, dialogue);

        var diagnostic = result.diagnostics().stream()
                .filter(value -> value.code().equals("ECHO_DOCTOR_REFERENCE_NOT_FOUND")).findFirst().orElseThrow();
        assertEquals(List.of(id("jdh:chemistry_class_intro")), diagnostic.suggestions());
        assertEquals(73, diagnostic.span().start().line());
    }

    @Test
    void detectsDeadNodeObservationScheduleWaitLeaseVoiceAndFormerlyProblems() {
        ContentDescriptor dialogue = descriptor("jdh:dialogue", ContentResourceKind.DIALOGUE, List.of(), List.of(
                new ContentNode("start", true, false, List.of(), span(1)),
                new ContentNode("dead", false, false, List.of(), span(8))), List.of(), List.of(), List.of(),
                List.of(), List.of(new VoiceFact(true, Optional.empty(), true, true, span(9))), true, true);
        ContentDescriptor observation = descriptor("jdh:observation", ContentResourceKind.OBSERVATION, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true, false);
        ContentDescriptor schedule = descriptor("jdh:schedule", ContentResourceKind.NPC, List.of(), List.of(), List.of(),
                List.of(new ScheduleFact("jdh:sasha", "09:00", ScheduleCategory.STATEFUL_EXCLUSIVE, "room_a", span(2)),
                        new ScheduleFact("jdh:sasha", "09:00", ScheduleCategory.STATEFUL_EXCLUSIVE, "room_b", span(3))),
                List.of(new WaitFact("wait", true, false, Optional.of("npc:sasha"), Optional.of("npc:sasha"), span(4))),
                List.of(new LeaseFact("camera", false, false, span(5))), List.of(), true, true);
        ContentDescriptor currentA = descriptor("jdh:new_a", ContentResourceKind.STORY, List.of(), List.of(),
                List.of(new AliasFact(id("jdh:old"), id("jdh:new_a"), span(6))), List.of(), List.of(), List.of(),
                List.of(), true, true);
        ContentDescriptor currentB = descriptor("jdh:new_b", ContentResourceKind.STORY, List.of(), List.of(),
                List.of(new AliasFact(id("jdh:old"), id("jdh:new_b"), span(7))), List.of(), List.of(), List.of(),
                List.of(), true, true);

        ContentDoctorResult result = doctor(dialogue, observation, schedule, currentA, currentB);
        List<String> codes = result.diagnostics().stream().map(value -> value.code()).toList();

        assertTrue(codes.contains("ECHO_DOCTOR_DIALOGUE_NODE_UNREACHABLE"));
        assertTrue(codes.contains("ECHO_DOCTOR_OBSERVATION_UNPLACED"));
        assertTrue(codes.contains("ECHO_DOCTOR_SCHEDULE_CONFLICT"));
        assertTrue(codes.contains("ECHO_DOCTOR_WAIT_IMPOSSIBLE"));
        assertTrue(codes.contains("ECHO_DOCTOR_LEASE_DEADLOCK"));
        assertTrue(codes.contains("ECHO_DOCTOR_LEASE_LEAK"));
        assertTrue(codes.contains("ECHO_DOCTOR_VOICE_INVALID"));
        assertTrue(codes.contains("ECHO_DOCTOR_FORMERLY_CONFLICT"));
    }

    @Test
    void validatesMigrationGraphDuringBuild() {
        MigrationDefinition only = new MigrationDefinition(new MigrationId("jdh:14_to_15"), version(14),
                version(15), 1, List.of());
        ContentDoctorInput input = new ContentDoctorInput(new ContentCatalogSnapshot(1, 0, List.of()),
                version(14), version(16), List.of(only));

        ContentDoctorResult result = new ContentDoctor().validate(input);

        assertTrue(result.diagnostics().stream().anyMatch(value ->
                value.code().equals("ECHO_DOCTOR_SAVE_MIGRATION_PATH_MISSING")));
    }

    private ContentDoctorResult doctor(ContentDescriptor... descriptors) {
        return new ContentDoctor().validate(ContentDoctorInput.catalogOnly(
                new ContentCatalogSnapshot(4, descriptors.length, List.of(descriptors))));
    }

    private ContentDescriptor simple(String id, ContentResourceKind kind, List<ContentReference> references) {
        return ContentDescriptor.simple(id(id), kind, span(1), references);
    }

    private ContentDescriptor descriptor(String value, ContentResourceKind kind, List<ContentReference> references,
            List<ContentNode> nodes, List<AliasFact> aliases, List<ScheduleFact> schedules, List<WaitFact> waits,
            List<LeaseFact> leases, List<VoiceFact> voices, boolean enabled, boolean placed) {
        return new ContentDescriptor(id(value), kind, span(1), references, nodes, aliases, schedules, waits, leases,
                voices, List.of(), enabled, placed);
    }

    private ContentResourceId id(String value) { return new ContentResourceId(value); }
    private SaveSchemaVersion version(int value) { return new SaveSchemaVersion(value); }

    private SourceSpan span(int line) {
        SourcePosition point = new SourcePosition(0, line, 1);
        return new SourceSpan(new SourceId("chapter_1/day_02/university.es"), 0, 0, point, point);
    }
}
