package io.github.kxyr0.echoconcentration.api;

public interface ConcentrationService {
    ConcentrationSnapshot snapshot();

    boolean enter();

    boolean exit();
}
