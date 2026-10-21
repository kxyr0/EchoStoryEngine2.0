package io.github.kxyr0.echodevscenarios.launcher;

import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.runtime.DevScenarioRuntime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class EchoPlayService {
    private final ScenarioCatalogStore catalog;
    private final DevScenarioRuntime runtime;
    private final AtomicLong sequence = new AtomicLong();

    public EchoPlayService(ScenarioCatalogStore catalog, DevScenarioRuntime runtime) {
        this.catalog = catalog;
        this.runtime = runtime;
    }

    public List<ScenarioDefinition> list() { return catalog.list(); }
    public List<ScenarioDefinition> search(String query) { return catalog.search(query); }
    public ScenarioTreeNode tree() { return new ScenarioTreeBuilder().build(catalog.list()); }
    public Optional<ScenarioInspection> status() { return runtime.status(); }

    public ScenarioInspection play(ScenarioId id) {
        if (catalog.error(id).isPresent()) throw new IllegalStateException("DEV_SCENARIO_INVALID: "
                + catalog.error(id).orElseThrow());
        ScenarioDefinition definition = catalog.find(id)
                .orElseThrow(() -> new IllegalArgumentException("DEV_SCENARIO_NOT_FOUND: " + id.value()));
        catalog.launched(id);
        return runtime.start(definition, new ScenarioSessionId("scenario-" + sequence.incrementAndGet()));
    }

    public ScenarioInspection restart() { return runtime.restart(); }
    public ScenarioInspection stop() { return runtime.stop(); }
}
