package io.github.kxyr0.echonpc.forge.client.form;

import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartId;
import java.util.Optional;

record Transform(
        NpcBodyPartId partId,
        double scaleX,
        double scaleY,
        double scaleZ,
        double pivotX,
        double pivotY,
        double pivotZ,
        double rotationX,
        double rotationY,
        double rotationZ
) {
    static Optional<Transform> parse(String value) {
        String[] fields = value.split(",", -1);
        if (fields.length != 10) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Transform(NpcBodyPartId.valueOf(fields[0].toUpperCase(java.util.Locale.ROOT)),
                    number(fields[1]), number(fields[2]), number(fields[3]), number(fields[4]), number(fields[5]),
                    number(fields[6]), number(fields[7]), number(fields[8]), number(fields[9])));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static double number(String value) {
        return Double.parseDouble(value);
    }
}
