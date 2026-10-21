package io.github.kxyr0.echonpcanimationadapter.resource;

import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpcanimationadapter.NpcAnimationBindingDefinition;
import io.github.kxyr0.echonpcanimationadapter.NpcAnimationBindingId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles adapter-owned NPC animation binding resources.
 */
public final class NpcAnimationBindingCompiler {
    private static final Pattern CALL = Pattern.compile("NpcAnimationBinding\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern FIRST_STRING = Pattern.compile("^\\s*\"([^\"]+)\"");

    public NpcAnimationBindingCatalog compile(List<NpcAnimationBindingResourceText> resources) {
        List<NpcAnimationBindingDefinition> bindings = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (NpcAnimationBindingResourceText resource : resources) {
            try {
                bindings.add(binding(resource));
            } catch (RuntimeException exception) {
                diagnostics.add("EANIM-A4-BINDING " + resource.sourceId() + ": " + exception.getMessage());
            }
        }
        detectAmbiguity(bindings, diagnostics);
        return new NpcAnimationBindingCatalog(bindings, diagnostics);
    }

    private NpcAnimationBindingDefinition binding(NpcAnimationBindingResourceText resource) {
        Matcher matcher = CALL.matcher(resource.text());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing NpcAnimationBinding(...) intrinsic.");
        }
        String body = matcher.group(1);
        String id = firstString(body).orElseThrow(() -> new IllegalArgumentException("Binding id is required."));
        Optional<ActorId> actor = field(body, "actor").map(ActorId::new);
        Optional<NpcDefinitionId> npc = field(body, "npc").map(NpcDefinitionId::new);
        AnimationProfileId profile = AnimationProfileId.parse(field(body, "profile")
                .orElseThrow(() -> new IllegalArgumentException("profile field is required.")));
        return new NpcAnimationBindingDefinition(NpcAnimationBindingId.parse(id), actor, npc, profile,
                fingerprint(resource.text()));
    }

    private void detectAmbiguity(List<NpcAnimationBindingDefinition> bindings, List<String> diagnostics) {
        Map<ActorId, NpcAnimationBindingDefinition> actors = new HashMap<>();
        Map<NpcDefinitionId, NpcAnimationBindingDefinition> definitions = new HashMap<>();
        for (NpcAnimationBindingDefinition binding : bindings) {
            binding.actorId().ifPresent(id -> {
                if (actors.putIfAbsent(id, binding) != null) {
                    diagnostics.add("EANIM-A4-BINDING Duplicate actor binding: " + id);
                }
            });
            binding.definitionId().ifPresent(id -> {
                if (definitions.putIfAbsent(id, binding) != null) {
                    diagnostics.add("EANIM-A4-BINDING Duplicate definition binding: " + id);
                }
            });
        }
    }

    private Optional<String> firstString(String body) {
        Matcher matcher = FIRST_STRING.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private Optional<String> field(String body, String name) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String fingerprint(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
