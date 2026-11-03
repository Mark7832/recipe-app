package com.example.RisNaloga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.RisNaloga.RecipeDtos.IngredientRequest;
import com.example.RisNaloga.RecipeDtos.PageResponse;
import com.example.RisNaloga.RecipeDtos.RecipeRequest;
import com.example.RisNaloga.RecipeDtos.RecipeResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

/** Preveri iskanje, filtre, razvrščanje in paginacijo na pravi (H2) bazi. */
@SpringBootTest
class RecipeSearchTests {

    @Autowired RecipeService service;
    @Autowired RecipeRepository recipes;
    @Autowired CategoryRepository categories;

    Long soupId;

    @BeforeEach
    void seed() {
        recipes.deleteAll();
        categories.deleteAll();
        Category soup = new Category();
        soup.setName("Juhe");
        soupId = categories.save(soup).getId();
        Category dessert = new Category();
        dessert.setName("Sladice");
        Long dessertId = categories.save(dessert).getId();

        add("Gobova juha", "Kremna", 40, 4, soupId, "gobe", "moka");
        add("Zelenjavna juha", "Lahka", 20, 2, soupId, "korenje", "moka");
        add("Palačinke", "Tanke 100% dobre", 15, 4, dessertId, "moka", "mleko");
        add("Torta", "Čokoladna", 90, 8, dessertId, "čokolada", "jajca");
    }

    private void add(String title, String desc, int prep, int servings, Long cat, String... ingredients) {
        List<IngredientRequest> list = java.util.Arrays.stream(ingredients)
                .map(n -> new IngredientRequest(n, "1", "kos")).toList();
        service.create(new RecipeRequest(title, desc, "x", prep, servings, cat, list));
    }

    private PageResponse<RecipeResponse> search(String q, Long cat, Integer max, String sortBy, String dir, int page, int size) {
        return service.search(q, cat, max, sortBy, dir, page, size);
    }

    private List<String> titles(PageResponse<RecipeResponse> p) {
        return p.content().stream().map(RecipeResponse::title).toList();
    }

    @Test
    void searchMatchesTitleDescriptionAndIngredientWithoutDuplicates() {
        assertThat(titles(search("JUHA", null, null, "title", "asc", 0, 10))).containsExactly("Gobova juha", "Zelenjavna juha");
        assertThat(titles(search("čokoladna", null, null, "title", "asc", 0, 10))).containsExactly("Torta");
        // "moka" je sestavina v treh receptih, vsak se pojavi le enkrat
        PageResponse<RecipeResponse> flour = search("moka", null, null, "title", "asc", 0, 10);
        assertThat(titles(flour)).containsExactly("Gobova juha", "Palačinke", "Zelenjavna juha");
        assertThat(flour.totalElements()).isEqualTo(3);
    }

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(titles(search("%", null, null, "title", "asc", 0, 10))).containsExactly("Palačinke");
        assertThat(titles(search("_", null, null, "title", "asc", 0, 10))).isEmpty();
    }

    @Test
    void filtersByCategoryAndMaxPrepTime() {
        assertThat(titles(search(null, soupId, null, "title", "asc", 0, 10))).containsExactly("Gobova juha", "Zelenjavna juha");
        assertThat(titles(search(null, soupId, 30, "title", "asc", 0, 10))).containsExactly("Zelenjavna juha");
        assertThat(titles(search(null, null, 20, "prepTime", "asc", 0, 10))).containsExactly("Palačinke", "Zelenjavna juha");
    }

    @Test
    void sortsAndPaginates() {
        PageResponse<RecipeResponse> p0 = search(null, null, null, "prepTime", "desc", 0, 3);
        assertThat(titles(p0)).containsExactly("Torta", "Gobova juha", "Zelenjavna juha");
        assertThat(p0.totalElements()).isEqualTo(4);
        assertThat(p0.totalPages()).isEqualTo(2);
        assertThat(titles(search(null, null, null, "prepTime", "desc", 1, 3))).containsExactly("Palačinke");
        assertThat(titles(search(null, null, null, "newest", null, 0, 10))).first().isEqualTo("Torta");
        assertThat(search(null, null, null, "title", "asc", 5, 3).content()).isEmpty();
        assertThat(p0.content().get(0).ingredients()).isNotEmpty();
    }

    @Test
    void rejectsInvalidParameters() {
        assertThatThrownBy(() -> search(null, null, null, "abc", null, 0, 10)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> search(null, null, null, "title", "up", 0, 10)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> search(null, null, null, "title", null, -1, 10)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> search(null, null, null, "title", null, 0, 500)).isInstanceOf(ResponseStatusException.class);
    }
}
