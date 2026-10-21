package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class TimeDeliveryIds {
    private TimeDeliveryIds() { }

    static String delivery(String occurrence, String subscription, String audience, boolean once) {
        String key = (once ? "once" : occurrence) + "|" + subscription + "|" + audience;
        return hex(key);
    }

    private static String hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            for (byte item : digest) output.append(String.format(java.util.Locale.ROOT, "%02x", item));
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
