import { useEffect, useState } from "react";
import { recipesApi } from "../api.js";
import { DEFAULT_FILTERS, PAGE_SIZES, PREP_TIME_OPTIONS, SORT_OPTIONS } from "../filters.js";
import Pagination from "./Pagination.jsx";

export default function RecipeList({ categories, filters, setFilters, onOpen, onNew }) {
  const [searchText, setSearchText] = useState(filters.q);
  const [data, setData] = useState(null); // { content, page, size, totalElements, totalPages }
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retry, setRetry] = useState(0);

  // Iskanje se sproži šele 300 ms po zadnji črki, da ne kličemo strežnika ob vsaki tipki.
  useEffect(() => {
    const t = setTimeout(() => {
      const q = searchText.trim();
      if (q !== filters.q) setFilters((f) => ({ ...f, q, page: 0 }));
    }, 300);
    return () => clearTimeout(t);
  }, [searchText, filters.q, setFilters]);

  // Vsaka sprememba filtra/strani pošlje novo poizvedbo na strežnik.
  useEffect(() => {
    let cancelled = false;
    const [sortBy, direction] = filters.sort.split(":");
    setLoading(true);
    recipesApi
      .search({
        q: filters.q,
        categoryId: filters.categoryId,
        maxPrepTime: filters.maxPrepTime,
        sortBy,
        direction,
        page: filters.page,
        size: filters.size,
      })
      .then((result) => {
        if (cancelled) return;
        // Če smo izbrisali zadnji recept na strani, se vrnemo na zadnjo obstoječo stran.
        if (result.content.length === 0 && result.page > 0 && result.totalPages > 0) {
          setFilters((f) => ({ ...f, page: result.totalPages - 1 }));
          return;
        }
        setData(result);
        setError(null);
        setLoading(false);
      })
      .catch((e) => {
        if (cancelled) return;
        setError(e.message);
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters, retry, setFilters]);

  const change = (patch) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const filtersActive = filters.q || filters.categoryId || filters.maxPrepTime;

  function reset() {
    setSearchText("");
    setFilters((f) => ({ ...DEFAULT_FILTERS, sort: f.sort, size: f.size }));
  }

  const recipes = data?.content ?? [];

  return (
    <section>
      <div className="toolbar">
        <input
          type="search"
          placeholder="Išči po naslovu, opisu ali sestavinah"
          aria-label="Iskanje"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
        />
        <select aria-label="Kategorija" value={filters.categoryId} onChange={(e) => change({ categoryId: e.target.value })}>
          <option value="">Vse kategorije</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select aria-label="Čas priprave" value={filters.maxPrepTime} onChange={(e) => change({ maxPrepTime: e.target.value })}>
          {PREP_TIME_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <select aria-label="Razvrščanje" value={filters.sort} onChange={(e) => change({ sort: e.target.value })}>
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div className="banner" role="alert">
          {error}{" "}
          <button className="btn small" onClick={() => setRetry((n) => n + 1)}>
            Poskusi znova
          </button>
        </div>
      )}

      {data && (
        <div className="count-row">
          <p className="count" data-testid="count">
            {data.totalElements === 0
              ? "Ni zadetkov"
              : "Zadetkov: " + data.totalElements + " · stran " + (data.page + 1) + " od " + data.totalPages}
          </p>
          <div className="count-tools">
            {filtersActive && (
              <button className="btn small" onClick={reset}>
                Počisti filtre
              </button>
            )}
            <label className="inline-label">
              Na stran
              <select
                aria-label="Receptov na stran"
                value={filters.size}
                onChange={(e) => change({ size: Number(e.target.value) })}
              >
                {PAGE_SIZES.map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
      )}

      {loading && !data && <p className="muted">Nalagam…</p>}

      {data && recipes.length === 0 && !loading && (
        <div className="empty">
          <p>{filtersActive ? "Noben recept ne ustreza iskanju." : "Ni še nobenega recepta."}</p>
          {!filtersActive && (
            <button className="btn" onClick={onNew}>
              Dodaj prvi recept
            </button>
          )}
        </div>
      )}

      <div className={"grid" + (loading && data ? " is-loading" : "")}>
        {recipes.map((r) => (
          <article key={r.id} className="card" data-testid="recipe-card">
            <h3>{r.title}</h3>
            <div className="meta">
              {r.category && <span className="chip">{r.category.name}</span>}
              <span>{r.prepTimeMinutes} min</span>
              <span>·</span>
              <span>{r.servings} {r.servings === 1 ? "porcija" : "porcij"}</span>
            </div>
            {r.description && <p className="desc">{r.description}</p>}
            <div className="card-actions">
              <button className="btn" onClick={() => onOpen(r.id)}>
                Odpri
              </button>
            </div>
          </article>
        ))}
      </div>

      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => setFilters((f) => ({ ...f, page: p }))} />}
    </section>
  );
}
