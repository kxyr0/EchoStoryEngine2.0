package io.github.kxyr0.echovisibility.forge.client.concealment;

import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcHeadRenderContext;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;

public final class VisibilityNpcRenderExtension implements NpcClientRenderExtension {
    @Override
    public NpcClientRenderResult render(NpcHeadRenderContext context) {
        return VisibilityClientRuntime.instance().hidesNpc(context.actorId().value())
                ? NpcClientRenderResult.CANCELLED : NpcClientRenderResult.NOT_HANDLED;
    }
}
