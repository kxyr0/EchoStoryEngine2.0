package io.github.kxyr0.echosavemigration.graph;

public final class MigrationPlanningException extends RuntimeException {
    private final String code;

    public MigrationPlanningException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
