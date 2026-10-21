package io.github.kxyr0.echodamagenumbers.server;

import io.github.kxyr0.echodamagenumbers.network.DamageNumberNetwork;
import io.github.kxyr0.echodamagenumbers.network.DamagePopupPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public final class DamageNumberServerEvents {
    private DamageNumberServerEvents() {
    }

    public static void onDamage(LivingDamageEvent event) {
        if (event.isCanceled() || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0.0F) return;
        ServerPlayer attacker = DamageNumberEnemyFilter.attacker(event.getSource()).orElse(null);
        LivingEntity victim = event.getEntity();
        if (attacker == null || !DamageNumberEnemyFilter.isEnemy(attacker, victim)) return;
        DamageNumberNetwork.send(attacker, new DamagePopupPacket(
                victim.getX(), victim.getBoundingBox().maxY + 0.22D, victim.getZ(), event.getAmount()));
    }
}

