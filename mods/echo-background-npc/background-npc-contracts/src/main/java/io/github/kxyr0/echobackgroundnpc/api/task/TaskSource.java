package io.github.kxyr0.echobackgroundnpc.api.task;

public enum TaskSource {
    SCHEDULE(10), AMBIENT_CONVERSATION(20), STORY_OVERRIDE(30), DEBUG(40);

    private final int priority;
    TaskSource(int priority) { this.priority = priority; }
    public int priority() { return priority; }
}
