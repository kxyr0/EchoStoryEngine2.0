package io.github.kxyr0.echonavigation.forge.api.service;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.forge.api.backend.NavigationBackendView;
import io.github.kxyr0.echonavigation.forge.api.view.NavigationDiagnosticsView;
import java.util.List;
import java.util.Optional;

public interface NavigationQueryService {
    boolean isMoving(NavigationSubjectId subjectId);

    Optional<NavigationSnapshot> active(NavigationSubjectId subjectId);

    Optional<NavigationSnapshot> move(NavigationMoveId moveId);

    List<NavigationSnapshot> list(int limit);

    List<NavigationBackendView> backends();

    NavigationSettingsSnapshot settings();

    NavigationDiagnosticsView diagnostics();
}
