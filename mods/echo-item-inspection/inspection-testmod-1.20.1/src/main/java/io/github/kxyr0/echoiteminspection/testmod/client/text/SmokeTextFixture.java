package io.github.kxyr0.echoiteminspection.testmod.client.text;

import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

final class SmokeTextFixture {
    static final String TITLE = "Осмотр предмета / Item inspection";
    static final String SHORT = "Короткое описание: кириллица, English, café.\n\nВторой абзац сохраняет перенос строки.";
    static final String LONG = ("Проверка длинного описания. Every paragraph must remain readable. "
            + "СловаБезПробеловДляПроверкиПереносаUnicodeABCDEFGHIJKLMN.\n\n").repeat(35);
    private Path file;
    private boolean owned;

    void create(Path gameDirectory) throws IOException {
        file = gameDirectory.resolve("story/item-inspection/echo_item_inspection_smoke")
                .resolve("text_" + UUID.randomUUID().toString().replace("-", "") + ".inspection.es");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source(SHORT), StandardOpenOption.CREATE_NEW);
        owned = true;
    }

    void longDescription() throws IOException {
        Files.writeString(file, source(LONG));
        if (!InspectionRuntime.reload()) throw new AssertionError("Text fixture reload failed");
    }

    void cleanup() throws IOException {
        if (!owned) return;
        Files.delete(file);
        owned = false;
        InspectionRuntime.reload();
    }

    private static String source(String description) {
        return "class SmokeText : ItemInspectionResource { Item(\"minecraft:clock\"); Title(\"" + TITLE
                + "\"); Description(\"" + description.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n") + "\"); }";
    }
}
