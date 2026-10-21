package io.github.kxyr0.echocontentdoctor.validation.reference;

final class EditDistance {
    int between(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            int[] current = new int[right.length() + 1];
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int replace = previous[column - 1] + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(replace, Math.min(previous[column] + 1, current[column - 1] + 1));
            }
            previous = current;
        }
        return previous[right.length()];
    }
}
