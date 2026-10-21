package io.github.kxyr0.echoitem.core.asset;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.core.reload.ReloadCounters;
import java.util.Arrays;
import java.util.List;

/**
 * Validates PNG bytes without invoking EchoScript.
 */
public final class TextureCandidateValidator {
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public AssetValidationResult validate(String sourcePath, byte[] bytes) {
        if (bytes.length < PNG.length || !Arrays.equals(Arrays.copyOf(bytes, PNG.length), PNG)) {
            return new AssetValidationResult(false, List.of(ItemDefinitionDiagnostic.error(
                    ItemDiagnosticCodes.TEXTURE_DECODE_FAILED, "PNG header is invalid.", sourcePath)),
                    ReloadCounters.texture(1, 0));
        }
        return new AssetValidationResult(true, List.of(), ReloadCounters.texture(1, 1));
    }
}
