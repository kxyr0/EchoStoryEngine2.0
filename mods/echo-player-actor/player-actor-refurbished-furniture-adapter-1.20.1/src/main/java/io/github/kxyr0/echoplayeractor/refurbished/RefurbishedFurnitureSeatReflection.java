package io.github.kxyr0.echoplayeractor.refurbished;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

final class RefurbishedFurnitureSeatReflection {
    private static final String SEAT_CLASS = "com.mrcrayfish.furniture.refurbished.entity.Seat";

    private final Class<?> seatClass;
    private final Method availableAt;
    private final Method sit;

    private RefurbishedFurnitureSeatReflection(Class<?> seatClass, Method availableAt, Method sit) {
        this.seatClass = seatClass;
        this.availableAt = availableAt;
        this.sit = sit;
    }

    static Optional<RefurbishedFurnitureSeatReflection> create() {
        try {
            Class<?> seat = Class.forName(SEAT_CLASS);
            Method available = seat.getMethod("availableAt", Level.class, BlockPos.class);
            Method sitMethod = seat.getMethod("sit", Player.class, BlockPos.class, double.class, Direction.class);
            return Optional.of(new RefurbishedFurnitureSeatReflection(seat, available, sitMethod));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    boolean isSeat(Entity entity) {
        return seatClass.isInstance(entity);
    }

    boolean availableAt(Level level, BlockPos position) {
        try {
            return (Boolean) availableAt.invoke(null, level, position);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    boolean sit(Player player, BlockPos position, double seatHeight, Direction direction) {
        try {
            return (Boolean) sit.invoke(null, player, position, seatHeight, direction);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
