package io.github.kxyr0.echocutscene.forge.api.service;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import java.util.List;
import java.util.Optional;

public interface CameraTrackCatalogView {
    Optional<CameraTrackSnapshot> find(CameraTrackId trackId);

    List<CameraTrackSnapshot> all();
}
