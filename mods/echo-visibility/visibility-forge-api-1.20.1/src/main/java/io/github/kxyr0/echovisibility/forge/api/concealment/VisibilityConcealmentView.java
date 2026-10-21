package io.github.kxyr0.echovisibility.forge.api.concealment;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public interface VisibilityConcealmentView {
    List<VisibilityConcealmentSnapshot> leases(ServerPlayer player);
}
