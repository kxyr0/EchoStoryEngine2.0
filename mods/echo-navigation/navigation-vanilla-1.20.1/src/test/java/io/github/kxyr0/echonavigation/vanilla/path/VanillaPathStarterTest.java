package io.github.kxyr0.echonavigation.vanilla.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.junit.jupiter.api.Test;

final class VanillaPathStarterTest {
    @Test
    void asksVanillaForExactThenRelaxedPaths() {
        assertEquals(0, VanillaPathStarter.EXACT_TARGET_ACCURACY);
        assertEquals(1, VanillaPathStarter.RELAXED_TARGET_ACCURACY);
    }

    @Test
    void rejectsPathEndingAboveTarget() {
        BlockPos target = new BlockPos(10, 64, 10);
        Path path = new Path(List.of(new Node(10, 65, 10)), target, true);

        assertFalse(VanillaPathStarter.reachesTarget(path, target));
    }

    @Test
    void acceptsPathEndingAtTargetFeetBlock() {
        BlockPos target = new BlockPos(10, 64, 10);
        Path path = new Path(List.of(new Node(9, 64, 10), new Node(10, 64, 10)), target, true);

        assertTrue(VanillaPathStarter.reachesTarget(path, target));
    }

    @Test
    void rejectsUnreachedTargetEvenWhenEndNodeMatches() {
        BlockPos target = new BlockPos(10, 64, 10);
        Path path = new Path(List.of(new Node(10, 64, 10)), target, false);

        assertFalse(VanillaPathStarter.reachesTarget(path, target));
    }
}
