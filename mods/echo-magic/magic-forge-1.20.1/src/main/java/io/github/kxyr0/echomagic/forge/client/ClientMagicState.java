package io.github.kxyr0.echomagic.forge.client;

import java.util.List;
import java.util.Optional;

public final class ClientMagicState {
    private List<String> known = List.of();
    private Optional<String> selected = Optional.empty();
    private Optional<String> gestureSchool = Optional.empty();
    private long cooldown;
    private String lastRejection = "";
    private long revision;

    public void update(List<String> spells, Optional<String> selectedSpell, Optional<String> newGestureSchool,
                       long remaining, long newRevision) {
        if (newRevision < revision) return;
        known = List.copyOf(spells);
        selected = selectedSpell;
        gestureSchool = newGestureSchool;
        cooldown = remaining;
        revision = newRevision;
    }
    public void rejected(String reason) { lastRejection = reason; }
    public void tick() { if (cooldown > 0L) cooldown--; }
    public List<String> known() { return known; }
    public Optional<String> selected() { return selected; }
    public Optional<String> gestureSchool() { return gestureSchool; }
    public long cooldown() { return cooldown; }
    public String lastRejection() { return lastRejection; }
}
