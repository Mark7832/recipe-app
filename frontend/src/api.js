const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function request(method, path, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(API_URL + path, options);
  } catch (e) {
    const err = new Error("Povezava z zaledjem ni uspela. Preveri, da teče na " + API_URL + ".");
    err.network = true;
    throw err;
  }

  if (!res.ok) {
    let message = "Napaka strežnika (" + res.status + ")";
    try {
      const data = await res.json();
      if (data && data.message) message = data.message;
    } catch (e) {
      /* odgovor brez JSON telesa */
    }
    const err = new Error(message);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

export const recipesApi = {
  // params: { q, categoryId, maxPrepTime, sortBy, direction, page, size } (prazne vrednosti se izpustijo)
  search: (params = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== "" && value !== null && value !== undefined) qs.set(key, value);
    });
    const text = qs.toString();
    return request("GET", "/recipes" + (text ? "?" + text : ""));
  },
  get: (id) => request("GET", "/recipes/" + id),
  create: (data) => request("POST", "/recipes", data),
  update: (id, data) => request("PUT", "/recipes/" + id, data),
  remove: (id) => request("DELETE", "/recipes/" + id),
};

export const categoriesApi = {
  list: () => request("GET", "/categories"),
  create: (name) => request("POST", "/categories", { name }),
  update: (id, name) => request("PUT", "/categories/" + id, { name }),
  remove: (id) => request("DELETE", "/categories/" + id),
};
