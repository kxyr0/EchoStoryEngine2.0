package io.github.kxyr0.echocutscene.forge.service;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.forge.api.service.CameraTrackCatalogView;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutsceneResourceCatalog;
import java.util.List;
import java.util.Optional;

public final class ForgeCameraTrackCatalog implements CameraTrackCatalogView {
    private final ServerCutsceneResourceCatalog catalog;

    public ForgeCameraTrackCatalog(ServerCutsceneResourceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<CameraTrackSnapshot> find(CameraTrackId trackId) {
        return catalog.track(trackId);
    }

    @Override
    public List<CameraTrackSnapshot> all() {
        return catalog.tracks();
    }
}
