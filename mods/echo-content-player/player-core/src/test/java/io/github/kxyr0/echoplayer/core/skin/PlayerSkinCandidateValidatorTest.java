package io.github.kxyr0.echoplayer.core.skin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PlayerSkinCandidateValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void validSixtyFourBySixtyFourPngPasses() throws Exception {
        Path skin = writePng("default.png", 64, 64);
        PlayerSkinCandidateValidator validator = new PlayerSkinCandidateValidator();

        var result = validator.validate(tempDir, skin, PlayerSkinId.parse("jdh:player/default"),
                PlayerSkinModel.CLASSIC);

        assertTrue(result.successful());
        assertTrue(result.value().orElseThrow().fingerprint().length() > 10);
    }

    @Test
    void brokenPngFallsBackToPreviousKnownGoodOrAccount() throws Exception {
        Path bad = writePng("bad.png", 32, 32);
        PlayerSkinCandidateValidator validator = new PlayerSkinCandidateValidator();

        var invalid = validator.validate(tempDir, bad, PlayerSkinId.parse("jdh:player/default"),
                PlayerSkinModel.CLASSIC);
        var previous = PlayerAppearanceSnapshot.resource(PlayerSkinId.parse("jdh:player/old"),
                PlayerSkinModel.SLIM, "old", 1L);
        PlayerSkinResolutionPlan withPrevious = validator.plan(invalid, Optional.of(previous));
        PlayerSkinResolutionPlan account = validator.plan(invalid, Optional.empty());

        assertFalse(invalid.successful());
        assertTrue(withPrevious.previousKnownGood().isPresent());
        assertTrue(account.accountFallback());
    }

    private Path writePng(String name, int width, int height) throws Exception {
        Path path = tempDir.resolve(name);
        Files.createDirectories(path.getParent());
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", path.toFile());
        return path;
    }
}
