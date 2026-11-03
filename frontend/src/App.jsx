import { useCallback, useEffect, useState } from "react";
import { categoriesApi } from "./api.js";
import { DEFAULT_FILTERS } from "./filters.js";
import RecipeList from "./components/RecipeList.jsx";
import RecipeDetail from "./components/RecipeDetail.jsx";
import RecipeForm from "./components/RecipeForm.jsx";
import CategoryManager from "./components/CategoryManager.jsx";

export default function App() {
  // view: { name: "list" } | { name: "detail", id } | { name: "form", recipe|null } | { name: "categories" }
  const [view, setView] = useState({ name: "list" });
    const [categories, setCategories] = useState([]);
  // Stanje iskanja/filtrov/strani živi tukaj, da se ohrani ob odprtju recepta in vrnitvi na seznam
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  const notify = useCallback((text) => {
    setToast(text);
    setTimeout(() => setToast(null), 2500);
  }, []);

  const reload = useCallback(async () => {
    try {
      setCategories(await categoriesApi.list());
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const goList = () => setView({ name: "list" });

  return (
    <>
      <header className="top">
        <div className="wrap top-inner">
          <h1 onClick={goList} role="button" tabIndex={0} onKeyDown={(e) => e.key === "Enter" && goList()}>
            Kuharski recepti
          </h1>
          <nav className="nav">
            <button className="btn" onClick={() => setView({ name: "categories" })}>
              Kategorije
            </button>
            <button className="btn primary" onClick={() => setView({ name: "form", recipe: null })}>
              + Nov recept
            </button>
          </nav>
        </div>
      </header>

      <main className="wrap">
        {error && (
          <div className="banner" role="alert">
            {error}{" "}
            <button className="btn small" onClick={reload}>
              Poskusi znova
            </button>
          </div>
        )}

        {view.name === "list" && (
          <RecipeList
            categories={categories}
            filters={filters}
            setFilters={setFilters}
            onOpen={(id) => setView({ name: "detail", id })}
            onNew={() => setView({ name: "form", recipe: null })}
          />
        )}

        {view.name === "detail" && (
          <RecipeDetail
            id={view.id}
            onBack={goList}
            onEdit={(recipe) => setView({ name: "form", recipe })}
            onDeleted={async () => {
              notify("Recept izbrisan");
              goList();
            }}
          />
        )}

        {view.name === "form" && (
          <RecipeForm
            recipe={view.recipe}
            categories={categories}
            onCancel={() => (view.recipe ? setView({ name: "detail", id: view.recipe.id }) : goList())}
            onSaved={async (saved, isNew) => {
              notify(isNew ? "Recept dodan" : "Recept posodobljen");
              setView({ name: "detail", id: saved.id });
            }}
          />
        )}

        {view.name === "categories" && (
          <CategoryManager
            categories={categories}
            onChanged={reload}
            onBack={goList}
            notify={notify}
          />
        )}
      </main>

      {toast && (
        <div className="toast" role="status">
          {toast}
        </div>
      )}
    </>
  );
}
