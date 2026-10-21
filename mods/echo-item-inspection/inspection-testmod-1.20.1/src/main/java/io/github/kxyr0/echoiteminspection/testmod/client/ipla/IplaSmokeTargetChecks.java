package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.akicater.blocks.LayingItemEntity;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

final class IplaSmokeTargetChecks {
    private final IplaSmokeEvidence evidence;
    private final IplaSmokeFixture fixture;
    private int phase;

    IplaSmokeTargetChecks(IplaSmokeEvidence evidence, IplaSmokeFixture fixture) {
        this.evidence = evidence;
        this.fixture = fixture;
    }

    boolean tick(Minecraft minecraft) {
        switch (phase) {
            case 0 -> fixture.mutate(level -> level.setBlockAndUpdate(IplaSmokeFixture.WALL, Blocks.STONE.defaultBlockState()));
            case 1 -> {
                if (!minecraft.level.getBlockState(IplaSmokeFixture.WALL).is(Blocks.STONE)) return false;
                evidence.check("solidWallOccludesPrompt", WorldInspectionRuntime.prompt().activeTarget() == null);
                evidence.capture("wall-occlusion");
                fixture.mutate(level -> level.setBlockAndUpdate(IplaSmokeFixture.WALL, Blocks.AIR.defaultBlockState()));
                fixture.lookAt(IplaSmokeFixture.OTHER);
            }
            case 2 -> {
                evidence.check("nonInspectablePlacedCobblestoneHasNoPrompt", WorldInspectionRuntime.prompt().activeTarget() == null);
                fixture.lookAway();
            }
            case 3 -> {
                evidence.check("lookAwayFadesOut", WorldInspectionRuntime.prompt().alpha(net.minecraft.Util.getMillis()) < 0.01);
                fixture.far(true);
                fixture.lookAt(IplaSmokeFixture.ITEM);
            }
            case 4 -> {
                evidence.check("outOfRangeHasNoPrompt", WorldInspectionRuntime.prompt().activeTarget() == null);
                fixture.far(false);
            }
            case 5 -> {
                evidence.check("insideRangeTargetReturns", WorldInspectionRuntime.prompt().activeTarget() != null);
                var entity = (LayingItemEntity) minecraft.level.getBlockEntity(IplaSmokeFixture.ITEM);
                var stack = entity.inv.get(4).copy();
                entity.inv.set(4, ItemStack.EMPTY); // Simulate a synchronized removal arriving between render and input.
                IplaSmokeInputChecks.press(GLFW.GLFW_KEY_E);
                evidence.check("staleSynchronizedStackRejectedBeforeOpen", !(minecraft.screen instanceof ItemInspectionScreen));
                entity.inv.set(4, stack);
                KeyMapping.releaseAll();
            }
            case 6 -> {
                WorldInspectionRuntime.refresh();
                IplaSmokeInputChecks.press(GLFW.GLFW_KEY_E);
                evidence.check("viewerBeforeExternalRemoval", minecraft.screen instanceof ItemInspectionScreen);
                fixture.mutate(level -> level.removeBlock(IplaSmokeFixture.ITEM, false));
            }
            case 7 -> {
                if (minecraft.level.getBlockEntity(IplaSmokeFixture.ITEM) != null) return false;
                evidence.check("snapshotSurvivesPlacedObjectRemoval", minecraft.screen instanceof ItemInspectionScreen
                        && ((ItemInspectionScreen) minecraft.screen).session().snapshot().copyStack().hasFoil());
                ((ItemInspectionScreen) minecraft.screen).onClose();
                evidence.check("removedObjectCloseReturnsToGameplay", minecraft.screen == null);
            }
            case 8 -> {
                evidence.check("removedObjectNotResurrected", minecraft.level.getBlockEntity(IplaSmokeFixture.ITEM) == null);
                fixture.restore();
            }
            case 9 -> {
                if (!fixture.ready(minecraft) || WorldInspectionRuntime.prompt().activeTarget() == null) return false;
                return true;
            }
            default -> throw new AssertionError("Unknown target phase " + phase);
        }
        phase++;
        return false;
    }
}
