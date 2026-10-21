package io.github.kxyr0.echotrigger.api.callback;

public enum TriggerCallbackKind {
    ENTER("OnTriggerEnter"),
    EXIT("OnTriggerExit"),
    STAY("OnTriggerStay");

    private final String methodName;

    TriggerCallbackKind(String methodName) {
        this.methodName = methodName;
    }

    public String methodName() {
        return methodName;
    }
}
