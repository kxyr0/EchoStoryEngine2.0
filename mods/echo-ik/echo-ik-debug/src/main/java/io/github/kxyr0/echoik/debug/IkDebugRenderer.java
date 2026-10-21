package io.github.kxyr0.echoik.debug;

import java.util.List;

/**
 * Adapter port for rendering debug primitives in any host.
 */
public interface IkDebugRenderer {
    void render(List<DebugPrimitive> primitives);
}
