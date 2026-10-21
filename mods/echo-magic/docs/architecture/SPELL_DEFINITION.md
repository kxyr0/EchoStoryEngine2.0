# SpellDefinition

Binder переводит Init-only AST в immutable `SpellDefinition`. Revision — SHA-256 исходного `.es`. Catalog публикует полный валидный snapshot атомарно; broken candidate оставляет предыдущий snapshot.

Active cast хранит сам immutable definition, поэтому hot reload не меняет уже летящие projectile.

