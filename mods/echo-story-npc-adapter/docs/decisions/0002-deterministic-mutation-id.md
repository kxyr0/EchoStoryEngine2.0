# ADR 0002: Детерминированный Mutation ID

Статус: принято для N3.

Адаптер выводит `ActorMutationId` из `ActionExecutionId` через фиксированный
namespace и UUID-v5-style SHA-1 mapping. Правило не использует random, clock
или locale-sensitive данные.

Это позволяет Story start/recovery после рестарта отправлять или наблюдать одну
и ту же NPC mutation.
