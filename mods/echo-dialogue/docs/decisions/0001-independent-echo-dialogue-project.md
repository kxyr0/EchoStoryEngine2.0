# ADR 0001: Independent Echo Dialogue Project

Status: accepted for D1.

Echo Dialogue создаётся как отдельный Forge mod `echo_dialogue`, а не внутри Echo Story Engine.

Reason:

- D1 должен доказать standalone dialogue pipeline;
- Story integration относится к D2;
- зависимость на Echo Story Engine скрыла бы реальные границы Dialogue runtime.
