import { useMemo, useState } from "preact/hooks";

function selection(enabled: boolean, id: string) {
  return enabled ? id : "-";
}

export function OutfitResolverLab() {
  const [glasses, setGlasses] = useState(true);
  const [robe, setRobe] = useState(true);
  const [pants, setPants] = useState("jdh:origin_john_pants");
  const [boots, setBoots] = useState(true);
  const key = useMemo(() => [
    "jdh:john",
    `FACE=${selection(glasses, "jdh:origin_john_glasses")}`,
    `TOP=${selection(robe, "jdh:origin_john_robe")}`,
    `BOTTOM=${pants}`,
    `FEET=${selection(boots, "jdh:origin_john_boots")}`,
  ].join("|"), [glasses, robe, pants, boots]);
  const exact = glasses && robe && pants === "jdh:origin_john_pants" && boots;
  const empty = !glasses && !robe && pants === "-" && !boots;
  const result = exact
    ? "jdh:echo_wardrobe/skins/origin/jdh_full_dressed"
    : empty
      ? "echo_content_player:player/base"
      : "profile base fallback + WARDROBE_OUTFIT_VARIANT_NOT_FOUND";
  return (
    <section class="interactive-panel" aria-labelledby="outfit-resolver-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">exact lookup</p>
        <h3 id="outfit-resolver-lab">OutfitResolverLab</h3>
      </div>
      <label><input type="checkbox" checked={glasses} onInput={() => setGlasses(!glasses)} /> FACE glasses</label>
      <label><input type="checkbox" checked={robe} onInput={() => setRobe(!robe)} /> TOP robe</label>
      <label>
        BOTTOM
        <select value={pants} onInput={(event) => setPants(event.currentTarget.value)}>
          <option value="-">empty</option>
          <option value="jdh:origin_john_pants">origin pants</option>
          <option value="jdh:classic_john_pants">classic pants</option>
        </select>
      </label>
      <label><input type="checkbox" checked={boots} onInput={() => setBoots(!boots)} /> FEET boots</label>
      <pre>{key}</pre>
      <pre>{result}</pre>
    </section>
  );
}

export function GlassesPortraitLab() {
  const [fullOutfit, setFullOutfit] = useState(true);
  const faceSkin = fullOutfit
    ? "jdh:echo_wardrobe/skins/origin/jdh_full_dressed"
    : "echo_content_player:player/base";
  const model = fullOutfit ? "jdh:item/origin_john_glasses" : "none";
  return (
    <section class="interactive-panel" aria-labelledby="glasses-portrait-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">portrait</p>
        <h3 id="glasses-portrait-lab">GlassesPortraitLab</h3>
      </div>
      <label>
        <input type="checkbox" checked={fullOutfit} onInput={() => setFullOutfit(!fullOutfit)} />
        exact outfit includes glasses
      </label>
      <pre>faceSkin: {faceSkin}</pre>
      <pre>model: {model}</pre>
    </section>
  );
}

export function OutfitCoverageLab() {
  const [classic, setClassic] = useState(true);
  const faces = classic ? 3 : 2;
  const tops = classic ? 3 : 2;
  const bottoms = classic ? 4 : 2;
  const feet = classic ? 3 : 2;
  const expected = faces * tops * bottoms * feet;
  const mapped = 2;
  return (
    <section class="interactive-panel" aria-labelledby="outfit-coverage-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">coverage</p>
        <h3 id="outfit-coverage-lab">OutfitCoverageLab</h3>
      </div>
      <label>
        <input type="checkbox" checked={classic} onInput={() => setClassic(!classic)} />
        include classic wearables
      </label>
      <pre>{faces} FACE x {tops} TOP x {bottoms} BOTTOM x {feet} FEET = {expected}</pre>
      <pre>{mapped} mapped, {expected - mapped} missing</pre>
    </section>
  );
}
