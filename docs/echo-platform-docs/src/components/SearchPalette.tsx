import { useEffect, useRef, useState } from "preact/hooks";

interface SearchRecord {
  type: string;
  title: string;
  section: string;
  url: string;
  context: string;
}

interface PagefindResult {
  data(): Promise<{ url: string; meta: { title?: string }; excerpt: string }>;
}

interface PagefindModule {
  search(query: string): Promise<{ results: PagefindResult[] }>;
}

const groups = ["Документы", "Команды", "ID", "EchoScript", "Ошибки", "API", "Файлы", "Решения"];

function cleanExcerpt(value: string) {
  return value.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
}

function displayType(record: SearchRecord) {
  if (record.type.toLowerCase().includes("command")) {
    return "Команда";
  }
  if (record.context.includes("EANIM")) {
    return "Ошибка";
  }
  if (record.context.includes("ActorId") || record.context.includes("AnimationSubjectId")) {
    return "ID";
  }
  return "Документ";
}

export function SearchPalette() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchRecord[]>([]);
  const input = useRef<HTMLInputElement>(null);
  const trigger = useRef<Element | null>(null);

  useEffect(() => {
    document.documentElement.dataset.searchReady = "true";
    const openSearch = (event?: Event) => {
      trigger.current = event?.target instanceof Element ? event.target : document.activeElement;
      setOpen(true);
    };
    const click = (event: MouseEvent) => {
      const target = event.target instanceof Element ? event.target : null;
      if (target?.closest("[data-open-search]")) {
        openSearch(event);
      }
    };
    const key = (event: KeyboardEvent) => {
      const target = event.target instanceof HTMLElement ? event.target : null;
      const editing = target?.matches("input, textarea, select, [contenteditable='true']");
      if ((event.ctrlKey && event.key.toLowerCase() === "k") || (event.key === "/" && !editing)) {
        event.preventDefault();
        openSearch(event);
      }
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    document.addEventListener("click", click);
    document.addEventListener("keydown", key);
    return () => {
      document.removeEventListener("click", click);
      document.removeEventListener("keydown", key);
    };
  }, []);

  useEffect(() => {
    if (open) {
      queueMicrotask(() => input.current?.focus());
    } else if (trigger.current instanceof HTMLElement) {
      trigger.current.focus();
    }
  }, [open]);

  useEffect(() => {
    let cancelled = false;
    const applyFallback = async () => {
      const fallback = (await fetch("/search-index.json").then((response) => response.json())) as SearchRecord[];
      const filtered = fallback.filter((record) =>
        `${record.title} ${record.type} ${record.context}`.toLowerCase().includes(query.toLowerCase()),
      );
      if (!cancelled) {
        setResults(filtered.slice(0, 9));
      }
    };
    async function search() {
      if (!query.trim()) {
        setResults([]);
        return;
      }
      await applyFallback();
      try {
        const pagefindUrl = "/pagefind/pagefind.js";
        const pagefind = (await import(/* @vite-ignore */ pagefindUrl)) as PagefindModule;
        const pagefindResults = await pagefind.search(query);
        const data = await Promise.all(pagefindResults.results.slice(0, 9).map((result) => result.data()));
        if (!cancelled) {
          setResults(data.map((record) => ({
            type: "Документ",
            title: record.meta.title ?? record.url,
            section: "Pagefind",
            url: record.url,
            context: cleanExcerpt(record.excerpt),
          })));
        }
        return;
      } catch {
        // During dev the generated Pagefind bundle may not exist yet; fallback results are already visible.
      }
    }
    search();
    return () => {
      cancelled = true;
    };
  }, [query]);

  if (!open) {
    return null;
  }

  return (
    <div class="search-overlay" role="presentation" onClick={() => setOpen(false)}>
      <section
        class="search-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="Поиск по атласу"
        onClick={(event) => event.stopPropagation()}
      >
        <div class="search-context" aria-hidden="true">
          <span>Ctrl K</span>
          <span>/</span>
          <span>Esc закрывает</span>
        </div>
        <input
          ref={input}
          value={query}
          onInput={(event) => setQuery(event.currentTarget.value)}
          placeholder="Найти страницу, ID, команду, ошибку или API"
          aria-label="Поисковый запрос"
        />
        <div class="search-groups" aria-label="Группы поиска">
          {groups.map((group) => <span>{group}</span>)}
        </div>
        <div aria-live="polite">
          {results.map((record) => (
            <a class="search-result" href={record.url}>
              <span aria-hidden="true">{displayType(record).slice(0, 1)}</span>
              <span>
                <strong>{record.title}</strong>
                <span>{displayType(record)} · {record.context}</span>
              </span>
              <code>{record.url}</code>
            </a>
          ))}
        </div>
      </section>
    </div>
  );
}
