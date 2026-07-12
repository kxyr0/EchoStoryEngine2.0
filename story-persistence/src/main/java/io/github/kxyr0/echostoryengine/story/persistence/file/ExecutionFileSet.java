package io.github.kxyr0.echostoryengine.story.persistence.file;

import java.nio.file.Path;

record ExecutionFileSet(Path primary, Path backup, Path temp) {
}
