# ADR 0006: Initial Linear-Only Model

Status: accepted for D1.

D1 поддерживает только ordered `Line(...)` list внутри start node.

Reason:

- choices, branches and conditions требуют отдельной UX, network и validation модели;
- первая веха должна доказать full server-authoritative linear flow;
- unsupported features должны явно диагностироваться.
