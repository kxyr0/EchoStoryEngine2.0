package io.github.kxyr0.echomagic.testmod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoMagicTestmod.MOD_ID)
public final class EchoMagicTestmod {
    public static final String MOD_ID = "echo_magic_test";
    public EchoMagicTestmod() { MinecraftForge.EVENT_BUS.addListener(this::commands); }
    private void commands(RegisterCommandsEvent event) { register(event.getDispatcher()); }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_magic_test").requires(source -> source.hasPermission(2))
                .then(Commands.literal("arena").executes(context -> arena(context.getSource()))));
    }

    private int arena(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer caster = source.getPlayerOrException();
        ServerLevel level = caster.serverLevel();
        Vec3 forward = caster.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 origin = caster.position();
        ArmorStand ally = dummy(level, origin.add(forward.scale(4)), "Magic Ally");
        ArmorStand target = dummy(level, origin.add(forward.scale(8)), "Magic Target");
        dummy(level, origin.add(forward.scale(12)), "Behind Wall");
        var scoreboard = level.getScoreboard();
        var team = scoreboard.getPlayerTeam("echo_magic_test_allies");
        if (team == null) team = scoreboard.addPlayerTeam("echo_magic_test_allies");
        scoreboard.addPlayerToTeam(caster.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        BlockPos wall = BlockPos.containing(origin.add(forward.scale(10)));
        for (int y = 0; y < 3; y++) level.setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
        source.sendSuccess(() -> Component.literal("Arena ready: ally at 4, target at 8, wall at 10, blocked target at 12."), false);
        return target.isAlive() ? 1 : 0;
    }

    private ArmorStand dummy(ServerLevel level, Vec3 position, String name) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        if (stand == null) throw new IllegalStateException("Cannot create armor stand.");
        stand.setPos(position.x, position.y, position.z);
        stand.setCustomName(Component.literal(name));
        stand.setCustomNameVisible(true);
        stand.setNoGravity(true);
        level.addFreshEntity(stand);
        return stand;
    }
}

