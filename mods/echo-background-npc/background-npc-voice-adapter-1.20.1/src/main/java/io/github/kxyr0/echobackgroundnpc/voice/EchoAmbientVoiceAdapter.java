package io.github.kxyr0.echobackgroundnpc.voice;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import io.github.kxyr0.echovoice.forge.api.EchoVoiceServiceLookupEvent;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.forge.api.ambient.AmbientVoiceService;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import net.minecraftforge.common.MinecraftForge;

/** Optional Echo Voice boundary; never routes ambient speech into the foreground story lane. */
public final class EchoAmbientVoiceAdapter {
    public Result playNearby(String playbackToken, String dialogueId, AmbientLine line,
            WorldPoint speakerPosition) {
        if (line.voiceStem().isEmpty()) return new Result(Status.NO_ASSET, "No voice authored.");
        EchoVoiceServiceLookupEvent event = new EchoVoiceServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.ambientProviders().size() != 1) return new Result(Status.UNAVAILABLE, "Echo Voice unavailable.");
        var request = new AmbientVoiceService.SpatialRequest(playbackToken,
                VoiceAssetKey.of(DialogueId.parse(dialogueId), line.voiceStem().orElseThrow()),
                speakerPosition.x(), speakerPosition.y(), speakerPosition.z(), 14.0D, 0.72D);
        var result = event.ambientProviders().get(0).play(request);
        return switch (result.status()) {
            case STARTED, QUEUED -> new Result(Status.PLAYING, result.detail());
            case OUT_OF_RANGE -> new Result(Status.OUT_OF_RANGE, result.detail());
            case FOREGROUND_PRIORITY -> new Result(Status.SUPPRESSED, result.detail());
            case NO_ASSET -> new Result(Status.NO_ASSET, result.detail());
            case CONCURRENCY_LIMIT, FAILED -> new Result(Status.UNAVAILABLE, result.detail());
        };
    }

    public void stop(String playbackToken) {
        EchoVoiceServiceLookupEvent event = new EchoVoiceServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.ambientProviders().size() == 1) event.ambientProviders().get(0).stop(playbackToken);
    }

    public record Result(Status status, String message) { }
    public enum Status { PLAYING, NO_ASSET, OUT_OF_RANGE, SUPPRESSED, UNAVAILABLE }
}
