package io.github.kxyr0.echovisibility.forge.client.runtime;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.api.state.VisibilitySampleResult;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import io.github.kxyr0.echovisibility.core.observation.VisibilityObservationClassifier;
import io.github.kxyr0.echovisibility.core.state.VisibilityTransitionResult;
import io.github.kxyr0.echovisibility.forge.client.camera.ActiveCameraSnapshotCapture;
import io.github.kxyr0.echovisibility.forge.client.concealment.ClientConcealmentState;
import io.github.kxyr0.echovisibility.forge.client.occlusion.ForgeVisibilityOcclusionProbe;
import io.github.kxyr0.echovisibility.forge.network.EchoVisibilityNetwork;
import java.util.Optional;
import net.minecraft.client.Minecraft;

final class VisibilityClientEvaluator {
    private static final VisibilitySampleResult UNAVAILABLE_SAMPLES =
            new VisibilitySampleResult(0, 0, 0, 0, 0, "target-unavailable");
    private final VisibilityObservationClassifier classifier = new VisibilityObservationClassifier();
    private final ClientTargetBounds targetBounds = new ClientTargetBounds();
    private final ActiveCameraSnapshotCapture cameraCapture;
    private final ClientConcealmentState concealment;
    private final VisibilityPerformanceCounters performance;

    VisibilityClientEvaluator(
            ActiveCameraSnapshotCapture cameraCapture,
            ClientConcealmentState concealment,
            VisibilityPerformanceCounters performance) {
        this.cameraCapture = cameraCapture;
        this.concealment = concealment;
        this.performance = performance;
    }

    void evaluate(
            ClientArmedWatch watch,
            VisibilityCameraSnapshot activeCamera,
            float partialTick,
            long renderedFrame,
            long clientTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        VisibilityCameraSnapshot camera = cameraCapture.viewpoint(
                activeCamera, watch.definition().viewpoint(), partialTick);
        long started = System.nanoTime();
        var resolved = targetBounds.resolve(watch, minecraft, partialTick)
                .filter(bounds -> bounds.dimension().equals(dimension(minecraft)));
        if (resolved.isEmpty()) {
            unavailable(watch, camera, renderedFrame, clientTick);
            performance.evaluation(System.nanoTime() - started);
            return;
        }
        var bounds = resolved.orElseThrow();
        var observation = classifier.evaluate(watch.definition(), camera, bounds.bounds(),
                new ForgeVisibilityOcclusionProbe(minecraft.level));
        var transition = watch.transitionState().observe(observation.state(), renderedFrame);
        performance.evaluation(System.nanoTime() - started);
        performance.rays(observation.sampleResult().viewportSamples());
        if (transition.reportable()) {
            long sequence = watch.nextReportSequence();
            Optional<VisibilityConcealmentLeaseId> lease = concealIfNeeded(
                    watch, transition, sequence, clientTick);
            watch.debug(Optional.of(bounds), camera, observation, transition, concealment.status(watch));
            send(new VisibilityReport(watch.definition().id(), watch.generation(), bounds.revision(),
                    watch.connectionGeneration(), sequence, renderedFrame, clientTick,
                    transition.stableState(), transition.transition(), camera,
                    observation.sampleResult(), lease));
        } else {
            watch.debug(Optional.of(bounds), camera, observation, transition, concealment.status(watch));
        }
    }

    private void unavailable(
            ClientArmedWatch watch,
            VisibilityCameraSnapshot camera,
            long renderedFrame,
            long clientTick) {
        VisibilityTransitionResult transition = watch.transitionState().observe(
                VisibilityState.TARGET_UNAVAILABLE, renderedFrame);
        watch.unavailable(camera, transition);
        if (transition.reportable()) {
            send(new VisibilityReport(watch.definition().id(), watch.generation(),
                    watch.definition().revision(), watch.connectionGeneration(),
                    watch.nextReportSequence(), renderedFrame, clientTick,
                    transition.stableState(), transition.transition(), camera,
                    UNAVAILABLE_SAMPLES, Optional.empty()));
        }
    }

    private Optional<VisibilityConcealmentLeaseId> concealIfNeeded(
            ClientArmedWatch watch,
            VisibilityTransitionResult transition,
            long sequence,
            long clientTick) {
        if (transition.transition().kind() != VisibilityTransitionKind.LOST_SIGHT
                || watch.definition().concealmentPolicy()
                != VisibilityConcealmentPolicy.HIDE_NPC_IMMEDIATELY) {
            return Optional.empty();
        }
        return concealment.conceal(watch, sequence, clientTick);
    }

    private void send(VisibilityReport report) {
        EchoVisibilityNetwork.sendReport(report);
        performance.report();
    }

    private static VisibilityDimensionId dimension(Minecraft minecraft) {
        return new VisibilityDimensionId(minecraft.level.dimension().location().toString());
    }
}
