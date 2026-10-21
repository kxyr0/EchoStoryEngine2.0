package io.github.kxyr0.echonavigation.vanilla.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.junit.jupiter.api.Test;

final class VanillaBlockGridFollowerTest {
    @Test
    void findsNextDownStepInLookahead() {
        Path path = new Path(List.of(
                new Node(0, 65, 0),
                new Node(1, 65, 0),
                new Node(2, 64, 0),
                new Node(3, 64, 0)), new BlockPos(3, 64, 0), true);
        path.setNextNodeIndex(1);

        var step = VanillaBlockGridFollower.nextDownStep(path);

        assertTrue(step.isPresent());
        assertEquals(new BlockPos(1, 65, 0), step.orElseThrow().from());
        assertEquals(new BlockPos(2, 64, 0), step.orElseThrow().to());
    }

    @Test
    void ignoresFlatRoute() {
        Path path = new Path(List.of(new Node(0, 65, 0), new Node(1, 65, 0)),
                new BlockPos(1, 65, 0), true);

        assertTrue(VanillaBlockGridFollower.nextDownStep(path).isEmpty());
    }
}
