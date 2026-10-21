package io.github.kxyr0.echoplayer.forge.dialogue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.core.profile.PlayerProfileCreator;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceContribution;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.PlayerAppearanceContributionId;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.PlayerAppearanceRevision;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSpeakerPresentationSessionTest {
    private static final UUID ACCOUNT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void resolvesJdhJohnToAudienceProfileSkinHead() {
        PlayerProfileSnapshot profile = profile();
        PlayerSpeakerPresentationSession session = new PlayerSpeakerPresentationSession(new SingleProfile(profile),
                new PlayerAppearanceExtensions());

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john", ACCOUNT_UUID.toString()));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        assertEquals("Лололошка", resolution.presentation().orElseThrow().displayName());
        assertEquals(SpeakerVisualRef.skinHead(PlayerBaseSkin.ID.toString()),
                resolution.presentation().orElseThrow().visual().orElseThrow());
    }

    @Test
    void wardrobeAppearanceReplacesDialogueVisualWithFaceSkin() {
        PlayerProfileSnapshot profile = profile();
        PlayerAppearanceExtensions extensions = new PlayerAppearanceExtensions();
        extensions.register(new FixedAppearanceContribution(
                "jdh:echo_wardrobe/skins/origin/john_outfit/john_outfit_1"));
        PlayerSpeakerPresentationSession session = new PlayerSpeakerPresentationSession(new SingleProfile(profile),
                extensions);

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john", ACCOUNT_UUID.toString()));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        assertEquals(SpeakerVisualRef.skinHead("jdh:echo_wardrobe/skins/origin/john_outfit/john_outfit_1"),
                resolution.presentation().orElseThrow().visual().orElseThrow());
    }

    @Test
    void ignoresNpcSpeakersAndNonUuidAudiences() {
        PlayerSpeakerPresentationSession session = new PlayerSpeakerPresentationSession(new SingleProfile(profile()),
                new PlayerAppearanceExtensions());

        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED,
                session.resolve(request("jdh:father", ACCOUNT_UUID.toString())).kind());
        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED,
                session.resolve(request("jdh:john", "audience")).kind());
    }

    private static SpeakerPresentationRequest request(String speakerId, String audienceId) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("session-1"),
                DialogueId.parse("jdh:home"),
                SpeakerId.parse(speakerId),
                new AudienceId(audienceId),
                SpeakerPresentationRequest.Kind.LINE,
                "Джон",
                Optional.empty());
    }

    private static PlayerProfileSnapshot profile() {
        return new PlayerProfileCreator().create(
                ACCOUNT_UUID, "AccountJohn", PlayerProfileTemplate.fallback(), Instant.EPOCH);
    }

    private record SingleProfile(PlayerProfileSnapshot profile) implements PlayerProfileQueryService {
        @Override
        public Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId) {
            return profile.playerId().equals(playerId) ? Optional.of(profile) : Optional.empty();
        }

        @Override
        public List<PlayerProfileSnapshot> findAll() {
            return List.of(profile);
        }
    }

    private record FixedAppearanceContribution(String faceSkin) implements PlayerAppearanceContribution {
        @Override
        public PlayerAppearanceContributionId id() {
            return new PlayerAppearanceContributionId("test:fixed");
        }

        @Override
        public Optional<ResolvedPlayerAppearance> resolve(EchoPlayerId playerId,
                io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot baseAppearance) {
            return Optional.of(new ResolvedPlayerAppearance(playerId, faceSkin, faceSkin, PlayerBaseSkin.MODEL,
                    Optional.of(id().value()), List.of(), new PlayerAppearanceRevision(1L),
                    new PlayerAppearanceRevision(2L), new PlayerAppearanceRevision(3L)));
        }
    }
}
