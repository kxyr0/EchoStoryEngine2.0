package io.github.kxyr0.echoanimationauthoring.draft;

import java.lang.reflect.InvocationTargetException;

final class EchoAnimationCompilerProbe {
    private static final String COMPILER = "io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler";

    ValidationResult validate(String sourceId, String sourceText) {
        try {
            Object compiler = Class.forName(COMPILER).getConstructor().newInstance();
            Object result = compiler.getClass().getMethod("compile", String.class, String.class)
                    .invoke(compiler, sourceId, sourceText);
            boolean successful = (boolean) result.getClass().getMethod("successful").invoke(result);
            Object diagnostics = result.getClass().getMethod("diagnostics").invoke(result);
            return new ValidationResult(successful, successful, successful, String.valueOf(diagnostics));
        } catch (ClassNotFoundException exception) {
            return new ValidationResult(false, false, false, "Echo Animation compiler class is unavailable.");
        } catch (ReflectiveOperationException exception) {
            return reflectedFailure(exception);
        }
    }

    private ValidationResult reflectedFailure(ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException target
                ? target.getTargetException()
                : exception;
        return new ValidationResult(false, false, false, cause.getMessage());
    }

    record ValidationResult(boolean parserPass, boolean binderPass, boolean compilerPass, String message) {
        boolean passed() {
            return parserPass && binderPass && compilerPass;
        }
    }
}
