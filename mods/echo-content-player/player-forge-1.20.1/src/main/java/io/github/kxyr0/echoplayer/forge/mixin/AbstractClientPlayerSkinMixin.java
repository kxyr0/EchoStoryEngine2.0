package io.github.kxyr0.echoplayer.forge.mixin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceExtensionRegistryBridge;
import io.github.kxyr0.echoplayer.forge.api.appearance.resolved.ResolvedPlayerAppearance;
import io.github.kxyr0.echoplayer.forge.client.PlayerClientMirror;
import io.github.kxyr0.echoplayer.forge.sync.PlayerProfileSyncPacket;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces the authored Echo base skin over Mojang account skins.
 */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerSkinMixin {
    private static final ResourceLocation BASE_SKIN = ResourceLocation.fromNamespaceAndPath(
            PlayerBaseSkin.ID.namespace(), PlayerBaseSkin.ID.resourcePath());

    @Inject(method = {"getSkinTextureLocation", "m_108560_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void echoPlayer$skin(CallbackInfoReturnable<ResourceLocation> callback) {
        if ((Object) this instanceof LocalPlayer player) {
            ResourceLocation texture = resolvedAppearance(player)
                    .map(ResolvedPlayerAppearance::worldSkin)
                    .map(AbstractClientPlayerSkinMixin::skinTexture)
                    .orElse(BASE_SKIN);
            callback.setReturnValue(texture);
        }
    }

    @Inject(method = {"getModelName", "m_108564_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void echoPlayer$model(CallbackInfoReturnable<String> callback) {
        if ((Object) this instanceof LocalPlayer player) {
            String model = resolvedAppearance(player)
                    .map(ResolvedPlayerAppearance::skinModel)
                    .map(AbstractClientPlayerSkinMixin::modelName)
                    .orElse(modelName(PlayerBaseSkin.MODEL));
            callback.setReturnValue(model);
        }
    }

    private static Optional<ResolvedPlayerAppearance> resolvedAppearance(LocalPlayer player) {
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(player.getUUID());
        PlayerAppearanceSnapshot baseAppearance = baseAppearance(playerId);
        return PlayerAppearanceExtensionRegistryBridge.lookup()
                .flatMap(registry -> registry.resolve(playerId, baseAppearance));
    }

    private static PlayerAppearanceSnapshot baseAppearance(EchoPlayerId playerId) {
        return PlayerClientMirror.INSTANCE.packet(playerId)
                .map(AbstractClientPlayerSkinMixin::baseAppearance)
                .orElseGet(() -> PlayerBaseSkin.snapshot(0L));
    }

    private static PlayerAppearanceSnapshot baseAppearance(PlayerProfileSyncPacket packet) {
        if (packet.skinMode() != PlayerSkinMode.RESOURCE || packet.skinId().isBlank()
                || packet.skinModel().isBlank()) {
            return PlayerBaseSkin.snapshot(packet.appearanceRevision());
        }
        try {
            return PlayerAppearanceSnapshot.resource(PlayerSkinId.parse(packet.skinId()),
                    PlayerSkinModel.valueOf(packet.skinModel()), "", packet.appearanceRevision());
        } catch (IllegalArgumentException exception) {
            return PlayerBaseSkin.snapshot(packet.appearanceRevision());
        }
    }

    private static ResourceLocation skinTexture(String skinId) {
        if (PlayerBaseSkin.ID.toString().equals(skinId)) {
            return BASE_SKIN;
        }
        ResourceLocation id = parse(skinId);
        String path = id.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return id;
        }
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + path + ".png");
    }

    private static ResourceLocation parse(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            return BASE_SKIN;
        }
        return ResourceLocation.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }

    private static String modelName(PlayerSkinModel model) {
        return model == PlayerSkinModel.SLIM ? "slim" : "default";
    }
}
