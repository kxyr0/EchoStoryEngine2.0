package io.github.kxyr0.echoanimationauthoring.draft.recovery;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringTransform;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class DraftAutosaveStore {
    public Path autosavePath(Path sourcePath) {
        return sourcePath.resolveSibling(sourcePath.getFileName().toString() + ".authoring-autosave.properties");
    }

    public Path write(Path sourcePath, List<AuthoringKeyframeMutation> keyframes) throws IOException {
        Path target = autosavePath(sourcePath);
        Files.createDirectories(target.getParent());
        Properties properties = new Properties();
        properties.setProperty("keyframes", Integer.toString(keyframes.size()));
        for (int index = 0; index < keyframes.size(); index++) {
            write(properties, index, keyframes.get(index));
        }
        try (Writer output = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            properties.store(output, "Echo Animation Authoring autosave");
        }
        return target;
    }

    public Optional<List<AuthoringKeyframeMutation>> read(Path sourcePath) throws IOException {
        Path target = autosavePath(sourcePath);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader input = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            properties.load(input);
        }
        return Optional.of(read(properties));
    }

    private static void write(Properties properties, int index, AuthoringKeyframeMutation mutation) {
        String key = "keyframe." + index + ".";
        properties.setProperty(key + "bone", mutation.boneId());
        properties.setProperty(key + "frame", Integer.toString(mutation.frame()));
        properties.setProperty(key + "interpolation", mutation.interpolation());
        writeTransform(properties, key, mutation.transform());
    }

    private static void writeTransform(Properties properties, String key, AuthoringTransform transform) {
        properties.setProperty(key + "x", Double.toString(transform.x()));
        properties.setProperty(key + "y", Double.toString(transform.y()));
        properties.setProperty(key + "z", Double.toString(transform.z()));
        properties.setProperty(key + "rotX", Double.toString(transform.rotX()));
        properties.setProperty(key + "rotY", Double.toString(transform.rotY()));
        properties.setProperty(key + "rotZ", Double.toString(transform.rotZ()));
        properties.setProperty(key + "scaleX", Double.toString(transform.scaleX()));
        properties.setProperty(key + "scaleY", Double.toString(transform.scaleY()));
        properties.setProperty(key + "scaleZ", Double.toString(transform.scaleZ()));
    }

    private static List<AuthoringKeyframeMutation> read(Properties properties) {
        int count = Integer.parseInt(properties.getProperty("keyframes", "0"));
        List<AuthoringKeyframeMutation> keyframes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            keyframes.add(readKeyframe(properties, index));
        }
        return List.copyOf(keyframes);
    }

    private static AuthoringKeyframeMutation readKeyframe(Properties properties, int index) {
        String key = "keyframe." + index + ".";
        return new AuthoringKeyframeMutation(properties.getProperty(key + "bone"),
                Integer.parseInt(properties.getProperty(key + "frame")),
                readTransform(properties, key), properties.getProperty(key + "interpolation"));
    }

    private static AuthoringTransform readTransform(Properties properties, String key) {
        return new AuthoringTransform(number(properties, key + "x"), number(properties, key + "y"),
                number(properties, key + "z"), number(properties, key + "rotX"), number(properties, key + "rotY"),
                number(properties, key + "rotZ"), number(properties, key + "scaleX"),
                number(properties, key + "scaleY"), number(properties, key + "scaleZ"));
    }

    private static double number(Properties properties, String key) {
        return Double.parseDouble(properties.getProperty(key));
    }
}
