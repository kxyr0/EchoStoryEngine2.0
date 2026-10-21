package io.github.kxyr0.echoplayer.core.skin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Strict 64x64 PNG validator for authored resource skins.
 */
public final class PlayerSkinCandidateValidator {
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    public PlayerProfileResult<SkinCandidate> validate(Path root, Path candidate, PlayerSkinId id,
                                                       PlayerSkinModel model) {
        try {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedCandidate = candidate.toAbsolutePath().normalize();
            requireInsideRoot(normalizedRoot, normalizedCandidate);
            byte[] bytes = Files.readAllBytes(normalizedCandidate);
            requirePngSignature(bytes);
            requireDimensions(normalizedCandidate);
            return PlayerProfileResult.valid(new SkinCandidate(id, model, normalizedCandidate, fingerprint(bytes)),
                    List.of());
        } catch (RuntimeException | IOException ex) {
            return PlayerProfileResult.invalid(List.of(PlayerProfileDiagnostic.error("PLAYER_SKIN_INVALID",
                    ex.getMessage(), candidate.toString())));
        }
    }

    public PlayerSkinResolutionPlan plan(PlayerProfileResult<SkinCandidate> result,
                                         java.util.Optional<PlayerAppearanceSnapshot> previous) {
        if (result.successful()) {
            SkinCandidate candidate = result.value().orElseThrow();
            return PlayerSkinResolutionPlan.accepted(PlayerAppearanceSnapshot.resource(candidate.skinId(),
                    candidate.model(), candidate.fingerprint(), 0L));
        }
        return PlayerSkinResolutionPlan.fallback(previous, result.diagnostics().get(0).message());
    }

    private void requireInsideRoot(Path root, Path candidate) {
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Skin path must stay inside the content root.");
        }
    }

    private void requirePngSignature(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) {
            throw new IllegalArgumentException("Skin PNG is truncated.");
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (bytes[index] != PNG_SIGNATURE[index]) {
                throw new IllegalArgumentException("Skin file does not have a PNG signature.");
            }
        }
    }

    private void requireDimensions(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null || image.getWidth() != 64 || image.getHeight() != 64) {
                throw new IllegalArgumentException("Skin PNG must be exactly 64x64.");
            }
        }
    }

    private String fingerprint(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is required.", ex);
        }
    }
}
