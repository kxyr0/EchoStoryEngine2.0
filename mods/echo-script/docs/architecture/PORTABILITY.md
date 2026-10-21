# Portability

EchoScript uses plain Java 17 source compatibility and Gradle. It is not a mod and has no loader lifecycle.

Public source identity is logical and platform-neutral. The public API does not expose `Path`, `File`, Minecraft
resource identifiers, or logging framework types.

Line and column positions are one-based. Offsets are zero-based UTF-16 code-unit offsets into decoded source text.
LF and CRLF are both supported and tested.
