package io.github.kxyr0.echoitem.core.asset;

import com.google.gson.JsonParser;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.core.reload.ReloadCounters;
import java.util.List;

/**
 * Validates optional item model JSON without invoking EchoScript.
 */
public final class ModelCandidateValidator {
    public AssetValidationResult validate(String sourcePath, String json) {
        try {
            JsonParser.parseString(json).getAsJsonObject();
            return new AssetValidationResult(true, List.of(), ReloadCounters.model(1, 1));
        } catch (RuntimeException ex) {
            return new AssetValidationResult(false, List.of(ItemDefinitionDiagnostic.error(
                    ItemDiagnosticCodes.MODEL_PARSE_FAILED, "Model JSON is invalid.", sourcePath)),
                    ReloadCounters.model(1, 0));
        }
    }
}
