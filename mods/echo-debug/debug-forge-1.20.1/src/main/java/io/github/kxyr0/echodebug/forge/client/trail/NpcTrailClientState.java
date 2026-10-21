package io.github.kxyr0.echodebug.forge.client.trail;

import io.github.kxyr0.echodebug.core.trail.NpcTrailCollector;
import io.github.kxyr0.echodebug.core.trail.NpcTrailEvent;
import io.github.kxyr0.echodebug.core.trail.NpcTrailHealthSnapshot;
import io.github.kxyr0.echodebug.forge.config.EchoDebugConfig;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcTrailClientState {
    private final NpcTrailCollector collector = new NpcTrailCollector();
    private final AtomicReference<NpcTrailRenderPlan> published = new AtomicReference<>(NpcTrailRenderPlan.empty());
    private long publishedRevision = -1L;
    private boolean integrationAvailable;
    private long progressSnapshotsReceived;
    private long renderSnapshotsPublished;
    private long renderCalls;
    private int trailsInCurrentDimension;
    private int trailsCulled;
    private long segmentsSubmitted;
    private long lastRenderTick = -1L;
    private String lastDiagnostic = "client has not received NPC trail packets";

    public void accept(NpcTrailEvent event) {
        integrationAvailable = true;
        progressSnapshotsReceived++;
        collector.accept(event, EchoDebugConfig.npcTrailConfig());
        lastDiagnostic = collector.lastDiagnostic();
        publishIfChanged();
    }

    public int clearAll() {
        int count = collector.clearAll();
        publishIfChanged();
        return count;
    }

    public NpcTrailRenderPlan renderPlan() {
        return published.get();
    }

    public void recordRender(int trailsInDimension, int culled, int submittedSegments, long gameTick) {
        renderCalls++;
        trailsInCurrentDimension = trailsInDimension;
        trailsCulled = culled;
        segmentsSubmitted += submittedSegments;
        lastRenderTick = gameTick;
        lastDiagnostic = "render trails=" + trailsInDimension + " culled=" + culled
                + " segments=" + submittedSegments;
    }

    public NpcTrailHealthSnapshot health() {
        return new NpcTrailHealthSnapshot(integrationAvailable, progressSnapshotsReceived,
                progressSnapshotsReceived, collector.staleSnapshotsRejected(), collector.trailCount(),
                collector.pointsAccepted(), collector.pointsRejectedByDistance(),
                collector.pointsRejectedByInterval(), renderSnapshotsPublished, trailsInCurrentDimension,
                trailsCulled, segmentsSubmitted, renderCalls, lastRenderTick, lastDiagnostic);
    }

    private void publishIfChanged() {
        long revision = collector.snapshotRevision();
        if (revision != publishedRevision) {
            publishedRevision = revision;
            published.set(new NpcTrailRenderPlan(revision, collector.snapshot()));
            renderSnapshotsPublished++;
        }
    }
}
