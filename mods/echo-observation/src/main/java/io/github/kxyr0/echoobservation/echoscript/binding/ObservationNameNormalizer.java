package io.github.kxyr0.echoobservation.echoscript.binding;

import java.util.Locale;

public final class ObservationNameNormalizer {
    private ObservationNameNormalizer() {
    }

    public static String normalize(String symbol) {
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < symbol.length(); index++) {
            char current = symbol.charAt(index);
            char previous = index == 0 ? 0 : symbol.charAt(index - 1);
            char next = index + 1 < symbol.length() ? symbol.charAt(index + 1) : 0;
            boolean boundary = index > 0 && Character.isUpperCase(current)
                    && (Character.isLowerCase(previous) || Character.isDigit(previous)
                    || Character.isUpperCase(previous) && Character.isLowerCase(next));
            if (boundary && output.length() > 0 && output.charAt(output.length() - 1) != '_') {
                output.append('_');
            }
            output.append(Character.isLetterOrDigit(current)
                    ? Character.toLowerCase(current) : '_');
        }
        return output.toString().replaceAll("_+", "_").replaceAll("^_|_$", "")
                .toLowerCase(Locale.ROOT);
    }
}
