package io.github.kxyr0.echobackgroundnpc.core.navigation;

import io.github.kxyr0.echobackgroundnpc.core.navigation.SharedRoutePlan.RouteKey;
import java.util.Optional;

/** Real integration boundary implemented by the Echo Navigation adapter. */
@FunctionalInterface
public interface BackgroundRoutePlanner {
    Optional<SharedRoutePlan> plan(RouteKey key);
}
