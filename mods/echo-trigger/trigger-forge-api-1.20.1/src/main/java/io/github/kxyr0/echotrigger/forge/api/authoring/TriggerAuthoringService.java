package io.github.kxyr0.echotrigger.forge.api.authoring;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaCatalogView;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerCatalogRevisionView;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceReloadService;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceValidationService;

public interface TriggerAuthoringService extends AreaCatalogView, TriggerCatalogRevisionView,
        TriggerSourceValidationService, TriggerSourceReloadService {
}
