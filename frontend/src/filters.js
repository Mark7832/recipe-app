// Privzeto stanje iskanja, filtrov, razvrščanja in paginacije seznama receptov.
export const DEFAULT_FILTERS = {
  q: "",
  categoryId: "",
  maxPrepTime: "",
  sort: "newest:desc", // oblika sortBy:direction
  page: 0,
  size: 6,
};

export const SORT_OPTIONS = [
  { value: "newest:desc", label: "Najnovejši najprej" },
  { value: "newest:asc", label: "Najstarejši najprej" },
  { value: "title:asc", label: "Naslov A–Ž" },
  { value: "title:desc", label: "Naslov Ž–A" },
  { value: "prepTime:asc", label: "Najhitrejši najprej" },
  { value: "prepTime:desc", label: "Najdaljša priprava najprej" },
  { value: "servings:desc", label: "Največ porcij" },
  { value: "servings:asc", label: "Najmanj porcij" },
];

export const PREP_TIME_OPTIONS = [
  { value: "", label: "Poljuben čas" },
  { value: "15", label: "do 15 min" },
  { value: "30", label: "do 30 min" },
  { value: "60", label: "do 60 min" },
  { value: "120", label: "do 120 min" },
];

export const PAGE_SIZES = [6, 12, 24];
