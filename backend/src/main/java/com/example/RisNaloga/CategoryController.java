package com.example.RisNaloga;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Kategorije", description = "CRUD operacije nad kategorijami receptov")
public class CategoryController {

    private final CategoryRepository categories;
    private final RecipeRepository recipes;

    public CategoryController(CategoryRepository categories, RecipeRepository recipes) {
        this.categories = categories;
        this.recipes = recipes;
    }

    @GetMapping
    @Operation(summary = "Vrne vse kategorije")
    public List<Category> getAll() {
        return categories.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Vrne eno kategorijo po id")
    @ApiResponse(responseCode = "404", description = "Kategorija ne obstaja", content = @Content)
    public Category getOne(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ustvari novo kategorijo")
    @ApiResponse(responseCode = "409", description = "Kategorija s tem imenom že obstaja", content = @Content)
    public Category create(@Valid @RequestBody Category category) {
        category.setId(null);
        return categories.save(category);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Preimenuje kategorijo")
    @ApiResponse(responseCode = "404", description = "Kategorija ne obstaja", content = @Content)
    @ApiResponse(responseCode = "409", description = "Kategorija s tem imenom že obstaja", content = @Content)
    public Category update(@PathVariable Long id, @Valid @RequestBody Category category) {
        Category existing = find(id);
        existing.setName(category.getName());
        return categories.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Izbriše kategorijo", description = "Kategorije, ki jo uporablja vsaj en recept, ni mogoče izbrisati.")
    @ApiResponse(responseCode = "404", description = "Kategorija ne obstaja", content = @Content)
    @ApiResponse(responseCode = "409", description = "Kategorija je v uporabi", content = @Content)
    public void delete(@PathVariable Long id) {
        Category existing = find(id);
        if (recipes.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Kategorije ni mogoče izbrisati, ker jo uporabljajo recepti");
        }
        categories.delete(existing);
    }

    private Category find(Long id) {
        return categories.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorija z id " + id + " ne obstaja"));
    }
}
