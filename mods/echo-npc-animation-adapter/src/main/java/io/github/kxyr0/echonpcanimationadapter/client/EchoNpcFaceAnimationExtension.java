package io.github.kxyr0.echonpcanimationadapter.client;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationFaceRenderResult;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderResult;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcHeadRenderContext;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bridges NPC rendering into Echo Animation.
 */
public final class EchoNpcFaceAnimationExtension
        implements NpcClientRenderExtension {

    private final AnimationSubjectMapping subjectMapping = new AnimationSubjectMapping();

    @Override
    public NpcClientRenderResult render( NpcHeadRenderContext context) {

        if (!context.visible()) {
            return NpcClientRenderResult.NOT_HANDLED;
        }

        return context.replacementPass() ? renderHumanoid(context) : renderFace(context);
    }

    private NpcClientRenderResult renderFace( NpcHeadRenderContext context) {

        List<FaceRigId> candidates = rigCandidates( context.definitionId(), context.skinId());

        if (candidates.isEmpty()) {
            return NpcClientRenderResult.NOT_HANDLED;
        }

        AnimationClientServiceBridge.Result lookup = AnimationClientServiceBridge.lookup();

        if (lookup.status()
                != AnimationClientServiceBridge.Status.RESOLVED) {

            return NpcClientRenderResult.NOT_HANDLED;
        }

        AnimationFaceRenderResult result = lookup.service() .orElseThrow() .renderFace( new AnimationFaceRenderContext(
                                        subjectMapping.subjectId( context.actorId()), candidates, context.poses(),
                                        context.buffers(), context.packedLight(), context.overlay(),
                                        context.partialTick(), context.head()));

        return switch (result) {
            case RENDERED ->
                    NpcClientRenderResult.RENDERED_OVERLAY;

            case FAILED ->
                    NpcClientRenderResult.FAILED;

            case NO_RIG, SERVICE_UNAVAILABLE ->
                    NpcClientRenderResult.NOT_HANDLED;
        };
    }

    private NpcClientRenderResult renderHumanoid( NpcHeadRenderContext context) {

        List<AnimationRigId> candidates = bodyRigCandidates( context.definitionId(), context.skinId());

        if (candidates.isEmpty()) {
            return NpcClientRenderResult.NOT_HANDLED;
        }

        List<FaceRigId> faceCandidates = rigCandidates( context.definitionId(), context.skinId());

        AnimationClientServiceBridge.Result lookup = AnimationClientServiceBridge.lookup();

        if (lookup.status()
                != AnimationClientServiceBridge.Status.RESOLVED) {

            return NpcClientRenderResult.NOT_HANDLED;
        }

        AnimationHumanoidRenderResult result = lookup.service() .orElseThrow() .renderHumanoid(
                                new AnimationHumanoidRenderContext( subjectMapping.subjectId( context.actorId()),
                                        candidates, faceCandidates, context.skinTexture(), skinVariant(
                                                context.skinModelVariant()), context.poses(), context.buffers(),
                                        context.packedLight(), context.overlay(), context.partialTick(),
                                        context.sitting(), bones ->
                                                context .replacementAttachments() .render(
                                                                new NpcAnimationAttachmentBridge( bones))));

        return switch (result) {
            case REPLACED_DEFAULT_MODEL ->
                    NpcClientRenderResult.REPLACED_DEFAULT_MODEL;

            case RENDERED_OVERLAY ->
                    NpcClientRenderResult.RENDERED_OVERLAY;

            case FAILED ->
                    NpcClientRenderResult.FAILED;

            case NOT_HANDLED ->
                    NpcClientRenderResult.NOT_HANDLED;
        };
    }


    private List<FaceRigId> rigCandidates( String definitionId, String skinId) {

        Set<FaceRigId> result = new LinkedHashSet<>();

        addCandidate( result, definitionId);

        addCandidate( result, skinId);

        return List.copyOf( new ArrayList<>(result));
    }

    private List<AnimationRigId> bodyRigCandidates( String definitionId, String skinId) {

        Set<AnimationRigId> result = new LinkedHashSet<>();

        addBodyCandidate( result, definitionId);

        addBodyCandidate( result, skinId);

        addNamespaceHumanoid( result, definitionId);

        addNamespaceHumanoid( result, skinId);

        return List.copyOf( new ArrayList<>(result));
    }

    private void addCandidate( Set<FaceRigId> result, String value) {

        if (value == null || value.isBlank()) {

            return;
        }

        try {
            result.add( FaceRigId.parse(value));
        } catch (IllegalArgumentException exception) {
            // Invalid NPC metadata is ignored.
        }
    }

    private void addBodyCandidate( Set<AnimationRigId> result, String value) {

        if (value == null || value.isBlank()) {

            return;
        }

        try {
            result.add( AnimationRigId.parse(value));
        } catch (IllegalArgumentException exception) {
            // Invalid NPC metadata is ignored.
        }
    }

    private void addNamespaceHumanoid( Set<AnimationRigId> result, String value) {

        if (value == null) {
            return;
        }

        int separator = value.indexOf(':');

        if (separator <= 0) {
            return;
        }

        addBodyCandidate( result, value.substring( 0, separator) + ":humanoid");
    }

    private AnimationSkinVariant skinVariant( String value) {

        try {
            return AnimationSkinVariant.valueOf( value == null ? "CLASSIC" : value);
        } catch (IllegalArgumentException exception) {
            return AnimationSkinVariant.CLASSIC;
        }
    }
}
