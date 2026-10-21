package io.github.kxyr0.echovisibility.core.report;

import io.github.kxyr0.echovisibility.api.identity.VisibilityEventId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;

public final class VisibilityEventIds {
    private VisibilityEventIds() {
    }

    public static VisibilityEventId from(VisibilityPlayerRef player, VisibilityReport report) {
        String value = report.watchId().value() + "/generation/" + report.generation().value()
                + "/player/" + player.uuid() + "/" + report.transition().kind().name().toLowerCase()
                + "/" + report.reportSequence();
        return new VisibilityEventId(value);
    }
}
