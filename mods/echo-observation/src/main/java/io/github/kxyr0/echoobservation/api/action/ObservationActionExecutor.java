package io.github.kxyr0.echoobservation.api.action;

public interface ObservationActionExecutor {
    String target();

    boolean execute(ObservationActionContext context, ObservationScriptAction action);
}
