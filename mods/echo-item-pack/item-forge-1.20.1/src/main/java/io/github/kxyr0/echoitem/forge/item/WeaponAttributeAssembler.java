package io.github.kxyr0.echoitem.forge.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class WeaponAttributeAssembler {
    private static final UUID DAMAGE_ID = UUID.fromString("2f7a89ec-198c-4a6b-815d-677992f257a2");
    private static final UUID SPEED_ID = UUID.fromString("4421d7fc-c50a-4d01-afde-81bd6c61bafc");

    Multimap<Attribute, AttributeModifier> assemble(WeaponDefinition definition) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_ID, "Echo weapon damage",
                definition.combat().attackDamage() - 1.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_ID, "Echo weapon speed",
                definition.combat().minecraftAttackSpeedModifier(), AttributeModifier.Operation.ADDITION));
        return builder.build();
    }
}
