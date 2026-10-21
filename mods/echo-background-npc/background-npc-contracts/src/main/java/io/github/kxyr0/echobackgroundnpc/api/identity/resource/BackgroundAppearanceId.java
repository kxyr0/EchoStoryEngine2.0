package io.github.kxyr0.echobackgroundnpc.api.identity.resource;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;

public record BackgroundAppearanceId(String value) {
    public BackgroundAppearanceId { value = new BackgroundNpcId(value).value(); }
    @Override public String toString() { return value; }
}
