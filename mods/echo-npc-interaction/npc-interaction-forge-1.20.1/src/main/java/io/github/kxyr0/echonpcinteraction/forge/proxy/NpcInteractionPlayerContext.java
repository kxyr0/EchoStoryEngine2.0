package io.github.kxyr0.echonpcinteraction.forge.proxy;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class NpcInteractionPlayerContext {
    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("echo_npc_interaction:proxy".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            "EchoNpcUseProxy");

    public ServerPlayer proxy(ServerLevel level) {
        return FakePlayerFactory.get(level, PROFILE);
    }
}
