package io.github.kxyr0.echowardrobe.core.resolution;

import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;

/**
 * Generates an author-facing skeleton for a missing exact combination.
 */
public final class MissingOutfitSkeleton {
    public String source(OutfitKey key) {
        return """
                class GeneratedOutfit : OutfitVariantResource
                {
                    private void Init()
                    {
                        Id("%s/todo_name");
                        Profile("%s");

                        Match(
                            face: "%s",
                            top: "%s",
                            bottom: "%s",
                            feet: "%s"
                        );

                        Skin("%s");
                    }
                }
                """.formatted(key.profileId(), key.profileId(), key.face().serialize(), key.top().serialize(),
                key.bottom().serialize(), key.feet().serialize(), "TODO");
    }
}
