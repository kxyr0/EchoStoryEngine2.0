package io.github.kxyr0.echoobservation.core.catalog;

public final class ObservationCatalog {
    private ObservationCatalogSnapshot current = ObservationCatalogSnapshot.empty();

    public synchronized boolean install(ObservationCatalogCandidate candidate) {
        if (!candidate.valid()) {
            return false;
        }
        current = new ObservationCatalogSnapshot(current.revision() + 1L, candidate.resources());
        return true;
    }

    public synchronized ObservationCatalogSnapshot snapshot() {
        return current;
    }
}
