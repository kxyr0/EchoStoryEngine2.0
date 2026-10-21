package io.github.kxyr0.echoconcentration.forge.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;

final class ConcentrationPresentationConflicts {
    private final List<Optional<Method>> activeChecks = List.of(
            check("io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient"));

    boolean blocksEntry(Minecraft minecraft) {
        return minecraft.screen != null || requiresExit(minecraft);
    }

    boolean requiresExit(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.gameRenderer.getMainCamera().getEntity() != minecraft.player) {
            return true;
        }
        return activeChecks.stream().flatMap(Optional::stream).anyMatch(this::active);
    }

    private Optional<Method> check(String className) {
        try { return Optional.of(Class.forName(className).getMethod("hudSuppressed")); }
        catch (ClassNotFoundException | NoSuchMethodException exception) { return Optional.empty(); }
    }

    private boolean active(Method method) {
        try { return Boolean.TRUE.equals(method.invoke(null)); }
        catch (IllegalAccessException | InvocationTargetException exception) { return true; }
    }
}
