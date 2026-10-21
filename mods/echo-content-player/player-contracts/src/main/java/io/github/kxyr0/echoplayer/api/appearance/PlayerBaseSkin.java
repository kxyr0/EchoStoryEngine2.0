package io.github.kxyr0.echoplayer.api.appearance;

/**
 * Canonical authored skin forced for every Echo player profile.
 */
public final class PlayerBaseSkin {
    public static final PlayerSkinId ID = PlayerSkinId.parse("echo_content_player:player/base");
    public static final PlayerSkinModel MODEL = PlayerSkinModel.CLASSIC;
    public static final String FINGERPRINT = "builtin:echo_content_player:player/base";

    private PlayerBaseSkin() {
    }

    public static PlayerAppearanceSnapshot snapshot(long revision) {
        return PlayerAppearanceSnapshot.resource(ID, MODEL, FINGERPRINT, revision);
    }

    public static boolean isBase(PlayerAppearanceSnapshot appearance) {
        return appearance.skinId().filter(ID::equals).isPresent()
                && appearance.skinModel().filter(MODEL::equals).isPresent();
    }

    public static PlayerAppearanceSnapshot normalize(PlayerAppearanceSnapshot appearance) {
        return isBase(appearance) ? appearance : snapshot(appearance.appearanceRevision());
    }
}
