package io.github.kxyr0.echonavigation.vanilla.path;

import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public final class VanillaPathProgress {
    public Snapshot snapshot(NavigationAgentHandle handle, NavigationDestination destination) {
        PathNavigation navigation = handle.mob().getNavigation();
        Path path = navigation.getPath();
        int index = path == null ? 0 : Math.max(0, path.getNextNodeIndex());
        return new Snapshot(handle.position(), destination.standingPosition().horizontalDistanceTo(handle.position()),
                index, nodes(path));
    }

    private List<Vector3> nodes(Path path) {
        if (path == null) {
            return List.of();
        }
        List<Vector3> values = new ArrayList<>();
        for (int index = 0; index < path.getNodeCount(); index++) {
            Node node = path.getNode(index);
            values.add(new Vector3(node.x + 0.5D, node.y, node.z + 0.5D));
        }
        return List.copyOf(values);
    }

    public record Snapshot(Vector3 current, double remainingDistance, int pathIndex, List<Vector3> nodes) {
    }
}
