package io.github.kxyr0.echomagic.forge.api.summoned;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class SummonedWeaponData {
    private static final String ROOT = "EchoMagicSummonedWeapon";
    private static final String OWNER = "Owner";
    private static final String EXPIRES = "ExpiresAt";
    private static final String ORIGINAL = "OriginalItem";
    private static final String COLOR = "TrailColor";
    private static final String LENGTH = "BladeLength";
    private static final String MOTION = "Motion";
    private static final String FORM = "Form";
    private static final String ATTACK_PULSE = "AttackPulse";

    private SummonedWeaponData() { }

    public static void configure(ItemStack weapon, UUID owner, long expiresAt, ItemStack original,
                                 int color, float bladeLength, String motion) {
        configure(weapon, owner, expiresAt, original, color, bladeLength, motion, "rapier");
    }

    public static void configure(ItemStack weapon, UUID owner, long expiresAt, ItemStack original,
                                 int color, float bladeLength, String motion, String form) {
        Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(motion, "motion");
        Objects.requireNonNull(form, "form");
        if (weapon.isEmpty()) throw new IllegalArgumentException("A summoned weapon must not be empty.");
        if (!Float.isFinite(bladeLength) || bladeLength < 0.25F || bladeLength > 3.0F) {
            throw new IllegalArgumentException("Blade length must be between 0.25 and 3.0.");
        }
        CompoundTag data = weapon.getOrCreateTagElement(ROOT);
        data.putUUID(OWNER, owner);
        data.putLong(EXPIRES, expiresAt);
        data.put(ORIGINAL, original.save(new CompoundTag()));
        data.putInt(COLOR, color & 0xFFFFFF);
        data.putFloat(LENGTH, bladeLength);
        data.putString(MOTION, motion);
        data.putString(FORM, form);
    }

    public static boolean isSummoned(ItemStack stack) {
        CompoundTag data = data(stack);
        return data != null && data.hasUUID(OWNER) && data.contains(EXPIRES, Tag.TAG_LONG);
    }

    public static boolean ownedBy(ItemStack stack, UUID player) {
        return owner(stack).filter(player::equals).isPresent();
    }

    public static Optional<UUID> owner(ItemStack stack) {
        CompoundTag data = data(stack);
        return data != null && data.hasUUID(OWNER) ? Optional.of(data.getUUID(OWNER)) : Optional.empty();
    }

    public static long expiresAt(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null ? Long.MIN_VALUE : data.getLong(EXPIRES);
    }

    public static void renew(ItemStack stack, long expiresAt) {
        CompoundTag data = data(stack);
        if (data != null) data.putLong(EXPIRES, expiresAt);
    }

    public static ItemStack original(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null || !data.contains(ORIGINAL, Tag.TAG_COMPOUND)
                ? ItemStack.EMPTY : ItemStack.of(data.getCompound(ORIGINAL));
    }

    public static int color(ItemStack stack) {
        CompoundTag data = data(stack);
        int color = data == null || !data.contains(COLOR, Tag.TAG_INT) ? 0xFFFFFF : data.getInt(COLOR) & 0xFFFFFF;
        return switch (color) {
            case 0xFF3030 -> 0xAE1020;
            case 0xE51818, 0xFF493D -> 0xF45C10;
            default -> color;
        };
    }

    public static float bladeLength(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null || !data.contains(LENGTH, Tag.TAG_FLOAT) ? 1.0F : data.getFloat(LENGTH);
    }

    public static String motion(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null || !data.contains(MOTION, Tag.TAG_STRING) ? "slash" : data.getString(MOTION);
    }

    public static String form(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null || !data.contains(FORM, Tag.TAG_STRING) ? "rapier" : data.getString(FORM);
    }

    public static void recordAttackPulse(ItemStack stack) {
        CompoundTag data = data(stack);
        if (data != null) data.putInt(ATTACK_PULSE, data.getInt(ATTACK_PULSE) + 1);
    }

    public static int attackPulse(ItemStack stack) {
        CompoundTag data = data(stack);
        return data == null ? 0 : data.getInt(ATTACK_PULSE);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.getTagElement(ROOT);
    }
}
