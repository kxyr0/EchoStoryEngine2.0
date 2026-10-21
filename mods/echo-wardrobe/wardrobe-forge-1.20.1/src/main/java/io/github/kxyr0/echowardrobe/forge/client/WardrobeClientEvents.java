package io.github.kxyr0.echowardrobe.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.forge.ForgeWardrobeRuntimeState;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeCatalogLoader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Client-only wardrobe overlays for external dialogue portrait renderers.
 */
public final class WardrobeClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ECHO_DIALOGUE_MOD_ID = "echo_dialogue";
    private static final String VISUAL_EVENT_CLASS =
            "io.github.kxyr0.echo" + "dialogue.forge.api.Echo" + "DialogueSpeakerVisualRenderEvent";
    private static final String SKIN_HEAD_KIND = "SKIN_HEAD";
    private static final String PLAYER_BASE_VISUAL = "echo_content_player:player/base";
    private static final String PLAYER_WARDROBE_SKIN_PREFIX = "jdh:echo_wardrobe/skins/";

    private WardrobeClientEvents() {
    }

    public static void register(ForgeWardrobeRuntimeState runtimeState, ForgeWardrobeCatalogLoader loader,
            Path gameDir) {
        Objects.requireNonNull(runtimeState, "runtimeState");
        WardrobeClientHotReload.register(runtimeState, loader, gameDir);
        if (!net.minecraftforge.fml.ModList.get().isLoaded(ECHO_DIALOGUE_MOD_ID)) {
            return;
        }
        try {
            Class<? extends Event> eventClass = Class.forName(VISUAL_EVENT_CLASS).asSubclass(Event.class);
            VisualEventAccess access = VisualEventAccess.create(eventClass);
            subscribe(eventClass, event -> render(runtimeState, access, event));
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.debug("Echo Wardrobe dialogue portrait overlay bridge is unavailable.", exception);
        }
    }

    private static void render(ForgeWardrobeRuntimeState runtimeState, VisualEventAccess access, Event event) {
        try {
            if (!SKIN_HEAD_KIND.equals(access.kind(event))) {
                return;
            }
            if (!isPlayerVisual(access.visualId(event))) {
                return;
            }
            GuiGraphics graphics = access.graphics(event);
            Bounds bounds = access.bounds(event);
            activeOverlay(runtimeState).ifPresent(overlay -> overlay.draw(graphics,
                    bounds.x(), bounds.y(), bounds.width(), bounds.height()));
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.debug("Failed to render Echo Wardrobe dialogue portrait overlay.", exception);
        }
    }

    private static boolean isPlayerVisual(String visualId) {
        return PLAYER_BASE_VISUAL.equals(visualId) || visualId.startsWith(PLAYER_WARDROBE_SKIN_PREFIX);
    }

    private static Optional<WardrobePortraitOverlay> activeOverlay(ForgeWardrobeRuntimeState runtimeState) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        return itemId(player.getItemBySlot(EquipmentSlot.HEAD))
                .flatMap(id -> runtimeState.catalog().wearables().activeByItemId(id.toString()))
                .flatMap(WearableDefinition::portraitOverlay)
                .flatMap(WardrobePortraitOverlay::from);
    }

    private static Optional<ResourceLocation> itemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void subscribe(Class<? extends Event> eventClass, Consumer<Event> consumer) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) eventClass, (Consumer) consumer);
    }

    private static Object invoke(Method method, Object target) throws ReflectiveOperationException {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException exception) {
            throw new ReflectiveOperationException(exception.getCause());
        }
    }

    private record VisualEventAccess(
            Method graphics,
            Method kind,
            Method visualId,
            Method x,
            Method y,
            Method width,
            Method height
    ) {
        static VisualEventAccess create(Class<?> type) throws NoSuchMethodException {
            return new VisualEventAccess(type.getMethod("graphics"), type.getMethod("kind"),
                    type.getMethod("visualId"),
                    type.getMethod("x"), type.getMethod("y"), type.getMethod("width"), type.getMethod("height"));
        }

        GuiGraphics graphics(Event event) throws ReflectiveOperationException {
            return (GuiGraphics) invoke(graphics, event);
        }

        String kind(Event event) throws ReflectiveOperationException {
            return invoke(kind, event).toString();
        }

        String visualId(Event event) throws ReflectiveOperationException {
            return (String) invoke(visualId, event);
        }

        Bounds bounds(Event event) throws ReflectiveOperationException {
            return new Bounds(value(x, event), value(y, event), value(width, event), value(height, event));
        }

        private int value(Method method, Event event) throws ReflectiveOperationException {
            return ((Integer) invoke(method, event)).intValue();
        }
    }

    private record Bounds(int x, int y, int width, int height) {
    }
}
