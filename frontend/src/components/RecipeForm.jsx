import { useState } from "react";
import { recipesApi } from "../api.js";

const emptyIngredient = () => ({ name: "", quantity: "", unit: "" });

export default function RecipeForm({ recipe, categories, onCancel, onSaved }) {
  const isNew = !recipe;

  const [title, setTitle] = useState(recipe?.title ?? "");
  const [description, setDescription] = useState(recipe?.description ?? "");
  const [instructions, setInstructions] = useState(recipe?.instructions ?? "");
  const [prepTimeMinutes, setPrepTimeMinutes] = useState(recipe?.prepTimeMinutes ?? 30);
  const [servings, setServings] = useState(recipe?.servings ?? 4);
  const [categoryId, setCategoryId] = useState(recipe?.category ? String(recipe.category.id) : "");
  const [ingredients, setIngredients] = useState(
    recipe?.ingredients?.length
      ? recipe.ingredients.map((i) => ({ name: i.name, quantity: i.quantity ?? "", unit: i.unit ?? "" }))
      : [emptyIngredient()]
  );
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  function updateIngredient(index, field, value) {
    setIngredients((list) => list.map((item, i) => (i === index ? { ...item, [field]: value } : item)));
  }

  function removeIngredient(index) {
    setIngredients((list) => list.filter((_, i) => i !== index));
  }

  async function submit(e) {
    e.preventDefault();

    const prep = parseInt(prepTimeMinutes, 10);
    const serv = parseInt(servings, 10);
    if (!title.trim()) return setError("Naslov je obvezen.");
    if (!Number.isInteger(prep) || prep < 1) return setError("Čas priprave mora biti vsaj 1 minuta.");
    if (!Number.isInteger(serv) || serv < 1) return setError("Število porcij mora biti vsaj 1.");

    const payload = {
      title: title.trim(),
      description: description.trim(),
      instructions: instructions.trim(),
      prepTimeMinutes: prep,
      servings: serv,
      categoryId: categoryId ? Number(categoryId) : null,
      ingredients: ingredients
        .filter((i) => i.name.trim())
        .map((i) => ({ name: i.name.trim(), quantity: i.quantity.trim(), unit: i.unit.trim() })),
    };

    setSaving(true);
    setError(null);
    try {
      // CREATE: POST /api/recipes, UPDATE: PUT /api/recipes/{id}
      const saved = isNew ? await recipesApi.create(payload) : await recipesApi.update(recipe.id, payload);
      await onSaved(saved, isNew);
    } catch (err) {
      setError("Shranjevanje ni uspelo: " + err.message);
      setSaving(false);
    }
  }

  return (
    <form className="form-card" onSubmit={submit} noValidate>
      <h2>{isNew ? "Nov recept" : "Uredi recept"}</h2>

      <label>
        Naslov
        <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={255} required />
      </label>

      <div className="row">
        <label>
          Kategorija
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">(brez kategorije)</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Čas priprave (min)
          <input type="number" min="1" value={prepTimeMinutes} onChange={(e) => setPrepTimeMinutes(e.target.value)} />
        </label>
        <label>
          Porcije
          <input type="number" min="1" value={servings} onChange={(e) => setServings(e.target.value)} />
        </label>
      </div>

      <label>
        Opis
        <textarea rows={2} maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)} />
      </label>

      <fieldset>
        <legend>Sestavine</legend>
        {ingredients.map((ing, index) => (
          <div className="ingredient-row" key={index}>
            <input
              placeholder="Količina"
              aria-label={"Količina " + (index + 1)}
              value={ing.quantity}
              onChange={(e) => updateIngredient(index, "quantity", e.target.value)}
            />
            <input
              placeholder="Enota"
              aria-label={"Enota " + (index + 1)}
              value={ing.unit}
              onChange={(e) => updateIngredient(index, "unit", e.target.value)}
            />
            <input
              placeholder="Sestavina"
              aria-label={"Sestavina " + (index + 1)}
              value={ing.name}
              onChange={(e) => updateIngredient(index, "name", e.target.value)}
            />
            <button type="button" className="btn small" onClick={() => removeIngredient(index)} aria-label="Odstrani sestavino">
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="btn small" onClick={() => setIngredients((l) => [...l, emptyIngredient()])}>
          + Dodaj sestavino
        </button>
      </fieldset>

      <label>
        Postopek
        <textarea rows={6} maxLength={4000} value={instructions} onChange={(e) => setInstructions(e.target.value)} />
      </label>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>
          Prekliči
        </button>
        <button type="submit" className="btn primary" disabled={saving}>
          {saving ? "Shranjujem…" : "Shrani"}
        </button>
      </div>
    </form>
  );
}
