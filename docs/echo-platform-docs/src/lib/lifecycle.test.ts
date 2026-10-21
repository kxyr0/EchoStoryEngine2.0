import { describe, expect, it } from "vitest";
import { nextPhase, npcLifecycle, restartRecovery } from "./lifecycle";

describe("lifecycle reducers", () => {
  it("moves ENTER to MAIN without skipping the persisted revision", () => {
    expect(nextPhase({ phase: "ENTER", cursorMs: 320, stopRequested: false, persistedRevision: 14 })).toMatchObject({
      phase: "MAIN",
      cursorMs: 0,
      persistedRevision: 15,
    });
  });

  it("does not remove subjects during chunk unload", () => {
    expect(npcLifecycle("DORMANT_CHUNK_UNLOADED")).toEqual({
      actor: true,
      entity: false,
      subject: true,
      playback: true,
      subscriptions: false,
    });
  });

  it("recovers mid-main without replaying ENTER", () => {
    expect(restartRecovery("mid-main")).toContain("не проигрывать ENTER повторно");
  });
});
