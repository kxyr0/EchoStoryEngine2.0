package io.github.kxyr0.echothoughtspace.api;

import java.util.List;

public interface ThoughtNodeProvider {
    String providerId();
    long revision();
    List<ThoughtNodeSnapshot> nodes();
}

