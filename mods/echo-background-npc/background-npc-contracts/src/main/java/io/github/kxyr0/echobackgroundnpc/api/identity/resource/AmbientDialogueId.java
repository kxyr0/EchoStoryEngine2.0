package io.github.kxyr0.echobackgroundnpc.api.identity.resource;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;

public record AmbientDialogueId(String value) {
    public AmbientDialogueId { value = new BackgroundNpcId(value).value(); }
    @Override public String toString() { return value; }
}
