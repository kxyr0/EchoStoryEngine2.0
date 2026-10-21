export type AnimationPhase = "ENTER" | "MAIN" | "EXIT" | "TARGET";
export type RestartMoment = "mid-enter" | "mid-main" | "stop-requested" | "safe-exit" | "mid-exit" | "terminal";

export interface LifecycleState {
  phase: AnimationPhase;
  cursorMs: number;
  stopRequested: boolean;
  persistedRevision: number;
}

export interface NpcLifecycleState {
  actor: boolean;
  entity: boolean;
  subject: boolean;
  playback: boolean;
  subscriptions: boolean;
}

export function nextPhase(state: LifecycleState): LifecycleState {
  if (state.phase === "ENTER") {
    return { ...state, phase: "MAIN", cursorMs: 0, persistedRevision: state.persistedRevision + 1 };
  }
  if (state.phase === "MAIN" && state.stopRequested) {
    return { ...state, phase: "EXIT", cursorMs: 0, persistedRevision: state.persistedRevision + 1 };
  }
  if (state.phase === "EXIT") {
    return { ...state, phase: "TARGET", cursorMs: 0, persistedRevision: state.persistedRevision + 1 };
  }
  return { ...state, cursorMs: state.cursorMs + 100 };
}

export function restartRecovery(moment: RestartMoment): string[] {
  const common = ["загрузить subject record", "восстановить leases", "опубликовать полный snapshot"];
  if (moment === "terminal") {
    return ["загрузить terminal record", "не перезапускать playback", "сохранить результат для аудита"];
  }
  if (moment === "stop-requested" || moment === "safe-exit") {
    return [...common, "продолжить с проверки EXIT", "сохранить идемпотентность команды"];
  }
  return [...common, "вернуть сохранённый phase cursor", "не проигрывать ENTER повторно"];
}

export function npcLifecycle(mode: string): NpcLifecycleState {
  const states: Record<string, NpcLifecycleState> = {
    ACTIVE: { actor: true, entity: true, subject: true, playback: true, subscriptions: true },
    DORMANT_CHUNK_UNLOADED: { actor: true, entity: false, subject: true, playback: true, subscriptions: false },
    REMATERIALIZED: { actor: true, entity: true, subject: true, playback: true, subscriptions: true },
    ABSENT: { actor: false, entity: false, subject: false, playback: false, subscriptions: false },
    UNBOUND: { actor: true, entity: true, subject: false, playback: false, subscriptions: false },
  };
  return states[mode] ?? states.ACTIVE;
}
