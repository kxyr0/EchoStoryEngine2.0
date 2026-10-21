package io.github.kxyr0.echowardrobe.playeradapter;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceContribution;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.PlayerAppearanceContributionId;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.PlayerAppearanceRevision;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.WearableModelDescriptor;
import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.forge.api.EchoWardrobeServiceBridge;
import java.util.List;
import java.util.Optional;

/**
 * Converts Wardrobe exact outfit contributions to Content Player neutral appearance.
 */
public final class WardrobePlayerAppearanceContribution implements PlayerAppearanceContribution {
    private static final PlayerAppearanceContributionId ID =
            new PlayerAppearanceContributionId("echo_wardrobe:equipped_outfit");

    @Override
    public PlayerAppearanceContributionId id() {
        return ID;
    }

    @Override
    public Optional<ResolvedPlayerAppearance> resolve(EchoPlayerId playerId, PlayerAppearanceSnapshot baseAppearance) {
        return EchoWardrobeServiceBridge.lookup()
                .flatMap(service -> service.wardrobeAppearance(playerId.accountUuid()))
                .map(contribution -> resolved(playerId, baseAppearance, contribution));
    }

    private ResolvedPlayerAppearance resolved(
            EchoPlayerId playerId,
            PlayerAppearanceSnapshot base,
            io.github.kxyr0.echowardrobe.api.contribution.WardrobeAppearanceContribution contribution
    ) {
        long baseRevision = base.appearanceRevision();
        return new ResolvedPlayerAppearance(playerId, contribution.worldSkin(), contribution.faceSkin(), skinModel(base),
                contribution.outfitVariantId().map(Object::toString), models(contribution.modelContributions()),
                new PlayerAppearanceRevision(baseRevision),
                new PlayerAppearanceRevision(contribution.appearanceRevision()),
                new PlayerAppearanceRevision(baseRevision + contribution.appearanceRevision()));
    }

    private PlayerSkinModel skinModel(PlayerAppearanceSnapshot base) {
        return base.skinModel().orElse(PlayerBaseSkin.MODEL);
    }

    private List<WearableModelDescriptor> models(List<WearableModelContribution> contributions) {
        return contributions.stream()
                .map(model -> new WearableModelDescriptor(model.modelResourceId(), model.modelResourceId(),
                        model.transform().x(), model.transform().y(), model.transform().z(), model.transform().pitch(),
                        model.transform().yaw(), model.transform().roll(), model.transform().scaleX(),
                        model.transform().scaleY(), model.transform().scaleZ()))
                .toList();
    }
}
