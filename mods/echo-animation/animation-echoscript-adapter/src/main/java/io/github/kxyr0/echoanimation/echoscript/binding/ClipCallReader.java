package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;
import io.github.kxyr0.echoanimation.core.pose.Keyframe;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.frame.FrameTransformReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import java.util.List;
import java.util.Optional;

/**
 * Reads FacialAnimation(...) semantics.
 */
final class ClipCallReader {
    private final ArgumentReader arguments = new ArgumentReader();
    private final FrameTransformReader frameTransforms = new FrameTransformReader();

    AnimationCompileResult read(List<ClassSyntax> classes, ClassSyntax resource, InvocationExpressionSyntax call,
            List<AnimationDiagnostic> diagnostics) {
        try {
            ClipHelperMethods helpers = new ClipHelperMethods(new MethodLookup(resource.members()));
            arguments.requireKnown(call, List.of("rig", "durationMillis", "durationTicks", "end", "tracks", "surfaces",
                    "suppressIdleBlink"), diagnostics);
            FacialAnimationId id = FacialAnimationId.parse(arguments.string(call, 0, null).orElseThrow());
            Optional<FaceRigId> rig = arguments.string(call, -1, "rig").map(FaceRigId::parse);
            boolean tickTime = arguments.has(call, "durationTicks");
            double duration = durationSeconds(call, tickTime);
            AnimationEndBehavior end = AnimationEndBehavior.valueOf(arguments.string(call, -1, "end").orElse("HOLD"));
            List<BoneTrack> tracks = arguments.calls(call, "tracks").stream()
                    .map(track -> track(helpers.track(track), tickTime, helpers))
                    .toList();
            List<FacialAnimationDefinition.Surface> surfaces = arguments.calls(call, "surfaces").stream()
                    .map(surface -> surface(helpers.surface(surface), rig, helpers))
                    .toList();
            surfaces = appendClassSurfaces(classes, surfaces, call, rig);
            boolean suppressIdleBlink = arguments.bool(call, "suppressIdleBlink", false);
            String fingerprint = new SemanticFingerprinter().fingerprint(List.of(id.toString(),
                    rig.toString(), Double.toString(duration), end.name(), tracks.toString(), surfaces.toString(),
                    Boolean.toString(suppressIdleBlink)));
            var animation = new FacialAnimationDefinition(id, rig, duration, end, tracks, surfaces, suppressIdleBlink,
                    fingerprint);
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.of(animation), diagnostics);
        } catch (RuntimeException exception) {
            diagnostics.add(AnimationSemanticDiagnostics.error("EANIM-ES-ANIMATION",
                    exception.getMessage(), call.span()));
            return new AnimationCompileResult(Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
        }
    }

    private double durationSeconds(InvocationExpressionSyntax call, boolean tickTime) {
        if (tickTime && arguments.has(call, "durationMillis")) {
            throw new IllegalArgumentException("Use either durationTicks or durationMillis, not both.");
        }
        int value = tickTime
                ? arguments.integer(call, -1, "durationTicks", 0)
                : arguments.integer(call, -1, "durationMillis", 0);
        return tickTime ? ticks(value) : value / 1000.0D;
    }

    private FacialAnimationDefinition.Surface surface(InvocationExpressionSyntax call, Optional<FaceRigId> rig,
            ClipHelperMethods helpers) {
        require(call, "Surface");
        String name = arguments.string(call, 0, null).orElseThrow();
        Optional<FaceRigId> surfaceRig = arguments.string(call, -1, "rig").map(FaceRigId::parse).or(() -> rig);
        return new FacialAnimationDefinition.Surface(name, surfaceRig,
                phase(call, "enter", "Enter", helpers),
                phase(call, "stay", "Stay", helpers),
                phase(call, "exit", "Exit", helpers));
    }

    private List<FacialAnimationDefinition.Surface> appendClassSurfaces(
            List<ClassSyntax> classes,
            List<FacialAnimationDefinition.Surface> surfaces,
            InvocationExpressionSyntax call,
            Optional<FaceRigId> rig
    ) {
        List<FacialAnimationDefinition.Surface> classSurfaces = arguments.expressions(call, "surfaces").stream()
                .filter(NameExpressionSyntax.class::isInstance)
                .map(NameExpressionSyntax.class::cast)
                .map(name -> classSurface(classes, name.name(), rig))
                .toList();
        if (classSurfaces.isEmpty()) {
            return surfaces;
        }
        return java.util.stream.Stream.concat(surfaces.stream(), classSurfaces.stream()).toList();
    }

    private FacialAnimationDefinition.Surface classSurface(List<ClassSyntax> classes, String className,
            Optional<FaceRigId> rig) {
        ClassSyntax surface = classes.stream().filter(clazz -> clazz.name().equals(className)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown surface class: " + className));
        ClipHelperMethods helpers = new ClipHelperMethods(new MethodLookup(surface.members()));
        Optional<FaceRigId> surfaceRig = helpers.rig().map(FaceRigId::parse).or(() -> rig);
        return new FacialAnimationDefinition.Surface(surfaceName(surface), surfaceRig,
                phase(helpers.phaseMethod("Enter"), helpers),
                phase(helpers.phaseMethod("Stay"), helpers),
                phase(helpers.phaseMethod("Exit"), helpers));
    }

    private String surfaceName(ClassSyntax surface) {
        return switch (surface.baseType().name().text()) {
            case "FacialAnimationSurface3d" -> FacialAnimationDefinition.SURFACE_3D;
            case "FacialAnimationSurfacePortrait" -> FacialAnimationDefinition.SURFACE_PORTRAIT;
            default -> throw new IllegalArgumentException("Unsupported surface class base type: "
                    + surface.baseType().name().text());
        };
    }

    private FacialAnimationDefinition.Phase phase(InvocationExpressionSyntax surface, String argumentName,
            String intrinsicName, ClipHelperMethods helpers) {
        Optional<InvocationExpressionSyntax> call = arguments.call(surface, argumentName)
                .or(() -> surface.arguments().stream()
                        .map(argument -> argument.value())
                        .filter(InvocationExpressionSyntax.class::isInstance)
                        .map(InvocationExpressionSyntax.class::cast)
                        .filter(invocation -> CallSyntax.named(invocation, intrinsicName))
                        .findFirst());
        return call.map(helpers::phase).map(value -> phase(value, helpers))
                .orElseGet(FacialAnimationDefinition.Phase::none);
    }

    private FacialAnimationDefinition.Phase phase(InvocationExpressionSyntax call, ClipHelperMethods helpers) {
        if (!CallSyntax.named(call, "Phase") && !CallSyntax.named(call, "Enter")
                && !CallSyntax.named(call, "Stay") && !CallSyntax.named(call, "Exit")) {
            throw new IllegalArgumentException("Ожидался intrinsic Phase/Enter/Stay/Exit.");
        }
        boolean tickTime = arguments.has(call, "durationTicks");
        double duration = durationSeconds(call, tickTime);
        AnimationEndBehavior end = AnimationEndBehavior.valueOf(arguments.string(call, -1, "end").orElse("HOLD"));
        List<BoneTrack> tracks = arguments.calls(call, "tracks").stream()
                .map(track -> track(helpers.track(track), tickTime, helpers))
                .toList();
        return new FacialAnimationDefinition.Phase(duration, end, tracks);
    }

    private BoneTrack track(InvocationExpressionSyntax call, boolean tickTime, ClipHelperMethods helpers) {
        require(call, "Track");
        BoneId id = BoneId.parse(arguments.string(call, 0, null).orElseThrow());
        List<Keyframe> frames = arguments.calls(call, "frames").stream()
                .map(frame -> frame(helpers.frame(frame), tickTime))
                .toList();
        return new BoneTrack(id, frames);
    }

    private Keyframe frame(InvocationExpressionSyntax call, boolean tickTime) {
        InvocationExpressionSyntax frame = frameTransforms.frame(call);
        require(frame, "Frame");
        double time = tickTime ? ticks(arguments.integer(frame, 0, null, 0))
                : arguments.integer(frame, 0, null, 0) / 1000.0D;
        InterpolationKind interpolation = InterpolationKind.valueOf(
                arguments.string(frame, -1, "interpolation").orElse("LINEAR"));
        return new Keyframe(time, frameTransforms.transform(call), interpolation);
    }

    private double ticks(int value) { return value / 20.0D; }

    private void require(InvocationExpressionSyntax call, String name) {
        if (!CallSyntax.named(call, name)) {
            throw new IllegalArgumentException("Ожидался intrinsic " + name + ".");
        }
    }

}
