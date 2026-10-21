import { useMemo, useState } from "preact/hooks";

export function PlayerProfileExplorer() {
  const [nameMode, setNameMode] = useState("ACCOUNT");
  const [customName, setCustomName] = useState("Искра");
  const [spark, setSpark] = useState(100);
  const [maximum, setMaximum] = useState(100);
  const [skinMode, setSkinMode] = useState("ACCOUNT");
  const [model, setModel] = useState("CLASSIC");
  const snapshot = useMemo(() => ({
    playerId: "EchoPlayerId(accountUuid)",
    accountName: "PlayerAccount",
    resolvedDisplayName: nameMode === "ACCOUNT" ? "PlayerAccount" : customName.trim(),
    nameMode,
    spark: { current: Math.min(spark, maximum), maximum },
    appearance: { skinMode, skinId: skinMode === "RESOURCE" ? "jdh:player/default" : "", model },
    templateId: "jdh:player/default",
    profileRevision: 1,
  }), [customName, maximum, model, nameMode, skinMode, spark]);
  return (
    <section class="interactive-panel" aria-labelledby="player-profile-explorer">
      <div class="stage-panel-copy">
        <p class="eyebrow">snapshot</p>
        <h3 id="player-profile-explorer">PlayerProfileExplorer</h3>
      </div>
      <label>Имя <select value={nameMode} onInput={(event) => setNameMode(event.currentTarget.value)}>
        <option>ACCOUNT</option><option>CUSTOM</option>
      </select></label>
      <label>Custom <input value={customName} onInput={(event) => setCustomName(event.currentTarget.value)} /></label>
      <label>Искра <input type="number" min="0" max={maximum} value={spark} onInput={(event) => setSpark(Number(event.currentTarget.value))} /></label>
      <label>Максимум <input type="number" min="1" max="1000000" value={maximum} onInput={(event) => setMaximum(Number(event.currentTarget.value))} /></label>
      <label>Скин <select value={skinMode} onInput={(event) => setSkinMode(event.currentTarget.value)}>
        <option>ACCOUNT</option><option>RESOURCE</option>
      </select></label>
      <label>Модель <select value={model} onInput={(event) => setModel(event.currentTarget.value)}>
        <option>CLASSIC</option><option>SLIM</option>
      </select></label>
      <pre>{JSON.stringify(snapshot, null, 2)}</pre>
    </section>
  );
}

export function SparkMutationLab() {
  const [current, setCurrent] = useState(80);
  const [maximum, setMaximum] = useState(100);
  const [amount, setAmount] = useState(30);
  const spend = () => current >= amount && setCurrent(current - amount);
  const restore = () => setCurrent(Math.min(current + amount, maximum));
  const clampMaximum = () => {
    const next = Math.max(1, amount);
    setMaximum(next);
    setCurrent(Math.min(current, next));
  };
  const percentageMaximum = () => {
    const next = Math.max(1, amount);
    setCurrent(Math.round((current / maximum) * next));
    setMaximum(next);
  };
  return (
    <section class="interactive-panel" aria-labelledby="spark-mutation-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">Искра</p>
        <h3 id="spark-mutation-lab">SparkMutationLab</h3>
      </div>
      <label>Current <input type="number" min="0" max={maximum} value={current} onInput={(event) => setCurrent(Number(event.currentTarget.value))} /></label>
      <label>Maximum <input type="number" min="1" value={maximum} onInput={(event) => setMaximum(Number(event.currentTarget.value))} /></label>
      <label>Amount <input type="number" min="0" value={amount} onInput={(event) => setAmount(Number(event.currentTarget.value))} /></label>
      <button type="button" onClick={spend}>Spend</button>
      <button type="button" onClick={restore}>Restore</button>
      <button type="button" onClick={clampMaximum}>CLAMP_CURRENT</button>
      <button type="button" onClick={percentageMaximum}>PRESERVE_PERCENTAGE</button>
      <p>{current}/{maximum} · {current >= amount ? "SUCCESS" : "INSUFFICIENT_SPARK"}</p>
    </section>
  );
}

export function PlayerRecoveryLab() {
  const [step, setStep] = useState(0);
  const steps = ["first join", "logout", "respawn", "restart", "reconnect"];
  return (
    <section class="interactive-panel" aria-labelledby="player-recovery-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">recovery</p>
        <h3 id="player-recovery-lab">PlayerRecoveryLab</h3>
      </div>
      <input type="range" min="0" max={steps.length - 1} value={step} onInput={(event) => setStep(Number(event.currentTarget.value))} />
      <p>{steps[step]}: EchoPlayerId сохраняется, profile revision не откатывается.</p>
    </section>
  );
}

export function SkinResolutionLab() {
  const [source, setSource] = useState("external");
  const result = source === "external" ? "external valid PNG"
    : source === "packaged" ? "packaged default"
      : source === "previous" ? "previous known-good"
        : "account fallback";
  return (
    <section class="interactive-panel" aria-labelledby="skin-resolution-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">skin</p>
        <h3 id="skin-resolution-lab">SkinResolutionLab</h3>
      </div>
      <select value={source} onInput={(event) => setSource(event.currentTarget.value)}>
        <option value="external">external</option>
        <option value="packaged">packaged</option>
        <option value="previous">broken PNG</option>
        <option value="account">no custom sources</option>
      </select>
      <p>{result}</p>
    </section>
  );
}
