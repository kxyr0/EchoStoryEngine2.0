package io.github.kxyr0.echobackgroundnpc.api.runtime;

public enum BackgroundSimulationLod {
    VIRTUAL(0), COARSE(4), ACTIVE(2), NEAR(1);
    private final int updateIntervalTicks;
    BackgroundSimulationLod(int updateIntervalTicks) { this.updateIntervalTicks = updateIntervalTicks; }
    public int updateIntervalTicks() { return updateIntervalTicks; }
}
