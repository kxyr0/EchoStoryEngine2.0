package io.github.kxyr0.echoteleportmagic.api;

public enum TeleportState {
    CREATED, PREPARING, DEPARTING, CONCEALED, COMMITTED, ARRIVING, RESIDUAL,
    COMPLETED, CANCELLED, FAILED;

    public boolean terminal() { return this == COMPLETED || this == CANCELLED || this == FAILED; }
}
