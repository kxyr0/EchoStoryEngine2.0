package io.github.kxyr0.echoplayersleep.script;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

final class SleepBlackspaceBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SCENE_CLASS =
            "io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceScene";
    private static final String INSTANCE_CLASS =
            "io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance";

    private SleepBlackspaceBridge() {
    }

    static Optional<Target> find(PlayerRef player, String instanceId) {
        Objects.requireNonNull(player, "player");
        String checkedId = Objects.requireNonNull(instanceId, "instanceId").trim();
        if (checkedId.isEmpty()) {
            return Optional.empty();
        }
        try {
            Optional<ServerPlayer> serverPlayer = player(player);
            if (serverPlayer.isEmpty()) {
                return Optional.empty();
            }
            Classes classes = classes();
            Method find = classes.scene().getMethod("find", ServerPlayer.class, String.class);
            Object result = find.invoke(null, serverPlayer.orElseThrow(), checkedId);
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }
            Method enter = classes.scene().getMethod("enter",
                    ServerPlayer.class, classes.instance(), Runnable.class);
            return Optional.of(new Target(serverPlayer.orElseThrow(), optional.orElseThrow(), enter));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            throw bridgeFailure("find Blackspace instance " + checkedId, exception);
        }
    }

    static boolean isActive(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        try {
            Optional<ServerPlayer> serverPlayer = player(player);
            if (serverPlayer.isEmpty()) {
                return false;
            }
            Method isActive = classes().scene().getMethod("isActive", ServerPlayer.class);
            Object result = isActive.invoke(null, serverPlayer.orElseThrow());
            return result instanceof Boolean active && active;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Echo Player Sleep could not query Blackspace activity.", exception);
            return false;
        }
    }

    static boolean exit(PlayerRef player, Runnable completion) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(completion, "completion");
        try {
            Optional<ServerPlayer> serverPlayer = player(player);
            if (serverPlayer.isEmpty()) {
                return false;
            }
            Method exit = classes().scene().getMethod("exit", ServerPlayer.class, Runnable.class);
            Object result = exit.invoke(null, serverPlayer.orElseThrow(), completion);
            return !(result instanceof Boolean active) || active;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException exception) {
            throw bridgeFailure("exit Blackspace", exception);
        }
    }

    private static Optional<ServerPlayer> player(PlayerRef player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Optional.empty()
                : Optional.ofNullable(server.getPlayerList().getPlayer(player.uuid()));
    }

    private static Classes classes() throws ClassNotFoundException {
        ClassLoader loader = SleepBlackspaceBridge.class.getClassLoader();
        return new Classes(Class.forName(SCENE_CLASS, false, loader),
                Class.forName(INSTANCE_CLASS, false, loader));
    }

    private static IllegalStateException bridgeFailure(String operation, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation
                ? invocation.getCause()
                : exception;
        return new IllegalStateException("Echo Player Sleep failed to " + operation + ".", cause);
    }

    record Target(ServerPlayer player, Object instance, Method enter) {
        void enter(Runnable completion) {
            Objects.requireNonNull(completion, "completion");
            try {
                enter.invoke(null, player, instance, completion);
            } catch (ReflectiveOperationException exception) {
                throw bridgeFailure("enter Blackspace", exception);
            }
        }
    }

    private record Classes(Class<?> scene, Class<?> instance) {
    }
}
