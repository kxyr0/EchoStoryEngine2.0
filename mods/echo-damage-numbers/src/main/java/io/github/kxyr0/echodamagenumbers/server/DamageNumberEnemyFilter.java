package io.github.kxyr0.echodamagenumbers.server;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

final class DamageNumberEnemyFilter {
    private DamageNumberEnemyFilter() {
    }

    static Optional<ServerPlayer> attacker(DamageSource source) {
        Optional<ServerPlayer> causing = playerFrom(source.getEntity());
        if (causing.isPresent()) return causing;
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) return playerFrom(projectile.getOwner());
        return playerFrom(direct);
    }

    static boolean isEnemy(ServerPlayer attacker, LivingEntity victim) {
        if (victim == attacker || victim.isAlliedTo(attacker) || attacker.isAlliedTo(victim)) return false;
        if (victim instanceof TamableAnimal tame && tame.isOwnedBy(attacker)) return false;
        if (victim instanceof Enemy) return true;
        if (victim instanceof Mob mob && mob.getTarget() == attacker) return true;
        return victim instanceof Player;
    }

    private static Optional<ServerPlayer> playerFrom(Entity entity) {
        if (entity instanceof ServerPlayer player) return Optional.of(player);
        if (entity instanceof TamableAnimal tame && tame.getOwner() instanceof ServerPlayer owner) {
            return Optional.of(owner);
        }
        return Optional.empty();
    }
}
