package io.github.kxyr0.echobackgroundnpc.api.identity.resource;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;

/** Exact authored location key resolved by the navigation boundary. */
public record BackgroundLocationId(String value) {
    public BackgroundLocationId { value = new BackgroundNpcId(value).value(); }
    @Override public String toString() { return value; }
}
