package io.github.kxyr0.echoanimation.forge.face;

import io.github.kxyr0.echoanimation.forge.network.AnimationNetworkChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerFaceAnimationState {
    private final Map<String, String> activeBySubject = new LinkedHashMap<>();
    private final Map<String, BlinkTimingOverride> blinkTimingBySubject = new LinkedHashMap<>();

    void set(MinecraftServer server, FaceAnimationCommandPayload payload) {
        activeBySubject.put(payload.subjectId(), payload.animationId());
        AnimationNetworkChannel.sendFaceAnimation(server, payload.subjectId(), payload.animationId());
    }

    void clear(MinecraftServer server, String subjectId) {
        activeBySubject.remove(subjectId);
        AnimationNetworkChannel.sendFaceAnimationClear(server, subjectId);
    }

    void clearAll(MinecraftServer server) {
        activeBySubject.clear();
        AnimationNetworkChannel.sendFaceAnimationClearAll(server);
    }

    void setBlinkTiming(MinecraftServer server, BlinkTimingCommandPayload payload) {
        blinkTimingBySubject.put(payload.subjectId(), payload.timing());
        AnimationNetworkChannel.sendBlinkTiming(server, payload.subjectId(), payload.timing());
    }

    void clearBlinkTiming(MinecraftServer server, String subjectId) {
        blinkTimingBySubject.remove(subjectId);
        AnimationNetworkChannel.sendBlinkTimingClear(server, subjectId);
    }

    public void sync(ServerPlayer player) {
        activeBySubject.forEach((subject, animation) ->
                AnimationNetworkChannel.sendFaceAnimation(player, subject, animation));
        blinkTimingBySubject.forEach((subject, timing) ->
                AnimationNetworkChannel.sendBlinkTiming(player, subject, timing));
    }
}
