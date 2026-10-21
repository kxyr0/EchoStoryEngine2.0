import { useEffect, useMemo, useState } from "preact/hooks";

interface SpinePoint {
  id: string;
  label: string;
  kind: "section" | "lab" | "table";
}

function makeId(element: Element, index: number) {
  if (element.id) {
    return element.id;
  }
  const text = element.textContent?.trim().toLowerCase() || `section-${index + 1}`;
  const id = text
    .replace(/[^\p{L}\p{N}]+/gu, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 48) || `section-${index + 1}`;
  element.id = id;
  return id;
}

function labelFor(element: Element) {
  if (element instanceof HTMLElement && element.dataset.spineLabel) {
    return element.dataset.spineLabel;
  }
  const title = element.querySelector("h2,h3,caption")?.textContent ?? element.textContent;
  return title?.trim().replace(/\s+/g, " ").slice(0, 72) || "Раздел";
}

export function ProgressSpine() {
  const [points, setPoints] = useState<SpinePoint[]>([]);
  const [active, setActive] = useState("");
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const article = document.querySelector(".doc-article");
    if (!article) {
      return;
    }
    const elements = Array.from(article.querySelectorAll("h2, .interactive-panel, .contract-grid, table"));
    const nextPoints = elements.slice(0, 18).map((element, index) => ({
      id: makeId(element, index),
      label: labelFor(element),
      kind: element.matches(".interactive-panel") ? "lab" : element.matches("table,.contract-grid") ? "table" : "section",
    })) as SpinePoint[];
    setPoints(nextPoints);
    setActive(nextPoints[0]?.id ?? "");

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (visible?.target.id) {
          setActive(visible.target.id);
        }
      },
      { rootMargin: "-18% 0px -64% 0px", threshold: [0.08, 0.2, 0.45] },
    );
    nextPoints.forEach((point) => {
      const element = document.getElementById(point.id);
      if (element) {
        observer.observe(element);
      }
    });
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const toggle = () => setOpen((value) => !value);
    document.addEventListener("echo:toggle-page-map", toggle);
    return () => document.removeEventListener("echo:toggle-page-map", toggle);
  }, []);

  const currentIndex = useMemo(() => Math.max(0, points.findIndex((point) => point.id === active)), [active, points]);
  const progress = points.length > 1 ? `${Math.round((currentIndex / (points.length - 1)) * 100)}%` : "0%";

  if (points.length === 0) {
    return null;
  }

  return (
    <aside class={`progress-spine ${open ? "is-open" : ""}`} aria-label="Карта страницы">
      <div class="spine-rail" aria-hidden="true">
        <span style={{ height: progress }} />
      </div>
      <nav class="spine-points">
        {points.map((point) => (
          <a class={`spine-point spine-${point.kind}`} href={`#${point.id}`} aria-current={active === point.id ? "location" : undefined}>
            <span class="spine-dot" aria-hidden="true" />
            <span class="spine-tip">{point.label}</span>
          </a>
        ))}
      </nav>
    </aside>
  );
}
