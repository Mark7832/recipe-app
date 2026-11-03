import { useState } from "react";
import { categoriesApi } from "../api.js";

export default function CategoryManager({ categories, onChanged, onBack, notify }) {
  const [newName, setNewName] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState("");
  const [error, setError] = useState(null);

  async function run(action, okMessage) {
    setError(null);
    try {
      await action();
      notify(okMessage);
      await onChanged();
      return true;
    } catch (e) {
      setError(e.message);
      return false;
    }
  }

  async function add(e) {
    e.preventDefault();
    if (!newName.trim()) return setError("Ime kategorije je obvezno.");
    if (await run(() => categoriesApi.create(newName.trim()), "Kategorija dodana")) setNewName("");
  }

  async function saveEdit(id) {
    if (!editName.trim()) return setError("Ime kategorije je obvezno.");
    if (await run(() => categoriesApi.update(id, editName.trim()), "Kategorija posodobljena")) setEditingId(null);
  }

  function remove(c) {
    if (!window.confirm("Izbrišem kategorijo »" + c.name + "«?")) return;
    run(() => categoriesApi.remove(c.id), "Kategorija izbrisana");
  }

  return (
    <section className="form-card">
      <button className="btn small" onClick={onBack}>
        ← Nazaj na seznam
      </button>
      <h2>Kategorije</h2>

      <form className="inline-form" onSubmit={add}>
        <input
          placeholder="Ime nove kategorije"
          aria-label="Ime nove kategorije"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          maxLength={100}
        />
        <button className="btn primary" type="submit">
          Dodaj
        </button>
      </form>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      {categories.length === 0 && <p className="muted">Ni še nobene kategorije.</p>}

      <ul className="category-list">
        {categories.map((c) => (
          <li key={c.id}>
            {editingId === c.id ? (
              <>
                <input value={editName} onChange={(e) => setEditName(e.target.value)} aria-label="Novo ime" maxLength={100} />
                <button className="btn small primary" onClick={() => saveEdit(c.id)}>
                  Shrani
                </button>
                <button className="btn small" onClick={() => setEditingId(null)}>
                  Prekliči
                </button>
              </>
            ) : (
              <>
                <span className="grow">{c.name}</span>
                <button
                  className="btn small"
                  onClick={() => {
                    setEditingId(c.id);
                    setEditName(c.name);
                    setError(null);
                  }}
                >
                  Preimenuj
                </button>
                <button className="btn small danger" onClick={() => remove(c)}>
                  Izbriši
                </button>
              </>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
