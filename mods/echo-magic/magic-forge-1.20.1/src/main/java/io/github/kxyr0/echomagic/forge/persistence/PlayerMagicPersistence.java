package io.github.kxyr0.echomagic.forge.persistence;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record PlayerMagicPersistence(Set<SpellId> known, Optional<SpellId> selected,
        Map<SpellId, Long> cooldownRemainingTicks, Optional<MagicGestureSymbol> gestureSchool,
        Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> gestureTemplates,
        long knownRevision, long selectedRevision) {
    public PlayerMagicPersistence {
        known = Set.copyOf(known);
        selected = selected == null ? Optional.empty() : selected;
        cooldownRemainingTicks = Map.copyOf(cooldownRemainingTicks);
        gestureSchool = gestureSchool == null ? Optional.empty() : gestureSchool;
        gestureTemplates = Map.copyOf(gestureTemplates);
    }
    public static PlayerMagicPersistence empty() {
        return new PlayerMagicPersistence(Set.of(), Optional.empty(), Map.of(), Optional.empty(), Map.of(), 0L, 0L);
    }
}
