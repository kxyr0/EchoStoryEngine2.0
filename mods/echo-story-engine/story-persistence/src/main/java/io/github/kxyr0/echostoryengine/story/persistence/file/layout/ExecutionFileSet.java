package io.github.kxyr0.echostoryengine.story.persistence.file.layout;

import java.nio.file.Path;

/**
 * Primary, backup and temp paths for one persisted execution snapshot.
 */
public record ExecutionFileSet(Path primary, Path backup, Path temp) {
}
