package io.github.kxyr0.echonpc.forge.gametest;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.definition.NpcDefinitionCatalog;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

final class ForgeNpcGameTestSupport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RUN_SUFFIX = Long.toString(Math.abs(System.nanoTime()), 36);

    private ForgeNpcGameTestSupport() {
    }

    static NpcService service(GameTestHelper helper) {
        return EchoNpcServiceBridge.lookup(helper.getLevel().getServer()).service().orElseThrow();
    }

    static ForgeNpcHost localHost(GameTestHelper helper, NpcCatalogState state) {
        ForgeNpcHost host = new ForgeNpcHost(helper.getLevel().getServer(), state, diagnostic -> { });
        host.start();
        return host;
    }

    static String actor(String leafName) {
        return "sample:gt_" + RUN_SUFFIX + "_" + leafName;
    }

    static NpcCatalogState installedState() {
        NpcCatalogState state = new NpcCatalogState();
        state.install(NpcDefinitionCatalog.of(1L, List.of(definition("sample:father", "Отец"))));
        return state;
    }

    static SpawnNpcRequest request(String actorId, String definitionId, GameTestHelper helper, NpcPosition position) {
        return new SpawnNpcRequest(new ActorId(actorId), new NpcDefinitionId(definitionId),
                ForgeWorldIds.fromLevel(helper.getLevel()), position, NpcRotation.zero());
    }

    static NpcPosition pos(GameTestHelper helper, int xOffset) {
        BlockPos pos = helper.absolutePos(new BlockPos(xOffset, 2, 0));
        BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))
                .forEach(block -> helper.getLevel().setBlockAndUpdate(block, Blocks.AIR.defaultBlockState()));
        return new NpcPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    static List<EchoNpcEntity> entities(GameTestHelper helper, NpcPosition position) {
        return helper.getLevel().getEntitiesOfClass(EchoNpcEntity.class,
                new AABB(position.x(), position.y(), position.z(), position.x(), position.y(), position.z())
                        .inflate(1.0D));
    }

    static List<EchoNpcEntity> entities(GameTestHelper helper, NpcPosition position, String actorId) {
        return entities(helper, position).stream()
                .filter(entity -> actorId.equals(entity.actorIdValue()))
                .toList();
    }

    static NpcDefinition definition(String id, String displayName) {
        return NpcDefinition.humanoid(new NpcDefinitionId(id), displayName,
                Optional.of(new NpcSkinId("sample:father")));
    }

    static String source(String id, String displayName) {
        return "class TestNpc : NpcResource { public override NpcResourceDef Build() { return Npc(id: \""
                + id + "\", display: \"" + displayName + "\", skin: Skin(\"sample:father\")); } }";
    }

    static void mark(String testName) {
        LOGGER.info("ENPC-GAMETEST-RUN {}", testName);
    }
}
