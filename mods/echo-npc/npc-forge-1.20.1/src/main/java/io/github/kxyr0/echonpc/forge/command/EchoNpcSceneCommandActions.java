package io.github.kxyr0.echonpc.forge.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceRequest;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

final class EchoNpcSceneCommandActions {
    private final Supplier<Optional<ForgeNpcHost>> host;

    EchoNpcSceneCommandActions(Supplier<Optional<ForgeNpcHost>> host) {
        this.host = java.util.Objects.requireNonNull(host, "host");
    }

    int spawnAt(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        var pos = Vec3Argument.getVec3(context, "pos");
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        var request = new SpawnNpcRequest(npcId, new NpcDefinitionId(npcId.value()),
                ForgeWorldIds.fromLevel(context.getSource().getLevel()),
                new NpcPosition(pos.x, pos.y, pos.z), new NpcRotation(yaw, pitch));
        host.get().ifPresentOrElse(value -> send(context,
                EchoNpcCommandFormatting.mutation(value.service().spawnAt(request))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int placeAt(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        var pos = Vec3Argument.getVec3(context, "pos");
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        var request = new NpcPlaceRequest(npcId, ForgeWorldIds.fromLevel(context.getSource().getLevel()),
                new NpcPosition(pos.x, pos.y, pos.z), new NpcRotation(yaw, pitch));
        host.get().ifPresentOrElse(value -> send(context,
                EchoNpcCommandFormatting.mutation(value.service().placeAt(request))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int sleepAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ActorId npcId = npcId(context);
        BlockPosition bed = blockPosition(BlockPosArgument.getLoadedBlockPos(context, "bed"));
        BlockId expected = new BlockId(ResourceLocationArgument.getId(context, "expectedBlock").toString());
        var request = new NpcSleepRequest(npcId, ForgeWorldIds.fromLevel(context.getSource().getLevel()),
                bed, expected);
        host.get().ifPresentOrElse(value -> send(context, value.service().sleepAt(request).toString()),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int wakeUp(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        host.get().ifPresentOrElse(value -> send(context,
                value.service().wakeUp(new NpcWakeRequest(npcId)).toString()),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int inspectRest(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        host.get().ifPresentOrElse(value -> send(context,
                value.service().inspectRest(npcId).map(Object::toString).orElse("NPC не спит.")),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int inspectBed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "bed");
        BlockState state = context.getSource().getLevel().getBlockState(pos);
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String actual = key == null ? "<unknown>" : key.toString();
        boolean bed = state.getBlock() instanceof BedBlock;
        boolean occupied = bed && state.hasProperty(BedBlock.OCCUPIED) && state.getValue(BedBlock.OCCUPIED);
        send(context, "bed=" + blockPosition(pos) + " actualBlock=" + actual
                + " vanillaBed=" + bed + " occupied=" + occupied);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int restProviders(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> value.restProviders().providers()
                .forEach(provider -> send(context, provider.providerId())),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private ActorId npcId(CommandContext<CommandSourceStack> context) {
        return new ActorId(ResourceLocationArgument.getId(context, "npcId").toString());
    }

    private BlockPosition blockPosition(BlockPos pos) {
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    private void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
