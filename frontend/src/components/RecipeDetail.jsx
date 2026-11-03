import { useEffect, useState } from "react";
import { recipesApi } from "../api.js";

export default function RecipeDetail({ id, onBack, onEdit, onDeleted }) {
  const [recipe, setRecipe] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setRecipe(null);
    setError(null);
    // READ (en zapis): GET /api/recipes/{id}
    recipesApi
      .get(id)
      .then((r) => !cancelled && setRecipe(r))
      .catch((e) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function remove() {
    if (!window.confirm("Izbrišem recept »" + recipe.title + "«?")) return;
    setBusy(true);
    try {
      await recipesApi.remove(recipe.id);
      await onDeleted();
    } catch (e) {
      if (e.status === 404) {
        await onDeleted();
      } else {
        setError(e.message);
        setBusy(false);
      }
    }
  }

  return (
    <section className="detail">
      <button className="btn small" onClick={onBack}>
        ← Nazaj na seznam
      </button>

      {error && (
        <div className="banner" role="alert">
          {error}
        </div>
      )}
      {!recipe && !error && <p className="muted">Nalagam…</p>}

      {recipe && (
        <article className="detail-card">
          <h2>{recipe.title}</h2>
          <div className="meta">
            {recipe.category && <span className="chip">{recipe.category.name}</span>}
            <span>{recipe.prepTimeMinutes} min</span>
            <span>·</span>
            <span>{recipe.servings} {recipe.servings === 1 ? "porcija" : "porcij"}</span>
          </div>

          {recipe.description && <p className="desc">{recipe.description}</p>}

          <h3>Sestavine</h3>
          {recipe.ingredients.length === 0 ? (
            <p className="muted">Ni vpisanih sestavin.</p>
          ) : (
            <ul className="ingredients">
              {recipe.ingredients.map((i) => (
                <li key={i.id}>
                  <span className="qty">{[i.quantity, i.unit].filter(Boolean).join(" ")}</span> {i.name}
                </li>
              ))}
            </ul>
          )}

          <h3>Postopek</h3>
          <p className="instructions">{recipe.instructions || "Ni vpisanega postopka."}</p>

          <div className="card-actions">
            <button className="btn" onClick={() => onEdit(recipe)} disabled={busy}>
              Uredi
            </button>
            <button className="btn danger" onClick={remove} disabled={busy}>
              Izbriši
            </button>
          </div>
        </article>
      )}
    </section>
  );
}
