package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.model.LayerRole;
import java.util.Optional;
import java.util.Set;

/**
 * Parser for the high-level face rig DSL.
 */
final class FaceScriptParser {
    private FaceScriptCursor cursor;

    FaceScriptModel.Face parse(String source) {
        cursor = new FaceScriptCursor(source);
        cursor.parseNamespace();
        cursor.expectIdentifier("face");
        String name = cursor.identifier();
        cursor.expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after face name.");
        String id = cursor.expect(FaceScriptLexer.Kind.STRING, "Face id must be a string.").text();
        cursor.expect(FaceScriptLexer.Kind.RIGHT_PAREN, "Expected ')' after face id.");
        FaceScriptModel.Face face = new FaceScriptModel.Face(name, id);
        cursor.expect(FaceScriptLexer.Kind.LEFT_BRACE, "Expected face body.");
        while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
            parseFaceMember(face);
        }
        cursor.expectEnd();
        return face;
    }

    private void parseFaceMember(FaceScriptModel.Face face) {
        String name = cursor.identifier();
        switch (name) {
            case "Textures" -> face.textureRoot = Optional.of(cursor.stringCall("Textures"));
            case "Eyes" -> parseEyes(face);
            case "Brows" -> parseBrows(face);
            case "Bone" -> parseExtraBone(face);
            case "Blink" -> parseBlink(face);
            default -> throw cursor.error("Unknown face member '" + name + "'.");
        }
    }

    private void parseEyes(FaceScriptModel.Face face) {
        cursor.expect(FaceScriptLexer.Kind.LEFT_BRACE, "Expected Eyes block.");
        while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
            String name = cursor.identifier();
            FaceScriptModel.PartKind kind = eyeKind(name);
            face.parts.add(parsePart(kind, new FaceScriptModel.Settings(), cursor.identifier()));
        }
    }

    private void parseBrows(FaceScriptModel.Face face) {
        FaceScriptModel.Settings group = new FaceScriptModel.Settings();
        cursor.expect(FaceScriptLexer.Kind.LEFT_BRACE, "Expected Brows block.");
        while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
            if (cursor.peekIdentifier("Pivot")) {
                cursor.identifier();
                group.pivot = Optional.of(cursor.pivotCall());
            } else {
                face.parts.add(parsePart(FaceScriptModel.PartKind.BROW, group.copy(), cursor.identifier()));
            }
        }
    }

    private FaceScriptModel.Part parsePart(
            FaceScriptModel.PartKind kind,
            FaceScriptModel.Settings base,
            String variant
    ) {
        validateVariant(kind, variant);
        FaceScriptModel.Settings settings = parseTerminator(base);
        return new FaceScriptModel.Part(kind, variant, settings);
    }

    private FaceScriptModel.Settings parseTerminator(FaceScriptModel.Settings base) {
        if (cursor.match(FaceScriptLexer.Kind.SEMICOLON)) {
            return base;
        }
        cursor.expect(FaceScriptLexer.Kind.LEFT_BRACE, "Expected ';' or settings block.");
        while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
            parseVisualSetting(base);
        }
        return base;
    }

    private void parseExtraBone(FaceScriptModel.Face face) {
        String name = cursor.identifier();
        Optional<String> parent = Optional.empty();
        FaceScriptModel.Settings settings = new FaceScriptModel.Settings();
        if (cursor.match(FaceScriptLexer.Kind.LEFT_BRACE)) {
            while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
                parent = parseBoneSetting(parent, settings);
            }
        } else {
            cursor.expect(FaceScriptLexer.Kind.SEMICOLON, "Expected ';' after Bone.");
        }
        face.bones.add(new FaceScriptModel.ExtraBone(name, parent, settings));
    }

    private Optional<String> parseBoneSetting(Optional<String> parent, FaceScriptModel.Settings settings) {
        String property = cursor.identifier();
        return switch (property) {
            case "Parent" -> Optional.of(cursor.stringCall("Parent"));
            case "Position" -> updatePosition(parent, settings);
            case "Pivot" -> updatePivot(parent, settings);
            case "Texture", "TextureArea", "Size", "Order", "Role" -> {
                parseVisualSetting(settings, property);
                yield parent;
            }
            default -> throw cursor.error("Unsupported Bone property '" + property + "'.");
        };
    }

    private void parseBlink(FaceScriptModel.Face face) {
        String id = cursor.expect(FaceScriptLexer.Kind.STRING, "Blink id must be a string.").text();
        FaceScriptModel.Blink blink = new FaceScriptModel.Blink(id);
        if (cursor.match(FaceScriptLexer.Kind.SEMICOLON)) {
            face.blinks.add(blink);
            return;
        }
        cursor.expect(FaceScriptLexer.Kind.LEFT_BRACE, "Expected Blink block.");
        while (!cursor.match(FaceScriptLexer.Kind.RIGHT_BRACE)) {
            parseBlinkSetting(blink);
        }
        face.blinks.add(blink);
    }

    private void parseBlinkSetting(FaceScriptModel.Blink blink) {
        String property = cursor.identifier();
        switch (property) {
            case "Travel" -> blink.travel = Optional.of(cursor.vec2Call("Travel"));
            case "Gaze" -> blink.gaze = Optional.of(cursor.vec2Call("Gaze"));
            case "Idle" -> blink.idle = Optional.of(cursor.boolCall("Idle"));
            case "Timing" -> blink.timing = Optional.of(cursor.timingCall());
            case "PeriodTicks", "CloseTicks", "HoldTicks", "OpenTicks" -> updateBlinkTiming(blink, property);
            default -> throw cursor.error("Unsupported Blink property '" + property + "'.");
        }
    }

    private void updateBlinkTiming(FaceScriptModel.Blink blink, String property) {
        int value = cursor.intCall(property);
        FaceScriptModel.Timing timing = blink.timing.orElse(new FaceScriptModel.Timing(80, 3, 2, 4));
        blink.timing = Optional.of(switch (property) {
            case "PeriodTicks" -> new FaceScriptModel.Timing(value, timing.closeTicks(),
                    timing.holdTicks(), timing.openTicks());
            case "CloseTicks" -> new FaceScriptModel.Timing(timing.periodTicks(), value,
                    timing.holdTicks(), timing.openTicks());
            case "HoldTicks" -> new FaceScriptModel.Timing(timing.periodTicks(), timing.closeTicks(),
                    value, timing.openTicks());
            default -> new FaceScriptModel.Timing(timing.periodTicks(), timing.closeTicks(),
                    timing.holdTicks(), value);
        });
    }

    private void parseVisualSetting(FaceScriptModel.Settings settings) {
        parseVisualSetting(settings, cursor.identifier());
    }

    private void parseVisualSetting(FaceScriptModel.Settings settings, String property) {
        switch (property) {
            case "Position" -> settings.position = Optional.of(cursor.positionCall());
            case "Pivot" -> settings.pivot = Optional.of(cursor.pivotCall());
            case "Texture" -> settings.texture = Optional.of(cursor.stringCall("Texture"));
            case "TextureArea" -> settings.textureArea = Optional.of(cursor.textureAreaCall());
            case "Size" -> settings.size = Optional.of(cursor.sizeCall());
            case "Order" -> settings.order = Optional.of(cursor.intCall("Order"));
            case "Bone" -> settings.bone = Optional.of(cursor.stringCall("Bone"));
            case "Role" -> settings.role = Optional.of(LayerRole.valueOf(cursor.stringCall("Role")));
            default -> throw cursor.error("Unsupported visual property '" + property + "'.");
        }
    }

    private Optional<String> updatePosition(Optional<String> parent, FaceScriptModel.Settings settings) {
        settings.position = Optional.of(cursor.positionCall());
        return parent;
    }

    private Optional<String> updatePivot(Optional<String> parent, FaceScriptModel.Settings settings) {
        settings.pivot = Optional.of(cursor.pivotCall());
        return parent;
    }

    private FaceScriptModel.PartKind eyeKind(String name) {
        return switch (name) {
            case "Sclera" -> FaceScriptModel.PartKind.SCLERA;
            case "Pupil" -> FaceScriptModel.PartKind.PUPIL;
            case "Eyelid" -> FaceScriptModel.PartKind.EYELID;
            default -> throw cursor.error("Eyes may contain only Sclera, Pupil and Eyelid.");
        };
    }

    private void validateVariant(FaceScriptModel.PartKind kind, String value) {
        Set<String> allowed = kind == FaceScriptModel.PartKind.EYELID
                ? Set.of("Upper", "Lower")
                : Set.of("Left", "Right");
        if (!allowed.contains(value)) {
            throw cursor.error(kind.name() + " expects " + String.join(" or ", allowed) + ", got '" + value + "'.");
        }
    }
}
