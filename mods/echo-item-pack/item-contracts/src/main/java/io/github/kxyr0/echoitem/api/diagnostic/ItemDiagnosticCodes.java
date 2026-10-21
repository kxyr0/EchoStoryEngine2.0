package io.github.kxyr0.echoitem.api.diagnostic;

/**
 * Stable diagnostic codes promised by Echo Item Pack.
 */
public final class ItemDiagnosticCodes {
    public static final String ITEM_SCRIPT_PARSE_ERROR = "ITEM_SCRIPT_PARSE_ERROR";
    public static final String ITEM_SCRIPT_BIND_ERROR = "ITEM_SCRIPT_BIND_ERROR";
    public static final String INVALID_ITEM_ID = "INVALID_ITEM_ID";
    public static final String DUPLICATE_ITEM_ID = "DUPLICATE_ITEM_ID";
    public static final String REGISTRY_ID_CONFLICT = "REGISTRY_ID_CONFLICT";
    public static final String ITEM_KIND_NOT_FOUND = "ITEM_KIND_NOT_FOUND";
    public static final String ITEM_KIND_CHANGE_REQUIRES_RESTART = "ITEM_KIND_CHANGE_REQUIRES_RESTART";
    public static final String NEW_ITEM_REQUIRES_RESTART = "NEW_ITEM_REQUIRES_RESTART";
    public static final String ITEM_SOURCE_REMOVED = "ITEM_SOURCE_REMOVED";
    public static final String ITEM_DEFINITION_UNAVAILABLE = "ITEM_DEFINITION_UNAVAILABLE";
    public static final String INVALID_ATTACK_DAMAGE = "INVALID_ATTACK_DAMAGE";
    public static final String INVALID_ATTACK_SPEED = "INVALID_ATTACK_SPEED";
    public static final String INVALID_DURABILITY = "INVALID_DURABILITY";
    public static final String INVALID_ATTRIBUTE = "INVALID_ATTRIBUTE";
    public static final String ATTRIBUTE_CONFLICT = "ATTRIBUTE_CONFLICT";
    public static final String TEXTURE_NOT_FOUND = "TEXTURE_NOT_FOUND";
    public static final String TEXTURE_DECODE_FAILED = "TEXTURE_DECODE_FAILED";
    public static final String MODEL_PARSE_FAILED = "MODEL_PARSE_FAILED";
    public static final String MODEL_REFERENCE_INVALID = "MODEL_REFERENCE_INVALID";
    public static final String LAST_KNOWN_GOOD_RETAINED = "LAST_KNOWN_GOOD_RETAINED";
    public static final String CLIENT_REVISION_GAP = "CLIENT_REVISION_GAP";
    public static final String MANIFEST_WRITE_FAILED = "MANIFEST_WRITE_FAILED";

    private ItemDiagnosticCodes() {
    }
}
