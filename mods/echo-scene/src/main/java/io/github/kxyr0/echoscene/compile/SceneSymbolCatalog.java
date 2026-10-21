package io.github.kxyr0.echoscene.compile;

import java.util.List;

public interface SceneSymbolCatalog {
    List<SceneSymbol> symbols();

    default List<SceneSymbol> resolve(String name) {
        return symbols().stream().filter(symbol -> symbol.name().equals(name)).toList();
    }

    default List<SceneSymbol> resolveResource(String resourceId, SceneSymbolKind kind) {
        return symbols().stream().filter(symbol -> symbol.resourceId().equals(resourceId) && symbol.kind() == kind)
                .toList();
    }

    static SceneSymbolCatalog of(List<SceneSymbol> symbols) {
        List<SceneSymbol> snapshot = List.copyOf(symbols);
        return () -> snapshot;
    }
    default List<String> suggest(String misspelled, SceneSymbolKind kind) {
        return symbols().stream().filter(s -> s.kind() == kind).map(SceneSymbol::name).distinct()
                .sorted(java.util.Comparator.comparingInt(name -> distance(misspelled, name))).limit(3).toList();
    }
    private static int distance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j < previous.length; j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1]
                        + (Character.toLowerCase(left.charAt(i - 1)) == Character.toLowerCase(right.charAt(j - 1)) ? 0 : 1));
            }
            previous = current;
        }
        return previous[right.length()];
    }
}
