# Face Layer Audit

Scope: A5 face-layer evidence for pupil and sclera behavior.

This document intentionally does not edit or prescribe coordinates for an
authored `.face-rig.es` file. Existing authored rigs remain the source of
truth for placement, pivots, texture regions, and draw order.

## Capability Matrix

| Level | Check | Result | Proof | Gap |
| --- | --- | --- | --- | --- |
| EchoScript parser | Parses valid face rig bones, layers, poses, controls, and facial animation tracks; rejects invalid canonical pupil/sclera parents and missing authored pupil/sclera layer pairs on the compile path. | PASS | `EchoScriptAnimationCompiler`, `EchoScriptAnimationCompilerTest.scleraParentedPupilFaceRigCompiles`, `EchoScriptFaceParentCompilerTest`, `EchoScriptFaceLayerPairCompilerTest`, `ForgeAnimationGameTests.faceRigResourceCompilesInForgeRuntime`, `facePoseResourceCompilesInForgeRuntime`, `facialAnimationResourceCompilesInForgeRuntime` | Does not prove in-game render output. |
| Rig binder | Creates immutable face rig hierarchy, layers, controls, and fingerprints. | PASS | `FaceRigDefinition`, `AnimationResourceCandidateCompiler`, `DefinitionRequirementMatrixTest.layerModelsAreImmutable` | Current proof uses compiled examples, not every content rig. |
| Clip binder | Accepts face bone tracks for facial animations. | PASS | `FacialAnimationDefinition`, `FacialAnimationValidator`, `DefinitionRequirementMatrixTest.unknownBoneTrackRejected`, `duplicateTrackRejected` | Does not prove visual eyelid/brow motion in Minecraft. |
| Validator | Checks layer bone references, duplicate layer ids, clip rect bounds, draw-order conflicts, canonical face parents, and known pupil/sclera layer pairs. | PASS | `FaceRigValidator`, `DefinitionRequirementMatrixTest`, `CanonicalFaceParentValidatorTest`, `FaceLayerPairValidatorTest` | Optional richer semantic coverage is still limited to known canonical and authored pair ids. |
| Catalog | Stores face rigs, poses, and facial animations in the last known-good catalog. | PASS | `AnimationResourceSet`, `AnimationCatalogState.CatalogAnimationService.faceRigs`, `facePoses`, `facialAnimations`, `ForgeAnimationGameTests.invalidCandidateRetainsCatalog` | Does not prove all external content files are authored correctly. |
| Hot reload | Recompiles changed animation resources and retains last valid catalog on invalid reload. | PARTIAL | `AnimationResourceCandidateCompiler.cached`, `retain`, `AnimationCatalogState.install`, `invalidCandidateRetainsCatalog` | No dedicated proof that a changed face clip hot-reloads while preserving a live face playback cursor. |
| Playback | Generic bone tracks can sample face bones because face bones use the same `BonePose`/track model. | PARTIAL | `FacialAnimationValidator`, `TransformEvaluator`, `DefinitionRequirementMatrixTest.poseApplicationIsDeterministic` | No end-to-end active facial animation playback proof in Minecraft yet. |
| Channels | Face data is modeled separately enough to avoid changing body hierarchy through poses. | PARTIAL | `FacePoseDefinition`, `DefinitionRequirementMatrixTest.poseCannotChangeParentBecauseOnlyTargetBoneDeltasAreStored` | No broad proof yet for simultaneous body channel plus face channel playback. |
| Sync | Client render service can derive a face pose for a subject and project layers. | PARTIAL | `AnimationClientFaceRenderService.facePose`, `FaceLayerPoseProjectorTest` | No reconnect/live network proof for current face phase. |
| Persistence | A3 playback persistence exists for whole animations. | PARTIAL | A3 playback host and query tests cover playback state, not facial animation-specific recovery. | Missing restart acceptance for face animation phases. |
| Renderer | Face layers are projected through evaluated bones and sent to the face quad renderer. | PASS | `AnimationClientFaceRenderService`, `FaceLayerPoseProjector`, `FaceLayerPoseProjectorTest` | Manual screenshot acceptance still required for actual texture appearance. |
| Draw order | Draw order is deterministic and same-order overlaps are rejected. | PASS | `LayerDrawPlanner`, `LayerDrawPlannerTest`, `LayerDrawPlannerRequirementMatrixTest`, `FaceRigValidator.validateDrawOrderConflicts` | None for unit scope. |
| Optional glasses | Hidden/missing glasses layers are not synthesized; authored layers can be hidden. | PASS | `LayerDrawPlannerTest.hiddenLayerIsNotPlanned`, `missingGlassesLayerDoesNotCreateGlassesCommand`, `LayerDrawPlannerRequirementMatrixTest.hiddenGlassesForInspectionAreNotPlanned` | No manual acceptance for a character with authored glasses after this audit. |
| Documentation | Working proof paths and known gaps are documented. | PASS | This file, `FACE_LAYERS.md`, `FACE_RIGS.md`, `FACE_CONTROLS.md` | Broader A5 interactive docs are still incomplete. |

## Pupil/Sclera Invariant

| Requirement | Current support | Proof |
| --- | --- | --- |
| Canonical pupils must be authored as sclera children | `FaceRigValidator` and `EchoScriptAnimationCompiler` reject `left_pupil` or `right_pupil` when the matching sclera bone is missing or is not the direct parent. | `animation-definition/src/main/java/.../FaceRigValidator.java`, `CanonicalFaceParentValidatorTest`, `EchoScriptFaceParentCompilerTest.canonicalPupilWithoutMatchingScleraIsRejectedByEchoScriptCompiler`, `canonicalPupilSiblingOfScleraIsRejectedByEchoScriptCompiler` |
| Canonical pupils inherit sclera transforms | `FaceLayerPoseProjector` evaluates the bone hierarchy before layer projection. | `FaceLayerPoseProjectorTest.pupilLayerInheritsScleraParentTransform`, `rightPupilLayerInheritsScleraParentTransform` |
| Canonical pupils cannot draw outside parent sclera surface | Pupil layers are clamped against the non-pupil layer whose id matches the parent sclera bone, then fall back to another parent surface only if no named sclera layer exists. Oversized pupils are clipped to the surface. | `FaceLayerPoseProjectorTest.pupilLayerIsClippedByParentScleraSurface`, `rightPupilLayerIsClippedByParentScleraSurface`, `pupilLayerPrefersNamedParentScleraSurface`, `pupilLayerFullyOutsideParentScleraSurfaceIsClamped`, `rightPupilLayerFullyOutsideParentScleraSurfaceIsClamped` |
| Unsafe pupil/sclera transforms do not bypass clipping | If a PUPIL layer or its resolved sclera surface is not axis-aligned, the projector omits the pupil instead of rendering it outside an unverified clip area. | `FaceLayerPoseProjector`, `FaceLayerUnsafeTransformProjectorTest.rotatedPupilIsNotProjectedBecauseItCannotBeSafelyClipped`, `pupilWithRotatedScleraSurfaceIsNotProjected` |
| Existing rigs with `pupil_left`/`sclera_left` layer IDs do not need migration | `ScleraLayerPairing` resolves `pupil_left -> sclera_left`, `pupil_right -> sclera_right`, `left_pupil -> left_sclera`, and `right_pupil -> right_sclera` without mutating the rig. Pupil draw commands are clamped or clipped when they leave the paired sclera surface. | `ScleraLayerPairing`, `FaceLayerPoseProjectorTest.pupilLayerIsClippedByPairedScleraLayerWhenBothUseFaceRoot`, `FaceLayerAuthoredPairingTest.pupilLeftLayerFullyOutsidePairedScleraIsClamped`, `pupilRightLayerFullyOutsidePairedScleraIsClamped` |
| Known authored pupil layers require matching sclera layers | `FaceRigValidator` rejects `pupil_left`, `pupil_right`, `left_pupil`, or `right_pupil` PUPIL layers when the paired sclera layer is missing or is also a PUPIL layer. | `FaceLayerPairValidatorTest`, `EchoScriptFaceLayerPairCompilerTest` |
| Authored `face.rig` coordinates are not changed by this behavior | Clipping happens at render projection time and produces a derived `LayerDrawCommand`. | `FaceLayerPoseProjector`, no `.face-rig.es` edits in `echo-animation` |

## Status

The renderer and validator checks above pass in unit tests and in the full
`./gradlew.bat check --console=plain` build.

This is not a replacement for manual Minecraft acceptance. In-game acceptance
still needs to verify the actual character face under current resource-pack
textures, lighting, animation playback, and hot reload.
