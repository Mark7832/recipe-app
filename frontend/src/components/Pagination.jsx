// Seznam strani za prikaz: vedno prva, zadnja in okolica trenutne ("…" vmes).
function pageList(current, total) {
  const pages = new Set([0, total - 1, current - 1, current, current + 1]);
  const sorted = [...pages].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
  const result = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push("gap-" + p);
    result.push(p);
  });
  return result;
}

export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Strani">
      <button className="btn small" onClick={() => onChange(page - 1)} disabled={page === 0}>
        ← Prejšnja
      </button>
      {pageList(page, totalPages).map((p) =>
        typeof p === "string" ? (
          <span key={p} className="muted" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={p}
            className={"btn small" + (p === page ? " primary" : "")}
            onClick={() => onChange(p)}
            aria-current={p === page ? "page" : undefined}
            aria-label={"Stran " + (p + 1)}
          >
            {p + 1}
          </button>
        )
      )}
      <button className="btn small" onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1}>
        Naslednja →
      </button>
    </nav>
  );
}
