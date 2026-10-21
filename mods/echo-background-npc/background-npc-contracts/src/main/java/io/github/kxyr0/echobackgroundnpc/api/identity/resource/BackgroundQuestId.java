package io.github.kxyr0.echobackgroundnpc.api.identity.resource;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;

/** QuestResource identifier used as an ambient quest entrypoint. */
public record BackgroundQuestId(String value) {
    public BackgroundQuestId { value = new BackgroundNpcId(value).value(); }
    @Override public String toString() { return value; }
}
