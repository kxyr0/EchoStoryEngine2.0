package io.github.kxyr0.echobackgroundnpc.forge.api.service;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.BackgroundAgentState;
import java.util.List;
import java.util.Optional;

public interface BackgroundNpcService {
    List<BackgroundAgentState> agents();
    Optional<BackgroundAgentState> find(BackgroundNpcId id);
    boolean reload();
    String performanceReport();
}
