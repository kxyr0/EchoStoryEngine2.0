package io.github.kxyr0.echoanimation.forge.client.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.client.FaceLayerTextureCache;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Optional reflection bridge to Echo Dialogue's client portrait events.
 */
public final class DialogueVisualFaceLayerBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ECHO_DIALOGUE_MOD_ID = "echo_dialogue";
    private static final String VISUAL_EVENT_CLASS =
            "io.github.kxyr0.echo" + "dialogue.forge.api.Echo" + "DialogueSpeakerVisualRenderEvent";
    private static final String OPEN_EVENT_CLASS =
            "io.github.kxyr0.echo" + "dialogue.forge.network.event.Clientbound" + "DialogueOpenEvent";
    private static final String CLOSE_EVENT_CLASS =
            "io.github.kxyr0.echo" + "dialogue.forge.network.event.Clientbound" + "DialogueCloseEvent";
    private static final String BASE_PLAYER_FACE_RIG = "echo_content_player:player/base";
    private static final String ECHO_WARDROBE_JOHN_PREFIX = "jdh:echo_wardrobe/skins/combo/john_";
    private static final String JOHN_SPEAKER_ID = "jdh:john";
    private static final Set<String> REPORTED_PLAY_KEYS = ConcurrentHashMap.newKeySet();

    private DialogueVisualFaceLayerBridge() {
    }

    public static void register(AnimationCatalogState catalogState, FaceLayerTextureCache textureCache) {
        if (!ModList.get().isLoaded(ECHO_DIALOGUE_MOD_ID)) {
            return;
        }
        DialoguePortraitAnimationState animationState = DialoguePortraitAnimationState.shared();
        DialogueVisualFaceLayerRenderer renderer =
                new DialogueVisualFaceLayerRenderer(catalogState, textureCache, animationState);
        registerVisualBridge(renderer);
        registerOpenBridge(animationState);
        registerCloseBridge(animationState);
    }

    private static void registerVisualBridge(DialogueVisualFaceLayerRenderer renderer) {
        try {
            Class<? extends Event> eventClass = Class.forName(VISUAL_EVENT_CLASS).asSubclass(Event.class);
            DialogueEventAccess.Visual access = DialogueEventAccess.Visual.create(eventClass);
            subscribe(eventClass, event -> render(access, renderer, event));
            LOGGER.info("Echo Dialogue portrait face layer render bridge registered.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Dialogue portrait face layer render bridge is unavailable: {}",
                    exception.toString());
        }
    }

    private static void registerOpenBridge(DialoguePortraitAnimationState state) {
        try {
            Class<? extends Event> eventClass = Class.forName(OPEN_EVENT_CLASS).asSubclass(Event.class);
            DialogueEventAccess.Open access = DialogueEventAccess.Open.create(eventClass);
            subscribe(eventClass, event -> open(access, state, event));
            LOGGER.info("Echo Dialogue portrait face animation open bridge registered.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Dialogue portrait face animation open bridge is unavailable: {}",
                    exception.toString());
        }
    }

    private static void registerCloseBridge(DialoguePortraitAnimationState state) {
        try {
            Class<? extends Event> eventClass = Class.forName(CLOSE_EVENT_CLASS).asSubclass(Event.class);
            subscribe(eventClass, event -> state.clear());
            LOGGER.info("Echo Dialogue portrait face animation close bridge registered.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Dialogue portrait face animation close bridge is unavailable: {}",
                    exception.toString());
        }
    }

    private static void render(DialogueEventAccess.Visual access, DialogueVisualFaceLayerRenderer renderer,
            Event event) {
        try {
            renderer.render(access.graphics(event), access.visualId(event), access.bounds(event));
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.debug("Failed to handle Echo Dialogue speaker visual event.", exception);
        }
    }

    private static void open(DialogueEventAccess.Open access, DialoguePortraitAnimationState state, Event event) {
        try {
            Optional<String> visualId = access.visualId(event);
            Optional<String> speakerId = access.speakerId(event);
            long gameTick = sampleTick();
            if (visualId.isEmpty()) {
                state.clear();
                return;
            }
            if (access.faceAnimationClear(event)) {
                state.clear();
                return;
            }
            if (access.faceAnimationStop(event)) {
                stopFace(state, visualId.orElseThrow(), speakerId, gameTick);
                return;
            }
            access.faceAnimation(event).ifPresent(animation ->
                    playFace(state, visualId.orElseThrow(), speakerId, animation, gameTick));
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.debug("Failed to handle Echo Dialogue open event.", exception);
        }
    }

    private static void playFace(DialoguePortraitAnimationState state, String visualId, Optional<String> speakerId,
            String animation, long gameTick) {
        state.playPortrait(visualId, animation, gameTick);
        state.playSubject(visualId, animation, gameTick);
        speakerId.filter(id -> !id.equals(visualId)).ifPresent(id -> state.playSubject(id, animation, gameTick));
        if (isPlayerVisual(visualId) || speakerId.filter(JOHN_SPEAKER_ID::equals).isPresent()) {
            state.playPortrait(BASE_PLAYER_FACE_RIG, animation, gameTick);
            state.playSubject(BASE_PLAYER_FACE_RIG, animation, gameTick);
        }
        reportPlay(visualId, speakerId, animation);
    }

    private static void stopFace(DialoguePortraitAnimationState state, String visualId, Optional<String> speakerId,
            long gameTick) {
        state.stopPortrait(visualId, gameTick);
        state.stopSubject(visualId, gameTick);
        speakerId.filter(id -> !id.equals(visualId)).ifPresent(id -> state.stopSubject(id, gameTick));
        if (isPlayerVisual(visualId) || speakerId.filter(JOHN_SPEAKER_ID::equals).isPresent()) {
            state.stopPortrait(BASE_PLAYER_FACE_RIG, gameTick);
            state.stopSubject(BASE_PLAYER_FACE_RIG, gameTick);
        }
    }

    private static boolean isPlayerVisual(String visualId) {
        return visualId != null && (visualId.equals(BASE_PLAYER_FACE_RIG)
                || visualId.startsWith(ECHO_WARDROBE_JOHN_PREFIX));
    }

    private static void reportPlay(String visualId, Optional<String> speakerId, String animation) {
        String key = visualId + "|" + speakerId.orElse("") + "|" + animation;
        if (REPORTED_PLAY_KEYS.add(key)) {
            LOGGER.info("Echo Dialogue faceAnimation cue accepted: visual={} speaker={} animation={}",
                    visualId, speakerId.orElse("<none>"), animation);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void subscribe(Class<? extends Event> eventClass, Consumer<Event> consumer) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) eventClass, (Consumer) consumer);
    }

    private static long sampleTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

}
