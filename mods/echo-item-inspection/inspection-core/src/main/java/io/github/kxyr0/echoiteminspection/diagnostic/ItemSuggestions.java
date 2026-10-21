package io.github.kxyr0.echoiteminspection.diagnostic;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Deterministic bounded typo suggestions for absent item IDs. */
public final class ItemSuggestions {
    private ItemSuggestions() { }

    public static List<String> find(String requested, Set<String> known) {
        int threshold = Math.max(3, requested.length() / 3);
        String namespace = requested.substring(0, requested.indexOf(':') + 1);
        return known.stream().filter(value -> distance(requested, value) <= threshold)
                .sorted(Comparator.comparing((String value) -> !value.startsWith(namespace))
                        .thenComparingInt(value -> distance(requested, value)).thenComparing(value -> value))
                .limit(3).toList();
    }

    private static int distance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        for (int index = 0; index < previous.length; index++) previous[index] = index;
        for (int row = 1; row <= first.length(); row++) {
            int[] current = new int[second.length() + 1];
            current[0] = row;
            for (int column = 1; column <= second.length(); column++) {
                int substitution = previous[column - 1] + (first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(substitution, Math.min(previous[column] + 1, current[column - 1] + 1));
            }
            previous = current;
        }
        return previous[second.length()];
    }
}
