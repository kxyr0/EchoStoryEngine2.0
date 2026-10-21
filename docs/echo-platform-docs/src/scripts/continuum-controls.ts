const storagePrefix = "echo-continuum";

function root() {
  return document.documentElement;
}

function preferredTheme() {
  return window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
}

function savedTheme() {
  return localStorage.getItem(`${storagePrefix}-theme`) || root().dataset.theme || preferredTheme();
}

function applySavedPreferences(target: Document = document) {
  target.documentElement.dataset.theme = savedTheme();
  const savedFocus = localStorage.getItem(`${storagePrefix}-focus`);
  if (savedFocus === "true") {
    target.documentElement.dataset.focusMode = "true";
  } else {
    delete target.documentElement.dataset.focusMode;
  }
}

applySavedPreferences();

document.addEventListener("astro:before-swap", (event) => {
  const swapEvent = event as Event & { newDocument: Document };
  applySavedPreferences(swapEvent.newDocument);
});

document.addEventListener("astro:page-load", () => {
  applySavedPreferences();
});

function showLens(title: string, body: string) {
  const lens = document.querySelector<HTMLElement>("[data-context-lens]");
  const titleNode = lens?.querySelector<HTMLElement>("[data-lens-title]");
  const bodyNode = lens?.querySelector<HTMLElement>("[data-lens-body]");
  if (!lens || !titleNode || !bodyNode) {
    return;
  }
  titleNode.textContent = title;
  bodyNode.textContent = body;
  lens.hidden = false;
  lens.dataset.open = "true";
}

function hideLens() {
  const lens = document.querySelector<HTMLElement>("[data-context-lens]");
  if (!lens) {
    return;
  }
  lens.hidden = true;
  lens.dataset.open = "false";
}

document.addEventListener("click", async (event) => {
  const target = event.target instanceof Element ? event.target : null;
  if (!target) {
    return;
  }

  if (target.closest("[data-theme-toggle]")) {
    const next = root().dataset.theme === "dark" ? "light" : "dark";
    root().dataset.theme = next;
    localStorage.setItem(`${storagePrefix}-theme`, next);
  }

  if (target.closest("[data-focus-toggle]")) {
    const next = root().dataset.focusMode === "true" ? "false" : "true";
    root().dataset.focusMode = next;
    localStorage.setItem(`${storagePrefix}-focus`, next);
  }

  if (target.closest("[data-copy-link]")) {
    await navigator.clipboard?.writeText(location.href);
    showLens("Ссылка скопирована", "Текущий адрес страницы помещён в буфер обмена.");
  }

  if (target.closest("[data-page-map-toggle]")) {
    document.dispatchEvent(new CustomEvent("echo:toggle-page-map"));
  }

  if (target.closest("[data-lens-close]")) {
    hideLens();
  }

  const subject = target.closest<HTMLElement>("[data-lens-subject]");
  if (subject?.dataset.lensSubject) {
    showLens(subject.dataset.lensSubject, "Логический идентификатор связан с текущим документом и остаётся стабильным поверх runtime-состояния.");
  }

  const code = target.closest<HTMLElement>(".doc-article code");
  if (code) {
    showLens(code.textContent?.trim() || "Фрагмент", "Выбранный ID, команда или тип можно сверить с generated contracts и связанными исходниками этой страницы.");
  }

  const row = target.closest<HTMLTableRowElement>(".doc-article tbody tr");
  if (row) {
    const text = Array.from(row.cells).map((cell) => cell.textContent?.trim()).filter(Boolean).join(" · ");
    showLens("Строка контракта", text || "Выбранная строка таблицы.");
  }
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    hideLens();
  }
});

const readingPage = document.querySelector<HTMLElement>("[data-reading-page]");
if (readingPage) {
  const pageKey = `${storagePrefix}-last-page`;
  const progressKey = `${storagePrefix}-progress:${location.pathname}`;
  const savedProgress = Number(localStorage.getItem(progressKey) ?? "0");
  if (savedProgress > 160 && history.scrollRestoration !== "manual") {
    requestAnimationFrame(() => window.scrollTo({ top: savedProgress, behavior: "instant" as ScrollBehavior }));
  }

  let pending = 0;
  window.addEventListener("scroll", () => {
    if (pending) {
      return;
    }
    pending = window.setTimeout(() => {
      pending = 0;
      localStorage.setItem(pageKey, location.pathname);
      localStorage.setItem(progressKey, String(Math.round(window.scrollY)));
    }, 300);
  }, { passive: true });
}
