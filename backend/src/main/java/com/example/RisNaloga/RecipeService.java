package com.example.RisNaloga;

import com.example.RisNaloga.RecipeDtos.CategoryResponse;
import com.example.RisNaloga.RecipeDtos.IngredientRequest;
import com.example.RisNaloga.RecipeDtos.IngredientResponse;
import com.example.RisNaloga.RecipeDtos.PageResponse;
import com.example.RisNaloga.RecipeDtos.RecipeRequest;
import com.example.RisNaloga.RecipeDtos.RecipeResponse;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final CategoryRepository categories;

    public RecipeService(RecipeRepository recipes, CategoryRepository categories) {
        this.recipes = recipes;
        this.categories = categories;
    }

    /** Dovoljena polja za razvrščanje: ime v URL-ju -> polje entitete. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "newest", "id",
            "title", "title",
            "prepTime", "prepTimeMinutes",
            "servings", "servings");

    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Iskanje, filtriranje, razvrščanje in paginacija na strežniku.
     *
     * @param q           iskani niz (naslov, opis ali ime sestavine, brez razlike velikih/malih črk)
     * @param categoryId  samo recepti te kategorije
     * @param maxPrepTime samo recepti s časom priprave do te vrednosti (minute)
     * @param sortBy      newest | title | prepTime | servings
     * @param direction   asc | desc
     * @param page        številka strani (od 0)
     * @param size        velikost strani (1 do 50)
     */
    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> search(String q, Long categoryId, Integer maxPrepTime,
                                               String sortBy, String direction, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter page ne sme biti negativen");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parameter size mora biti med 1 in " + MAX_PAGE_SIZE);
        }
        if (maxPrepTime != null && maxPrepTime < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter maxPrepTime mora biti vsaj 1");
        }
        String field = SORT_FIELDS.get(sortBy == null || sortBy.isBlank() ? "newest" : sortBy);
        if (field == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Neveljaven sortBy. Dovoljeno: " + String.join(", ", SORT_FIELDS.keySet()));
        }
        Sort.Direction dir;
        if (direction == null || direction.isBlank()) {
            dir = "newest".equals(sortBy) || sortBy == null || sortBy.isBlank()
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("asc")) {
            dir = Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            dir = Sort.Direction.DESC;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neveljaven direction. Dovoljeno: asc, desc");
        }

        // id kot drugi kriterij naredi vrstni red stabilen (brez podvajanja med stranmi)
        Sort sort = Sort.by(dir, field).and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Recipe> result = recipes.findAll(buildSpec(q, categoryId, maxPrepTime), pageable);
        return new PageResponse<>(
                result.getContent().stream().map(RecipeService::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static Specification<Recipe> buildSpec(String q, Long categoryId, Integer maxPrepTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";

                // EXISTS podpoizvedba po sestavinah: recept se ne podvoji, če se ujema več sestavin
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Ingredient> ing = sub.from(Ingredient.class);
                sub.select(ing.<Long>get("id")).where(
                        cb.equal(ing.get("recipe"), root),
                        cb.like(cb.lower(ing.<String>get("name")), pattern, '!'));

                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern, '!'),
                        cb.like(cb.lower(root.<String>get("description")), pattern, '!'),
                        cb.exists(sub)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (maxPrepTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("prepTimeMinutes"), maxPrepTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Znaki % in _ iz uporabnikovega niza naj ne delujejo kot LIKE nadomestni znaki. */
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Transactional(readOnly = true)
    public RecipeResponse findOne(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        Recipe recipe = new Recipe();
        apply(recipe, request);
        return toResponse(recipes.save(recipe));
    }

    @Transactional
    public RecipeResponse update(Long id, RecipeRequest request) {
        Recipe recipe = load(id);
        apply(recipe, request);
        return toResponse(recipes.save(recipe));
    }

    @Transactional
    public void delete(Long id) {
        recipes.delete(load(id));
    }

    private Recipe load(Long id) {
        return recipes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Recept z id " + id + " ne obstaja"));
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.setTitle(request.title().trim());
        recipe.setDescription(request.description());
        recipe.setInstructions(request.instructions());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setServings(request.servings());

        if (request.categoryId() == null) {
            recipe.setCategory(null);
        } else {
            recipe.setCategory(categories.findById(request.categoryId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Kategorija z id " + request.categoryId() + " ne obstaja")));
        }

        // Sestavine nadomestimo z novim seznamom (orphanRemoval pobriše stare vrstice)
        recipe.getIngredients().clear();
        if (request.ingredients() != null) {
            for (IngredientRequest in : request.ingredients()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setName(in.name().trim());
                ingredient.setQuantity(in.quantity());
                ingredient.setUnit(in.unit());
                ingredient.setRecipe(recipe);
                recipe.getIngredients().add(ingredient);
            }
        }
    }

    static RecipeResponse toResponse(Recipe r) {
        Category c = r.getCategory();
        return new RecipeResponse(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getInstructions(),
                r.getPrepTimeMinutes(),
                r.getServings(),
                c == null ? null : new CategoryResponse(c.getId(), c.getName()),
                r.getIngredients().stream()
                        .map(i -> new IngredientResponse(i.getId(), i.getName(), i.getQuantity(), i.getUnit()))
                        .toList());
    }
}
