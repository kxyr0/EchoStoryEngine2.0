package io.github.kxyr0.echobackgroundnpc.api.identity.resource;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;

public record BackgroundAppearancePoolId(String value) {
    public BackgroundAppearancePoolId { value = new BackgroundNpcId(value).value(); }
    @Override public String toString() { return value; }
}
