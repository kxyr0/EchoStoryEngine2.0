package io.github.kxyr0.echonpc.forge.gametest;

import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.definition;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.entities;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.mark;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.pos;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.request;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.service;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.source;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.actor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinitionCatalog;
import io.github.kxyr0.echonpc.echoscript.EchoScriptNpcCompiler;
import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.resource.NpcCatalogCompiler;
import io.github.kxyr0.echonpc.forge.resource.NpcResourceCandidate;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(EchoNpcMod.MOD_ID)
public final class ForgeNpcGameTests {
    private ForgeNpcGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void hostStarts(GameTestHelper helper) {
        mark("ForgeNpcGameTests.hostStarts");
        helper.assertTrue(service(helper).health().active(), "Echo NPC host must be active");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void commandsParseNamespacedIds(GameTestHelper helper) {
        mark("ForgeNpcGameTests.commandsParseNamespacedIds");
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withPermission(4);
        assertCommandParses(helper, dispatcher, source,
                "echo_npc spawn sample:gt_command sample:father");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc spawn sample:gt_command sample:father ~ ~ ~");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc actor sample:gt_command");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc teleport sample:gt_command ~ ~ ~");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc despawn sample:gt_command");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc skin set sample:gt_command sample:father/backpack");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc spawn_at sample:gt_command ~ ~ ~ 90 0");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc sleep_at sample:gt_command ~ ~ ~ minecraft:red_bed");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc wake_up sample:gt_command");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc inspect_rest sample:gt_command");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc inspect_bed ~ ~ ~");
        assertCommandParses(helper, dispatcher, source,
                "echo_npc rest_providers");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void npcResourceCompiles(GameTestHelper helper) {
        mark("ForgeNpcGameTests.npcResourceCompiles");
        var result = new EchoScriptNpcCompiler().compile("sample:father.npc.es", source("sample:father", "Отец"));
        helper.assertTrue(result.definition().isPresent(), "valid NpcResource must compile");
        helper.assertTrue(result.definition().orElseThrow().displayName().equals("Отец"), "UTF-8 display survives");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void catalogInstalls(GameTestHelper helper) {
        mark("ForgeNpcGameTests.catalogInstalls");
        NpcCatalogState state = new NpcCatalogState();
        state.install(NpcDefinitionCatalog.of(1L, List.of(definition("sample:father", "Отец"))));
        helper.assertTrue(state.current().definitions().size() == 1, "catalog must install atomically");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidReloadRetainsCatalog(GameTestHelper helper) {
        mark("ForgeNpcGameTests.invalidReloadRetainsCatalog");
        NpcCatalogState state = new NpcCatalogState();
        state.install(NpcDefinitionCatalog.of(1L, List.of(definition("sample:father", "Отец"))));
        NpcResourceCandidate invalid = new NpcResourceCandidate(
                new ResourceLocation("sample", "echo_npc/npcs/bad.npc.es"),
                "class Bad : NpcResource { public override NpcResourceDef Build() { "
                        + "return Npc(display: \"Ошибочный\"); } }");
        var result = new NpcCatalogCompiler(new EchoScriptNpcCompiler()).compile(List.of(invalid), 2L);
        helper.assertTrue(result.catalog().isEmpty(), "invalid reload must not produce a replacement catalog");
        helper.assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()),
                "invalid reload must report diagnostics");
        helper.assertTrue(state.current().revision() == 1L, "invalid reload keeps known-good catalog revision");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spawnCreatesOneEntity(GameTestHelper helper) {
        mark("ForgeNpcGameTests.spawnCreatesOneEntity");
        NpcService service = service(helper);
        NpcPosition pos = pos(helper, 1);
        String actorId = actor("spawn_one");
        var result = service.spawn(request(actorId, "sample:father", helper, pos));
        helper.assertTrue(result.status() == NpcResults.SpawnStatus.QUEUED, "spawn operation must be queued");
        helper.assertTrue(result.operationId().isPresent(), "spawn operation id must be returned");
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1,
                    "exactly one EchoNpcEntity must exist near spawn");
            service.despawn(new ActorId(actorId));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spawnedEntityHasActorId(GameTestHelper helper) {
        mark("ForgeNpcGameTests.spawnedEntityHasActorId");
        NpcPosition pos = pos(helper, 2);
        NpcService service = service(helper);
        String actorId = actor("actor_id");
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "entity must materialize");
            EchoNpcEntity entity = entities(helper, pos, actorId).get(0);
            helper.assertTrue(entity.actorIdValue().equals(actorId), "entity must sync logical ActorId");
            service.despawn(new ActorId(actorId));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spawnedEntityHasDefinitionData(GameTestHelper helper) {
        mark("ForgeNpcGameTests.spawnedEntityHasDefinitionData");
        NpcPosition pos = pos(helper, 3);
        NpcService service = service(helper);
        String actorId = actor("definition_data");
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "entity must materialize");
            EchoNpcEntity entity = entities(helper, pos, actorId).get(0);
            helper.assertTrue(entity.displayNameValue().equals("Отец"), "display name must sync");
            helper.assertTrue(entity.skinIdValue().equals("sample:father"), "skin ID must sync");
            service.despawn(new ActorId(actorId));
        });
    }

    private static void assertCommandParses(GameTestHelper helper, CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source, String command) {
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
        helper.assertTrue(parsed.getReader().getRemaining().isEmpty(), command + " must parse to the end");
        helper.assertTrue(parsed.getExceptions().isEmpty(), command + " must not report parse exceptions");
    }
}
