package io.github.kxyr0.echocore.core;

import io.github.kxyr0.echocore.api.ChapterResult.Code;
import java.io.IOException;

public final class ChapterFailure extends IOException {
    private final Code code;
    public ChapterFailure(Code code, String message) { super(message); this.code = code; }
    public Code code() { return code; }
}
