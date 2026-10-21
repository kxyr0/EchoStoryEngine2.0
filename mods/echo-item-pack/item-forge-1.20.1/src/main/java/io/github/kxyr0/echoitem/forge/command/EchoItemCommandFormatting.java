package io.github.kxyr0.echoitem.forge.command;

import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;

final class EchoItemCommandFormatting {
    String inspect(WeaponDefinition definition, boolean registered) {
        return "Item ID=" + definition.id()
                + ", kind=" + definition.kind()
                + ", registered=" + registered
                + ", available=" + definition.available()
                + ", revision=" + definition.revision().value()
                + ", damage=" + definition.combat().attackDamage()
                + ", attacksPerSecond=" + definition.combat().attacksPerSecond()
                + ", durability=" + definition.durability().durability()
                + ", unbreakable=" + definition.durability().unbreakable()
                + ", texture=" + definition.visual().texture()
                + ", model=" + definition.visual().model().orElse("generated handheld");
    }
}
