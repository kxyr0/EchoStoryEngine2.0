package io.github.kxyr0.echofiremagic.forge.api.event;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

public final class BlacksmithHeatBlockEvent extends Event {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final BlockPos pos;
    private final BlockState state;
    private boolean handled;

    public BlacksmithHeatBlockEvent(ServerLevel level, ServerPlayer player, BlockPos pos,
                                    BlockState state) {
        this.level = Objects.requireNonNull(level, "level");
        this.player = Objects.requireNonNull(player, "player");
        this.pos = Objects.requireNonNull(pos, "pos").immutable();
        this.state = Objects.requireNonNull(state, "state");
    }

    public ServerLevel level() { return level; }
    public ServerPlayer player() { return player; }
    public BlockPos pos() { return pos; }
    public BlockState state() { return state; }
    public boolean handled() { return handled; }
    public void markHandled() { handled = true; }
}
