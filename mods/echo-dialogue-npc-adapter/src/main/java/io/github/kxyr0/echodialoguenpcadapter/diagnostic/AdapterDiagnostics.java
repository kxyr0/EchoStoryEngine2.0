package io.github.kxyr0.echodialoguenpcadapter.diagnostic;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;

public final class AdapterDiagnostics {
    private static final int MAX_WARNINGS = 32;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Set<String> warned = new LinkedHashSet<>();

    public void warnOnce(String code, String message) {
        String checkedCode = Objects.requireNonNull(code, "code").trim();
        String checkedMessage = Objects.requireNonNull(message, "message").trim();
        if (warned.add(checkedCode)) {
            LOGGER.warn("{} {}", checkedCode, checkedMessage);
        }
        while (warned.size() > MAX_WARNINGS) {
            warned.remove(warned.iterator().next());
        }
    }
}
