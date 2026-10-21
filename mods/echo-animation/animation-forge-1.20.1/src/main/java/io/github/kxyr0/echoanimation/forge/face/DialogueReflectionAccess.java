package io.github.kxyr0.echoanimation.forge.face;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

final class DialogueReflectionAccess {
    private final Class<? extends Event> waitEventClass;
    private final EventMethods events;
    private final WaitMethods waits;
    private final SessionMethods sessions;
    private final ServiceMethods services;

    private DialogueReflectionAccess(
            Class<? extends Event> waitEventClass,
            EventMethods events,
            WaitMethods waits,
            SessionMethods sessions,
            ServiceMethods services
    ) {
        this.waitEventClass = waitEventClass;
        this.events = events;
        this.waits = waits;
        this.sessions = sessions;
        this.services = services;
    }

    static DialogueReflectionAccess create() throws ReflectiveOperationException {
        String base = "io.github.kxyr0.echo" + "dialogue.";
        Class<? extends Event> eventClass = Class.forName(base + "forge.api.wait.DialogueExternalWaitDispatchEvent")
                .asSubclass(Event.class);
        Class<?> sessionClass = Class.forName(base + "api.session.DialogueSessionView");
        Class<?> waitClass = Class.forName(base + "api.wait.DialogueExternalWait");
        Class<?> sessionIdClass = Class.forName(base + "api.identity.DialogueSessionId");
        Class<?> requestClass = Class.forName(base + "api.wait.DialogueExternalResumeRequest");
        Class<?> serviceClass = Class.forName(base + "api.service.DialogueService");
        Class<?> lookupClass = Class.forName(base + "forge.api.Echo" + "DialogueServiceLookupEvent");
        Constructor<?> resumeRequest = requestClass.getConstructor(sessionIdClass, int.class, String.class);
        return new DialogueReflectionAccess(eventClass, EventMethods.create(eventClass), WaitMethods.create(waitClass),
                SessionMethods.create(sessionClass), ServiceMethods.create(lookupClass, serviceClass, resumeRequest));
    }

    Class<? extends Event> waitEventClass() {
        return waitEventClass;
    }

    Optional<ServerPlayer> player(Event event) throws ReflectiveOperationException {
        Optional<?> value = (Optional<?>) invoke(events.player(), event);
        return value.map(ServerPlayer.class::cast);
    }

    Object session(Event event) throws ReflectiveOperationException {
        return invoke(events.session(), event);
    }

    Wait wait(Event event) throws ReflectiveOperationException {
        Object value = invoke(events.externalWait(), event);
        return new Wait((String) invoke(waits.type(), value), (String) invoke(waits.key(), value),
                (String) invoke(waits.payload(), value));
    }

    void resume(MinecraftServer server, Object session, Wait wait) throws ReflectiveOperationException {
        if (server == null) {
            return;
        }
        Optional<Object> service = service(server);
        if (service.isEmpty()) {
            return;
        }
        Object request = services.resumeRequest().newInstance(
                invoke(sessions.sessionId(), session), (Integer) invoke(sessions.revision(), session), wait.key());
        invoke(services.resumeExternal(), service.orElseThrow(), request);
    }

    private Optional<Object> service(MinecraftServer server) throws ReflectiveOperationException {
        Object event = services.lookup().newInstance(server);
        MinecraftForge.EVENT_BUS.post((Event) event);
        List<?> providers = (List<?>) invoke(services.providers(), event);
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    private static Object invoke(
            Method method,
            Object target,
            Object... arguments
    ) throws ReflectiveOperationException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw new ReflectiveOperationException(exception.getCause());
        }
    }

    record Wait(String type, String key, String payload) {
    }

    private record EventMethods(Method player, Method session, Method externalWait) {
        static EventMethods create(Class<?> type) throws NoSuchMethodException {
            return new EventMethods(type.getMethod("player"), type.getMethod("session"),
                    type.getMethod("externalWait"));
        }
    }

    private record WaitMethods(Method type, Method key, Method payload) {
        static WaitMethods create(Class<?> type) throws NoSuchMethodException {
            return new WaitMethods(type.getMethod("waitType"), type.getMethod("waitKey"),
                    type.getMethod("payload"));
        }
    }

    private record SessionMethods(Method sessionId, Method revision) {
        static SessionMethods create(Class<?> type) throws NoSuchMethodException {
            return new SessionMethods(type.getMethod("sessionId"), type.getMethod("revision"));
        }
    }

    private record ServiceMethods(
            Constructor<?> lookup,
            Method providers,
            Constructor<?> resumeRequest,
            Method resumeExternal
    ) {
        static ServiceMethods create(
                Class<?> lookupType,
                Class<?> serviceType,
                Constructor<?> resumeRequest
        ) throws NoSuchMethodException {
            Method resumeExternal = serviceType.getMethod("resumeExternal", resumeRequest.getDeclaringClass());
            return new ServiceMethods(lookupType.getConstructor(MinecraftServer.class),
                    lookupType.getMethod("providers"), resumeRequest, resumeExternal);
        }
    }
}
