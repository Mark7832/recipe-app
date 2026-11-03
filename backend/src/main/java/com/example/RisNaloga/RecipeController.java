package com.example.RisNaloga;

import com.example.RisNaloga.RecipeDtos.PageResponse;
import com.example.RisNaloga.RecipeDtos.RecipeRequest;
import com.example.RisNaloga.RecipeDtos.RecipeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recepti", description = "CRUD operacije nad recepti (vključno s sestavinami in kategorijo)")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Išče, filtrira, razvršča in straniči recepte",
            description = "Vsi parametri so neobvezni. Brez parametrov vrne prvo stran (10 receptov), najnovejši najprej.")
    @ApiResponse(responseCode = "200", description = "Ena stran receptov (content je lahko prazen)")
    @ApiResponse(responseCode = "400", description = "Neveljaven parameter", content = @io.swagger.v3.oas.annotations.media.Content)
    public PageResponse<RecipeResponse> search(
            @Parameter(description = "Iskani niz: išče po naslovu, opisu in imenu sestavine (brez razlike velikih/malih črk)")
            @RequestParam(required = false) String q,
            @Parameter(description = "Id kategorije")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Največji čas priprave v minutah")
            @RequestParam(required = false) Integer maxPrepTime,
            @Parameter(description = "Razvrščanje: newest, title, prepTime ali servings")
            @RequestParam(defaultValue = "newest") String sortBy,
            @Parameter(description = "Smer: asc ali desc (privzeto desc za newest, sicer asc)")
            @RequestParam(required = false) String direction,
            @Parameter(description = "Številka strani, začne se pri 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Število receptov na strani (1 do 50)")
            @RequestParam(defaultValue = "10") int size) {
        return service.search(q, categoryId, maxPrepTime, sortBy, direction, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Vrne en recept po id")
    @ApiResponse(responseCode = "200", description = "Najden recept")
    @ApiResponse(responseCode = "404", description = "Recept ne obstaja", content = @io.swagger.v3.oas.annotations.media.Content)
    public RecipeResponse getOne(@PathVariable Long id) {
        return service.findOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ustvari nov recept", description = "Skupaj z recepti se shranijo tudi sestavine. categoryId je neobvezen.")
    @ApiResponse(responseCode = "201", description = "Recept ustvarjen")
    @ApiResponse(responseCode = "400", description = "Neveljavni podatki ali neobstoječa kategorija", content = @io.swagger.v3.oas.annotations.media.Content)
    public RecipeResponse create(@Valid @RequestBody RecipeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Posodobi obstoječi recept", description = "Seznam sestavin se nadomesti s poslanim seznamom.")
    @ApiResponse(responseCode = "200", description = "Recept posodobljen")
    @ApiResponse(responseCode = "400", description = "Neveljavni podatki ali neobstoječa kategorija", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "404", description = "Recept ne obstaja", content = @io.swagger.v3.oas.annotations.media.Content)
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Izbriše recept (in njegove sestavine)")
    @ApiResponse(responseCode = "204", description = "Recept izbrisan")
    @ApiResponse(responseCode = "404", description = "Recept ne obstaja", content = @io.swagger.v3.oas.annotations.media.Content)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
