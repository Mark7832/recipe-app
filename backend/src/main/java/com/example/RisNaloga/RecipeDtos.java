package com.example.RisNaloga;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Vhodni in izhodni objekti REST vmesnika (ločeni od entitet). */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record IngredientRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String quantity,
            @Size(max = 50) String unit) {
    }

    public record RecipeRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 1000) String description,
            @Size(max = 4000) String instructions,
            @Min(1) int prepTimeMinutes,
            @Min(1) int servings,
            Long categoryId,
            List<@Valid IngredientRequest> ingredients) {
    }

    public record CategoryResponse(Long id, String name) {
    }

    public record IngredientResponse(Long id, String name, String quantity, String unit) {
    }

    public record RecipeResponse(
            Long id,
            String title,
            String description,
            String instructions,
            int prepTimeMinutes,
            int servings,
            CategoryResponse category,
            List<IngredientResponse> ingredients) {
    }

    /** Ena stran rezultatov (paginacija). page je indeksiran od 0. */
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
