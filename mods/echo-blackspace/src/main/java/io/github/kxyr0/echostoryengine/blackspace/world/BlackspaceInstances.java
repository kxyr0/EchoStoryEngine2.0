package io.github.kxyr0.echostoryengine.blackspace.world;

import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BlackspaceInstances {
    private final BlackspaceAllocator allocator = new BlackspaceAllocator();
    private final BlackspaceBuilder builder = new BlackspaceBuilder();
    private final Map<String, BlackspaceInstance> instances = new ConcurrentHashMap<>();

    public BlackspaceInstance getOrCreate(ServerPlayer owner, String id) {
        Objects.requireNonNull(owner, "owner");
        return getOrCreate((ServerLevel) owner.level(), id);
    }

    public BlackspaceInstance getOrCreate(ServerLevel level, String id) {
        Objects.requireNonNull(level, "level");
        String checkedId = requireId(id);
        String key = key(level, checkedId);
        BlackspaceInstance existing = instances.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = instances.get(key);
            if (existing != null) {
                return existing;
            }
            BlackspaceInstance created = allocateUnique(level, checkedId);
            builder.build(level, created);
            instances.put(key, created);
            return created;
        }
    }

    public Optional<BlackspaceInstance> find(ServerPlayer player, String id) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(instances.get(key((ServerLevel) player.level(), requireId(id))));
    }

    public Optional<BlackspaceInstance> containing(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return instances.values().stream()
                .filter(instance -> instance.contains(player))
                .findFirst();
    }

    public Collection<BlackspaceInstance> all() {
        return List.copyOf(instances.values());
    }

    public boolean isPlatformBlock(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return instances.values().stream()
                .filter(instance -> instance.dimension().equals(level.dimension()))
                .anyMatch(instance -> instance.isPlatformBlock(pos));
    }

    public void clear() {
        instances.clear();
    }

    private BlackspaceInstance allocateUnique(ServerLevel level, String id) {
        return allocator.allocate(level, id, 0);
    }

    private String key(ServerLevel level, String id) {
        return level.dimension().location() + "|" + id;
    }

    private String requireId(String id) {
        String checked = Objects.requireNonNull(id, "id").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("Blackspace id must not be blank.");
        }
        return checked;
    }
}
