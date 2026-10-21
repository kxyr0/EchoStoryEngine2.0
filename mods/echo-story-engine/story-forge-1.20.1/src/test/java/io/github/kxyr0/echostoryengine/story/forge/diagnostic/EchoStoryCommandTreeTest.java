package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogReloadStatus;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.ExecutionSummary;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHostState;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostHealth;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class EchoStoryCommandTreeTest {
    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");

    @Test
    void rootContainsRequiredLiteralsOnly() {
        CommandDispatcher<FakeSource> dispatcher = dispatcher();

        Set<String> children = dispatcher.getRoot().getChild("echo_story").getChildren().stream()
                .map(node -> node.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("health", "list", "stories", "executions", "execution", "extensions", "extension",
                "diagnostics"), children);
    }

    @Test
    void requiredCommandsExecuteAndSendRussianOutput() throws CommandSyntaxException {
        FakeSource source = new FakeSource(true);
        CommandDispatcher<FakeSource> dispatcher = dispatcher();

        dispatcher.execute("echo_story health", source);
        dispatcher.execute("echo_story list", source);
        dispatcher.execute("echo_story stories", source);
        dispatcher.execute("echo_story executions", source);
        dispatcher.execute("echo_story execution sample:execution", source);
        dispatcher.execute("echo_story extensions", source);
        dispatcher.execute("echo_story extension sample:missing", source);
        dispatcher.execute("echo_story diagnostics", source);
        dispatcher.execute("echo_story diagnostics 20", source);

        String output = source.output();
        assertTrue(output.contains("Состояние Echo Story Engine"));
        assertTrue(output.contains("Story в каталоге"));
        assertTrue(output.contains("sample:execution история=sample:story фаза=WAITING узел=action"));
        assertTrue(output.contains("типОжидающегоДействия=sample:action"));
        assertTrue(output.contains("ПРЕДУПРЕЖДЕНИЕ ESE-FORGE-ACTION-001"));
        assertTrue(output.contains("Очередь сохранения"));
        assertTrue(output.contains("Недавних предупреждений/ошибок"));
    }

    @Test
    void diagnosticsLimitRejectsInvalidValuesInRussian() throws CommandSyntaxException {
        for (String command : List.of(
                "echo_story diagnostics 0",
                "echo_story diagnostics -1",
                "echo_story diagnostics abc",
                "echo_story diagnostics 51")) {
            FakeSource source = new FakeSource(true);
            dispatcher().execute(command, source);
            assertTrue(source.output().contains("Лимит должен быть целым числом от 1 до 50."));
        }
    }

    @Test
    void executionIdErrorsAreRussianAndBounded() throws CommandSyntaxException {
        FakeSource source = new FakeSource(true);
        CommandDispatcher<FakeSource> dispatcher = dispatcher();

        dispatcher.execute("echo_story execution bad-id", source);
        dispatcher.execute("echo_story execution sample:missing", source);

        assertTrue(source.output().contains("ID execution должен иметь формат namespace:path."));
        assertTrue(source.output().contains("Выполнение sample:missing не найдено."));
    }

    @Test
    void permissionRequirementHidesCommandTree() {
        FakeSource source = new FakeSource(false);
        CommandDispatcher<FakeSource> dispatcher = dispatcher();

        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("echo_story health", source));
    }

    private CommandDispatcher<FakeSource> dispatcher() {
        CommandDispatcher<FakeSource> dispatcher = new CommandDispatcher<>();
        StoryCatalogState catalogState = new StoryCatalogState(NOW);
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(16);
        buffer.accept(new ForgeRuntimeDiagnostics().commandHandlerMissing(
                ExecutionId.of("sample:execution"),
                "sample:action",
                NOW));
        HostSnapshot snapshot = new HostSnapshot(health(catalogState), List.of(execution()));
        EchoStoryCommandTree.CommandSurface<FakeSource> surface =
                new EchoStoryCommandTree.CommandSurface<>() {
                    @Override
                    public boolean hasPermission(FakeSource checked) {
                        return checked.permitted();
                    }

                    @Override
                    public void send(FakeSource target, String line) {
                        target.lines().add(line);
                    }
                };
        new EchoStoryCommandTree<>(buffer, () -> Optional.of(snapshot), catalogState, ExtensionCatalog::empty,
                surface).register(dispatcher);
        return dispatcher;
    }

    private HostHealth health(StoryCatalogState catalogState) {
        return new HostHealth(
                ForgeServerHostState.ACTIVE,
                catalogState.current().revision(),
                catalogState.current().storyCount(),
                new CatalogReloadStatus("ACCEPTED", 1, 1, 0, NOW),
                1,
                1,
                0,
                64,
                0,
                0);
    }

    private ExecutionSummary execution() {
        return new ExecutionSummary(
                ExecutionId.of("sample:execution"),
                StoryId.of("sample:story"),
                StoryExecutionPhase.WAITING,
                "action",
                "sample:action");
    }

    private record FakeSource(boolean permitted, List<String> lines) {
        private FakeSource(boolean permitted) {
            this(permitted, new ArrayList<>());
        }

        private String output() {
            return String.join("\n", lines);
        }
    }
}
