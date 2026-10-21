package io.github.kxyr0.echobackgroundnpc.echoscript;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BackgroundScriptCompilerTest {
    private final BackgroundScriptCompiler compiler = new BackgroundScriptCompiler();

    @Test
    void compilesCanonicalHighLevelDslAndTypedSymbol() {
        var result = compiler.compile(List.of(
                source("npcs/student_anton.es", """
                        class StudentAnton : BackgroundNpcResource {
                            Home("jdh:dorm_anton");
                            At("09:00") { GoTo("jdh:university"); }
                        }
                        """),
                source("npcs/student_mira.es", """
                        class StudentMira : BackgroundNpcResource {
                            Profile("jdh:university_student");
                            Home("jdh:dorm_mira");
                            At("08:40") { GoTo("jdh:university_gate"); }
                            At("09:50") { TalkWith(StudentAnton, "jdh:before_class"); }
                            At("10:00") { GoTo("jdh:room_204"); }
                        }
                        """),
                source("profiles/university_student.es", """
                        class RenamableClass : BackgroundNpcProfileResource {
                            AppearancePool("jdh:university_students");
                            WalkSpeed(1.05);
                            LocomotionProfile("jdh:human_default");
                        }
                        """),
                source("appearance_pools/university_students.es", """
                        class Students : BackgroundAppearancePoolResource {
                            Skin("jdh:student_01"); Skin("jdh:student_02");
                        }
                        """),
                source("ambient_dialogues/before_class.es", """
                        class BeforeClass : AmbientDialogueResource {
                            Line("mira", "Ты домашку сделала?");
                            Line("anton", "Я вообще забыл.", voice: Voice("anton_02"));
                            OnComplete() { StartQuest("jdh:find_notebook", range: 8.0); }
                        }
                        """)), 7L);

        assertTrue(result.catalog().isPresent(), () -> result.diagnostics().toString());
        var catalog = result.catalog().orElseThrow();
        var mira = catalog.definitions().get(new BackgroundNpcId("jdh:student_mira"));
        assertEquals(3, mira.schedule().size());
        BackgroundTask.Converse talk = assertInstanceOf(BackgroundTask.Converse.class, mira.schedule().get(1).task());
        assertEquals(new BackgroundNpcId("jdh:student_anton"), talk.participant());
        assertEquals("jdh:university_student", mira.profile().orElseThrow().value());
        assertEquals("anton_02", catalog.dialogues().values().iterator().next().lines().get(1)
                .voiceStem().orElseThrow());
        var quest = catalog.dialogues().values().iterator().next().completionQuest().orElseThrow();
        assertEquals("jdh:find_notebook", quest.questId().value());
        assertEquals(8.0D, quest.maximumDistance());
    }

    @Test
    void rejectsUnknownSymbolAndInvalidTime() {
        var result = compiler.compile(List.of(source("npcs/mira.es", """
                class Mira : BackgroundNpcResource {
                    Home("jdh:home");
                    At("25:61") { TalkWith(Missing, "jdh:talk"); }
                }
                """)), 1L);
        assertTrue(result.catalog().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.error()));
    }

    @Test
    void resourceIdComesFromFileAndNotClassName() {
        var result = compiler.compile(List.of(source("npcs/stable_name.es", """
                class CompletelyRenamed : BackgroundNpcResource { Home("jdh:home"); }
                """)), 1L);
        assertTrue(result.catalog().isPresent(), () -> result.diagnostics().toString());
        assertTrue(result.catalog().orElseThrow().definitions()
                .containsKey(new BackgroundNpcId("jdh:stable_name")));
    }

    @Test
    void rejectsUnsafeQuestCompletionRange() {
        var result = compiler.compile(List.of(source("ambient_dialogues/talk.es", """
                class Talk : AmbientDialogueResource {
                    Line("mira", "Поговорили.");
                    OnComplete() { StartQuest("jdh:find_notebook", range: 40.0); }
                }
                """)), 1L);
        assertTrue(result.catalog().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("range")));
    }

    private BackgroundScriptSource source(String path, String text) {
        return new BackgroundScriptSource("data/jdh/echo_background_npc/" + path, text);
    }
}
