package io.github.kxyr0.echoplayer.core.persistence;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.identity.PlayerIdentitySnapshot;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileLifecycleMetadata;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileRevision;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.spark.PlayerSparkSnapshot;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Flat durable DTO for profile persistence.
 */
public record PlayerProfileData(
        String playerId,
        String accountName,
        String nameMode,
        String customName,
        int sparkCurrent,
        int sparkMaximum,
        String skinMode,
        String skinId,
        String skinModel,
        String skinFingerprint,
        String templateId,
        String templateFingerprint,
        long profileRevision,
        long repositoryRevision,
        long appearanceRevision,
        String createdAt,
        String lastLoginAt,
        String lastLogoutAt,
        String firstAccountName,
        String lastAccountName,
        boolean defaultTemplateApplied
) {
    public PlayerProfileData {
        playerId = Objects.requireNonNull(playerId, "playerId");
        accountName = Objects.requireNonNull(accountName, "accountName");
        nameMode = Objects.requireNonNull(nameMode, "nameMode");
        customName = Objects.requireNonNull(customName, "customName");
        skinMode = Objects.requireNonNull(skinMode, "skinMode");
        skinId = Objects.requireNonNull(skinId, "skinId");
        skinModel = Objects.requireNonNull(skinModel, "skinModel");
        skinFingerprint = Objects.requireNonNull(skinFingerprint, "skinFingerprint");
        templateId = Objects.requireNonNull(templateId, "templateId");
        templateFingerprint = Objects.requireNonNull(templateFingerprint, "templateFingerprint");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        lastLoginAt = Objects.requireNonNull(lastLoginAt, "lastLoginAt");
        lastLogoutAt = Objects.requireNonNull(lastLogoutAt, "lastLogoutAt");
        firstAccountName = Objects.requireNonNull(firstAccountName, "firstAccountName");
        lastAccountName = Objects.requireNonNull(lastAccountName, "lastAccountName");
    }

    public static PlayerProfileData fromSnapshot(PlayerProfileSnapshot snapshot) {
        return new PlayerProfileData(snapshot.playerId().value(), snapshot.identity().accountName(),
                snapshot.identity().nameMode().name(),
                snapshot.identity().customDisplayName().map(PlayerDisplayName::value).orElse(""),
                snapshot.spark().current(), snapshot.spark().maximum(), snapshot.appearance().skinMode().name(),
                snapshot.appearance().skinId().map(PlayerSkinId::value).orElse(""),
                snapshot.appearance().skinModel().map(PlayerSkinModel::name).orElse(""),
                snapshot.appearance().sourceFingerprint(), snapshot.templateId().value(),
                snapshot.templateFingerprint(), snapshot.profileRevision().value(),
                snapshot.repositoryRevision(), snapshot.appearance().appearanceRevision(),
                snapshot.lifecycle().createdAt().toString(), snapshot.lifecycle().lastLoginAt().toString(),
                snapshot.lifecycle().lastLogoutAt().map(Instant::toString).orElse(""),
                snapshot.lifecycle().firstAccountName(), snapshot.lifecycle().lastAccountName(),
                snapshot.lifecycle().defaultTemplateApplied());
    }

    public PlayerProfileSnapshot toSnapshot() {
        EchoPlayerId id = EchoPlayerId.parse(playerId);
        PlayerIdentitySnapshot identity = identity(id);
        PlayerAppearanceSnapshot appearance = appearance();
        PlayerProfileLifecycleMetadata lifecycle = new PlayerProfileLifecycleMetadata(Instant.parse(createdAt),
                Instant.parse(lastLoginAt), lastLogoutAt.isBlank() ? Optional.empty()
                : Optional.of(Instant.parse(lastLogoutAt)), firstAccountName, lastAccountName,
                defaultTemplateApplied);
        return new PlayerProfileSnapshot(id, identity, new PlayerSparkSnapshot(sparkCurrent, sparkMaximum),
                appearance, PlayerProfileTemplateId.parse(templateId), templateFingerprint,
                lifecycle, new PlayerProfileRevision(profileRevision), repositoryRevision);
    }

    private PlayerIdentitySnapshot identity(EchoPlayerId id) {
        PlayerIdentitySnapshot account = PlayerIdentitySnapshot.account(id, accountName);
        return PlayerNameMode.valueOf(nameMode) == PlayerNameMode.CUSTOM
                ? account.withCustomName(PlayerDisplayName.of(customName))
                : account;
    }

    private PlayerAppearanceSnapshot appearance() {
        PlayerSkinMode mode = PlayerSkinMode.valueOf(skinMode);
        if (mode == PlayerSkinMode.ACCOUNT) {
            return PlayerBaseSkin.snapshot(appearanceRevision);
        }
        return PlayerBaseSkin.snapshot(appearanceRevision);
    }
}
