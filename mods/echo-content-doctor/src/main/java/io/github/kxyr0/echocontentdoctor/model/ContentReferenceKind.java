package io.github.kxyr0.echocontentdoctor.model;

public enum ContentReferenceKind {
    STORY(ContentResourceKind.STORY),
    SCENE(ContentResourceKind.SCENE),
    DIALOGUE(ContentResourceKind.DIALOGUE),
    NPC(ContentResourceKind.NPC),
    PLAYER(ContentResourceKind.PLAYER),
    LOCATION(ContentResourceKind.LOCATION),
    CAMERA(ContentResourceKind.CAMERA),
    CUTSCENE(ContentResourceKind.CUTSCENE),
    ANIMATION(ContentResourceKind.ANIMATION),
    ANIMATION_CLIP(ContentResourceKind.ANIMATION_CLIP),
    ANIMATION_RIG(ContentResourceKind.ANIMATION_RIG),
    ANIMATION_PROFILE(ContentResourceKind.ANIMATION_PROFILE),
    FACE_ANIMATION(ContentResourceKind.FACE_ANIMATION),
    FACE_POSE(ContentResourceKind.FACE_POSE),
    FACE_RIG(ContentResourceKind.FACE_RIG),
    VOICE(ContentResourceKind.VOICE),
    AUDIO(ContentResourceKind.AUDIO),
    TRIGGER(ContentResourceKind.TRIGGER),
    OBSERVATION(ContentResourceKind.OBSERVATION),
    EFFECT(ContentResourceKind.EFFECT),
    ITEM(ContentResourceKind.ITEM),
    ITEM_INSPECTION(ContentResourceKind.ITEM_INSPECTION),
    SCENARIO(ContentResourceKind.SCENARIO),
    MIGRATION(ContentResourceKind.MIGRATION);

    private final ContentResourceKind targetKind;

    ContentReferenceKind(ContentResourceKind targetKind) {
        this.targetKind = targetKind;
    }

    public ContentResourceKind targetKind() {
        return targetKind;
    }
}
