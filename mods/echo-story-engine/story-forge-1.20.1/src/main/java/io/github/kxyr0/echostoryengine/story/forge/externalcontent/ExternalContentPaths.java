package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Defines the external JDH content pack location relative to the game directory.
 */
public final class ExternalContentPaths {
    public static final String CONTENT_MOD_ID = "echo_story_content_pack";
    public static final String PACK_ID = "echo_story_engine:jdh_external_content";
    public static final String PACK_TITLE = "Echo Story JDH external content";
    public static final String SEED_ROOT = "story/content-pack/jdh";
    public static final String CONFIG_SEED_ROOT = "story/configs/jdh";
    public static final String ARCHIVE_MARKER = ".archived";

    private final Path gameDirectory;

    public ExternalContentPaths(Path gameDirectory) {
        this.gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
    }

    public Path packRoot() {
        return gameDirectory.resolve("story").resolve("content-pack").resolve("jdh");
    }

    public Path configRoot() {
        return gameDirectory.resolve("story").resolve("configs").resolve("jdh");
    }

}
