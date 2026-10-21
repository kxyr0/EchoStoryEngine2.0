import { useState } from "preact/hooks";

const bones = [
  { id: "body", parent: "root", pivot: "0,24,0", geometry: "торс и слой куртки" },
  { id: "head", parent: "body", pivot: "0,24,0", geometry: "куб головы и overlay face rig" },
  { id: "left_arm", parent: "body", pivot: "5,22,0", geometry: "плечо, предплечье и ладонь" },
  { id: "right_arm", parent: "body", pivot: "-5,22,0", geometry: "плечо, предплечье и ладонь" },
  { id: "left_leg", parent: "body", pivot: "2,12,0", geometry: "бедро и голень" },
  { id: "right_leg", parent: "body", pivot: "-2,12,0", geometry: "бедро и голень" },
];

export function BoneRigExplorer() {
  const [selected, setSelected] = useState(bones[0]);
  return (
    <section class="interactive-panel bone-rig-explorer" aria-labelledby="bone-title" data-spine-label="Проверка костей">
      <div class="stage-panel-copy">
        <p class="eyebrow">rig и кости</p>
        <h3 id="bone-title">Проверка костей humanoid</h3>
      </div>
      <div class="segmented">
        {bones.map((bone) => (
          <button type="button" aria-pressed={selected.id === bone.id} onClick={() => setSelected(bone)}>
            {bone.id}
          </button>
        ))}
      </div>
      <p>parent=<code>{selected.parent}</code> pivot=<code>{selected.pivot}</code></p>
      <p>Затронутая геометрия: {selected.geometry}. Ладони, ступни и рот проверяются как отдельные UV-зоны.</p>
    </section>
  );
}
