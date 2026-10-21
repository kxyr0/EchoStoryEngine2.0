package io.github.kxyr0.echoanimation.echoscript.binding;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.echoscript.AnimationCompileResult;
import io.github.kxyr0.echoanimation.echoscript.body.BodyClipCallReader;
import io.github.kxyr0.echoanimation.echoscript.body.BodyRigCallReader;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationSemanticDiagnostics;
import io.github.kxyr0.echoanimation.echoscript.lifecycle.AnimationDefinitionCallReader;
import io.github.kxyr0.echoanimation.echoscript.lifecycle.AnimationProfileCallReader;
import io.github.kxyr0.echoanimation.echoscript.procedural.animation.ProceduralAnimationReader;
import io.github.kxyr0.echoanimation.echoscript.procedural.clip.ProceduralClipReader;
import io.github.kxyr0.echoanimation.echoscript.syntax.CallSyntax;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Owns Echo Animation resource syntax binding. */
public final class AnimationResourceBinder {
    private final RigCallReader rigReader = new RigCallReader();
    private final PoseCallReader poseReader = new PoseCallReader();
    private final ClipCallReader clipReader = new ClipCallReader();
    private final BodyRigCallReader bodyRigReader = new BodyRigCallReader();
    private final BodyClipCallReader bodyClipReader = new BodyClipCallReader();
    private final AnimationDefinitionCallReader animationReader = new AnimationDefinitionCallReader();
    private final AnimationProfileCallReader profileReader = new AnimationProfileCallReader();
    private final ProceduralClipReader proceduralClipReader = new ProceduralClipReader();
    private final ProceduralAnimationReader proceduralAnimationReader = new ProceduralAnimationReader();

    public AnimationCompileResult bind(CompilationUnitSyntax unit) {
        List<AnimationDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax resource = unit.classDeclaration();
        Optional<MethodSyntax> build = new MethodLookup(resource.members()).unique("Build");
        if (build.isEmpty()) {
            diagnostics.add( AnimationSemanticDiagnostics.error( "EANIM-ES-BUILD",
                            "Resource must contain exactly one Build method.", resource.span()));
            return empty(diagnostics);
        }
        List<StatementSyntax> statements = build.orElseThrow().body().statements();
        boolean legacy = statements.stream().anyMatch(ReturnStatementSyntax.class::isInstance);
        if (legacy && statements.stream() .anyMatch(statement -> !(statement instanceof ReturnStatementSyntax))) {
            diagnostics.add( AnimationSemanticDiagnostics.error( "EANIM-ES-MIXED-BUILD",
                            "Build cannot mix legacy return syntax with the procedural builder" + " API.",
                            build.orElseThrow().span()));
            return empty(diagnostics);
        }
        if (!legacy) {
            return bindProcedural( resource.baseType().name().text(), build.orElseThrow(), diagnostics);
        }
        Optional<InvocationExpressionSyntax> returned = buildReturn(build.orElseThrow());
        if (returned.isEmpty()) {
            diagnostics.add( AnimationSemanticDiagnostics.error( "EANIM-ES-BUILD",
                            "Legacy Build must return one intrinsic call.", build.orElseThrow().span()));
            return empty(diagnostics);
        }
        if (CallSyntax.named(returned.orElseThrow(), "Animation")
                || CallSyntax.named(returned.orElseThrow(), "AnimationClip")) {
            diagnostics.add( AnimationSemanticDiagnostics.warning( "EANIM-ES-LEGACY",
                            "return Animation(...)/AnimationClip(...) is deprecated; use procedural"
                                + " Build calls."));
        }
        return bindReturned( unit.classDeclarations(), resource, resource.baseType().name().text(),
                returned.orElseThrow(), diagnostics);
    }

    private Optional<InvocationExpressionSyntax> buildReturn(MethodSyntax build) {
        return build.body().statements().stream() .filter(ReturnStatementSyntax.class::isInstance)
                .map(ReturnStatementSyntax.class::cast) .flatMap(statement -> statement.value().stream())
                .filter(InvocationExpressionSyntax.class::isInstance) .map(InvocationExpressionSyntax.class::cast)
                .findFirst();
    }

    private AnimationCompileResult bindProcedural(
            String baseType, MethodSyntax build, List<AnimationDiagnostic> diagnostics) {
        if (baseType.equals("AnimationClipResource")) {
            return proceduralClipReader.read(build, diagnostics);
        }
        if (baseType.equals("AnimationResource")) {
            return proceduralAnimationReader.read(build, diagnostics);
        }
        diagnostics.add( AnimationSemanticDiagnostics.error( "EANIM-ES-BUILDER-TYPE",
                        "Procedural Build is supported for AnimationClipResource and" + " AnimationResource.",
                        build.span()));
        return empty(diagnostics);
    }

    private AnimationCompileResult bindReturned( List<ClassSyntax> classes, ClassSyntax resource, String baseType,
            InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        Optional<AnimationCompileResult> face = bindFace(classes, resource, baseType, call, diagnostics);
        if (face.isPresent()) {
            return face.orElseThrow();
        }
        Optional<AnimationCompileResult> body = bindBody(baseType, call, diagnostics);
        if (body.isPresent()) {
            return body.orElseThrow();
        }
        if (baseType.equals("AnimationResource") && CallSyntax.named(call, "Animation")) {
            return animationReader.read(call, diagnostics);
        }
        if (baseType.equals("AnimationProfileResource") && CallSyntax.named(call, "AnimationProfile")) {
            return profileReader.read(call, diagnostics);
        }
        diagnostics.add( AnimationSemanticDiagnostics.error( "EANIM-ES-RESOURCE",
                        "Build returns an intrinsic that does not match the resource base type.", call.span()));
        return empty(diagnostics);
    }

    private Optional<AnimationCompileResult> bindFace( List<ClassSyntax> classes, ClassSyntax resource, String baseType,
            InvocationExpressionSyntax call, List<AnimationDiagnostic> diagnostics) {
        if (baseType.equals("FaceRigResource") && CallSyntax.named(call, "FaceRig")) {
            return Optional.of(rigReader.read(call, diagnostics));
        }
        if (baseType.equals("FacePoseResource") && CallSyntax.named(call, "FacePose")) {
            return Optional.of(poseReader.read(call, diagnostics));
        }
        if (baseType.equals("FacialAnimationResource") && CallSyntax.named(call, "FacialAnimation")) {
            return Optional.of(clipReader.read(classes, resource, call, diagnostics));
        }
        return Optional.empty();
    }

    private Optional<AnimationCompileResult> bindBody( String baseType, InvocationExpressionSyntax call,
            List<AnimationDiagnostic> diagnostics) {
        if (baseType.equals("AnimationRigResource") && CallSyntax.named(call, "AnimationRig")) {
            return Optional.of(bodyRigReader.read(call, diagnostics));
        }
        if (baseType.equals("AnimationClipResource") && CallSyntax.named(call, "AnimationClip")) {
            return Optional.of(bodyClipReader.read(call, diagnostics));
        }
        return Optional.empty();
    }

    private AnimationCompileResult empty(List<AnimationDiagnostic> diagnostics) {
        return new AnimationCompileResult( Optional.empty(), Optional.empty(), Optional.empty(), diagnostics);
    }
}
