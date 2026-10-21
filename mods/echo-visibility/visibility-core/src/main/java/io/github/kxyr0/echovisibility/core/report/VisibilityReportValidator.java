package io.github.kxyr0.echovisibility.core.report;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.core.arm.ArmedVisibilityWatch;
import java.util.Optional;

public final class VisibilityReportValidator {
    public VisibilityReportValidation validate(ArmedVisibilityWatch armed, VisibilityReport report) {
        Optional<VisibilityReportValidation> identity = validateIdentity(armed, report);
        if (identity.isPresent()) {
            return identity.orElseThrow();
        }
        Optional<VisibilityReportValidation> sequence = validateSequence(armed, report);
        if (sequence.isPresent()) {
            return sequence.orElseThrow();
        }
        if (!validCamera(report.camera())) {
            return rejected(VisibilityReportValidation.Status.CAMERA_INVALID,
                    VisibilityDiagnosticCode.VISIBILITY_CAMERA_SNAPSHOT_INVALID,
                    "Camera basis, FOV, aspect ratio, or framebuffer is invalid.");
        }
        if (!validTransition(armed, report)) {
            return rejected(VisibilityReportValidation.Status.TRANSITION_INVALID,
                    VisibilityDiagnosticCode.VISIBILITY_REPORT_REJECTED,
                    "Report transition does not describe the reported state.");
        }
        if (!validConcealment(armed, report)) {
            return rejected(VisibilityReportValidation.Status.CONCEALMENT_INVALID,
                    VisibilityDiagnosticCode.VISIBILITY_CONCEALMENT_REJECTED,
                    "Concealment lease is not allowed for this transition and target.");
        }
        return VisibilityReportValidation.accepted();
    }

    private static Optional<VisibilityReportValidation> validateIdentity(
            ArmedVisibilityWatch armed,
            VisibilityReport report) {
        if (!armed.generation().equals(report.generation())) {
            return Optional.of(rejected(VisibilityReportValidation.Status.GENERATION_MISMATCH,
                    VisibilityDiagnosticCode.VISIBILITY_REPORT_GENERATION_MISMATCH,
                    "Watch generation does not match the active arm."));
        }
        if (!armed.definition().revision().equals(report.targetRevision())) {
            return Optional.of(rejected(VisibilityReportValidation.Status.REVISION_MISMATCH,
                    VisibilityDiagnosticCode.VISIBILITY_TARGET_REVISION_MISMATCH,
                    "Target definition revision does not match the active arm."));
        }
        if (armed.connectionGeneration() != report.connectionGeneration()) {
            return Optional.of(rejected(VisibilityReportValidation.Status.CONNECTION_MISMATCH,
                    VisibilityDiagnosticCode.VISIBILITY_REPORT_REJECTED,
                    "Observation connection generation is stale."));
        }
        return Optional.empty();
    }

    private static Optional<VisibilityReportValidation> validateSequence(
            ArmedVisibilityWatch armed,
            VisibilityReport report) {
        if (report.reportSequence() <= armed.lastAcceptedSequence()
                || report.clientTick() < armed.lastAcceptedClientTick()) {
            return Optional.of(rejected(VisibilityReportValidation.Status.SEQUENCE_INVALID,
                    VisibilityDiagnosticCode.VISIBILITY_REPORT_SEQUENCE_INVALID,
                    "Report sequence or client tick did not advance."));
        }
        return Optional.empty();
    }

    private static boolean validTransition(ArmedVisibilityWatch armed, VisibilityReport report) {
        return report.transition().current() == report.state()
                && report.transition().renderedFrame() == report.renderedFrame()
                && report.transition().previous() == armed.snapshot().lastState()
                && (report.transition().kind() != VisibilityTransitionKind.LOST_SIGHT
                        || armed.snapshot().lastState().visible());
    }

    private static boolean validCamera(VisibilityCameraSnapshot camera) {
        double fov = camera.dynamicFovDegrees();
        double aspect = camera.aspectRatio();
        double forwardUp = Math.abs(camera.forward().dot(camera.up()));
        double forwardRight = Math.abs(camera.forward().dot(camera.right()));
        double upRight = Math.abs(camera.up().dot(camera.right()));
        return fov > 1.0D && fov < 179.0D && aspect > 0.1D && aspect < 10.0D
                && camera.framebufferWidth() <= 32768 && camera.framebufferHeight() <= 32768
                && forwardUp < 0.05D && forwardRight < 0.05D && upRight < 0.05D;
    }

    private static boolean validConcealment(ArmedVisibilityWatch armed, VisibilityReport report) {
        if (report.concealmentLeaseId().isEmpty()) {
            return true;
        }
        return armed.definition().target() instanceof VisibilityNpcTarget
                && armed.definition().concealmentPolicy() == VisibilityConcealmentPolicy.HIDE_NPC_IMMEDIATELY
                && report.transition().kind() == VisibilityTransitionKind.LOST_SIGHT
                && report.state().geometricallyHidden();
    }

    private static VisibilityReportValidation rejected(
            VisibilityReportValidation.Status status,
            VisibilityDiagnosticCode code,
            String message) {
        return new VisibilityReportValidation(status,
                Optional.of(VisibilityDiagnostic.of(code, message)));
    }
}
