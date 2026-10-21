package io.github.kxyr0.echoanimation.definition.resource;

import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable facial animation clip definition. */
public record FacialAnimationDefinition( FacialAnimationId id, Optional<FaceRigId> rigId, double durationSeconds,
        AnimationEndBehavior endBehavior, List<BoneTrack> tracks, List<Surface> surfaces, boolean suppressIdleBlink,
        String semanticFingerprint) {
    public static final String SURFACE_3D = "3d";
    public static final String SURFACE_PORTRAIT = "portrait";

    public FacialAnimationDefinition {
        id = Objects.requireNonNull(id, "id");
        rigId = Objects.requireNonNull(rigId, "rigId");
        endBehavior = Objects.requireNonNull(endBehavior, "endBehavior");
        tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
        surfaces = List.copyOf(Objects.requireNonNull(surfaces, "surfaces"));
        semanticFingerprint = Objects.requireNonNull(semanticFingerprint, "semanticFingerprint");
        if (!Double.isFinite(durationSeconds)
                || (tracks.isEmpty() ? durationSeconds < 0.0D : durationSeconds <= 0.0D)) {
            throw new IllegalArgumentException("durationSeconds должен быть положительным finite.");
        }
        if (tracks.isEmpty() && surfaces.isEmpty()) {
            throw new IllegalArgumentException("FacialAnimation must contain tracks or surfaces.");
        }
    }

    public Optional<Surface> surface(String surfaceName, FaceRigId rig) {
        String checkedSurface = Objects.requireNonNull(surfaceName, "surfaceName");
        FaceRigId checkedRig = Objects.requireNonNull(rig, "rig");
        Optional<Surface> exact = surfaces.stream() .filter(surface -> surface.name().equals(checkedSurface))
                        .filter(surface -> surface.rigId().filter(checkedRig::equals).isPresent()) .findFirst();
        return exact.or( () ->
                                surfaces.stream() .filter(surface -> surface.name().equals(checkedSurface))
                                        .filter(surface -> surface.rigId().isEmpty()) .findFirst())
                .or(() -> legacySurface(checkedSurface, checkedRig));
    }

    private Optional<Surface> legacySurface(String surfaceName, FaceRigId rig) {
        if (tracks.isEmpty() || rigId.filter(id -> !id.equals(rig)).isPresent()) {
            return Optional.empty();
        }
        return Optional.of( new Surface( surfaceName, rigId, Phase.none(),
                        new Phase(durationSeconds, endBehavior, tracks), Phase.none()));
    }

    public record Surface( String name, Optional<FaceRigId> rigId, Phase enter, Phase stay, Phase exit) {
        public Surface {
            name = Objects.requireNonNull(name, "name").trim();
            rigId = Objects.requireNonNull(rigId, "rigId");
            enter = Objects.requireNonNull(enter, "enter");
            stay = Objects.requireNonNull(stay, "stay");
            exit = Objects.requireNonNull(exit, "exit");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Surface name must not be blank.");
            }
            if (!enter.present() && !stay.present() && !exit.present()) {
                throw new IllegalArgumentException("Surface must contain enter, stay or exit.");
            }
        }
    }

    public record Phase( double durationSeconds, AnimationEndBehavior endBehavior, List<BoneTrack> tracks) {
        public static Phase none() {
            return new Phase(0.0D, AnimationEndBehavior.HOLD, List.of());
        }

        public Phase {
            endBehavior = Objects.requireNonNull(endBehavior, "endBehavior");
            tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
            if (!Double.isFinite(durationSeconds) || (!tracks.isEmpty() && durationSeconds <= 0.0D)
                    || (tracks.isEmpty() && durationSeconds < 0.0D)) {
                throw new IllegalArgumentException( "Phase durationSeconds must be finite and non-negative.");
            }
        }

        public boolean present() {
            return !tracks.isEmpty() && durationSeconds > 0.0D;
        }
    }
}
