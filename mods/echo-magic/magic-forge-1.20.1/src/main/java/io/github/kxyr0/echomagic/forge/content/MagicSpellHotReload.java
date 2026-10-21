package io.github.kxyr0.echomagic.forge.content;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class MagicSpellHotReload {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SCAN_INTERVAL_TICKS = 5L;
    private static final long DEBOUNCE_TICKS = 10L;
    private final Path root;
    private final ForgeSpellContentLoader loader;
    private final ForgeMagicServices services;
    private Map<String, byte[]> snapshot = Map.of();
    private long nextScanTick;
    private long reloadTick = -1L;
    private boolean initialized;
    private boolean scanFailureLogged;

    public MagicSpellHotReload(Path gameDir, ForgeSpellContentLoader loader, ForgeMagicServices services) {
        root = gameDir.resolve("story").resolve("content-pack");
        this.loader = loader;
        this.services = services;
    }

    public void serverStarted() {
        snapshot = Map.of();
        nextScanTick = 0L;
        reloadTick = -1L;
        initialized = false;
        scanFailureLogged = false;
    }

    public void tick(MinecraftServer server, long serverTick) {
        if (serverTick < nextScanTick) return;
        nextScanTick = serverTick + SCAN_INTERVAL_TICKS;
        Optional<Map<String, byte[]>> scanned = scan();
        if (scanned.isEmpty()) return;
        Map<String, byte[]> current = scanned.get();
        if (!initialized) {
            snapshot = current;
            initialized = true;
            return;
        }
        if (!sameSnapshot(snapshot, current)) {
            snapshot = current;
            reloadTick = serverTick + DEBOUNCE_TICKS;
        }
        if (reloadTick >= 0L && serverTick >= reloadTick) reload(server);
    }

    private Optional<Map<String, byte[]>> scan() {
        if (!Files.isDirectory(root)) return Optional.of(Map.of());
        try (var paths = Files.walk(root)) {
            Map<String, byte[]> result = new LinkedHashMap<>();
            List<Path> sources = paths.filter(Files::isRegularFile).filter(this::isSpellSource)
                    .sorted(Comparator.comparing(Path::toString)).toList();
            for (Path path : sources) {
                result.put(root.relativize(path).toString().replace('\\', '/'), digest(path));
            }
            scanFailureLogged = false;
            return Optional.of(Map.copyOf(result));
        } catch (IOException | UncheckedIOException exception) {
            if (!scanFailureLogged) LOGGER.warn("Cannot scan Echo Magic spell content for hot reload.", exception);
            scanFailureLogged = true;
            return Optional.empty();
        }
    }

    private boolean isSpellSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/echo_magic/spells/") && path.getFileName().toString().endsWith(".es");
    }

    private byte[] digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(Files.readAllBytes(path));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private boolean sameSnapshot(Map<String, byte[]> first, Map<String, byte[]> second) {
        if (!first.keySet().equals(second.keySet())) return false;
        return first.entrySet().stream().allMatch(entry -> MessageDigest.isEqual(entry.getValue(), second.get(entry.getKey())));
    }

    private void reload(MinecraftServer server) {
        reloadTick = -1L;
        long previousRevision = services.catalog().revision();
        services.accept(loader.reloadCurrent());
        boolean published = services.catalog().revision() > previousRevision;
        String key = published ? "message.echo_magic.hot_reload.success" : "message.echo_magic.hot_reload.failed";
        int count = services.catalog().definitions().size();
        if (published) LOGGER.info("Hot reloaded {} Echo Magic spell definitions.", count);
        else LOGGER.warn("Echo Magic hot reload was rejected; catalog revision {} remains active.",
                services.catalog().revision());
        server.getPlayerList().getPlayers().stream().filter(player -> player.hasPermissions(2))
                .forEach(player -> player.sendSystemMessage(Component.translatable(key, count)));
    }
}
