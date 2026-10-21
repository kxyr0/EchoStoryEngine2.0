package io.github.kxyr0.echoitem.forge.api;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import java.util.List;

/**
 * Query API for bounded item diagnostics.
 */
public interface EchoItemDiagnosticsView {
    List<ItemDefinitionDiagnostic> latest();

    List<ItemDefinitionDiagnostic> forItem(EchoItemId id);
}
