package io.github.kxyr0.echostoryengine.story.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeDeterminismTest {
    @Test
    void sameInitialStateAndOrderedInputsProduceTheSameTransitions() {
        List<StoryRuntimeResult> firstRun = runHappyPath();
        List<StoryRuntimeResult> secondRun = runHappyPath();

        assertEquals(firstRun, secondRun);
    }

    @Test
    void transitionBudgetStopsInvalidSynchronousLoop() {
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCaseWithBudget(RuntimeTestFixture.loopingStory(), 3);

        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.startInput());

        assertEquals(StoryExecutionPhase.FAILED, result.state().phase());
        assertEquals("ESE-RUNTIME-002", result.state().failureCode());
        assertEquals("ESE-RUNTIME-002", result.diagnostics().get(0).code());
    }

    @Test
    void storyCoreTestsRunWithoutMinecraftOrForgeClasses() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("net.minecraft.world.entity.Entity"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("net.minecraftforge.fml.common.Mod"));
    }

    private static List<StoryRuntimeResult> runHappyPath() {
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(
                new RuntimeTestFixture.RecordingRepository(),
                new RuntimeTestFixture.RecordingSink());
        return List.of(
                useCase.advance(RuntimeTestFixture.startInput()),
                useCase.advance(RuntimeTestFixture.successfulCompletion()));
    }
}
