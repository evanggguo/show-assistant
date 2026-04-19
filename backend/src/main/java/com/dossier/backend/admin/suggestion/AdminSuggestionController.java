package com.dossier.backend.admin.suggestion;

import com.dossier.backend.admin.suggestion.dto.CreateSuggestionRequest;
import com.dossier.backend.admin.suggestion.dto.SuggestionResponse;
import com.dossier.backend.admin.suggestion.dto.UpdateSuggestionRequest;
import com.dossier.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin CRUD endpoints for prompt suggestions
 */
@RestController
@RequestMapping("/api/admin/suggestions")
@RequiredArgsConstructor
public class AdminSuggestionController {

    private final AdminSuggestionService service;

    @GetMapping
    public ApiResponse<List<SuggestionResponse>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SuggestionResponse> create(@Valid @RequestBody CreateSuggestionRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SuggestionResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateSuggestionRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
