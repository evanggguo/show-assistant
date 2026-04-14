package com.showassistant.backend.admin.suggestion;

import com.showassistant.backend.admin.suggestion.dto.CreateSuggestionRequest;
import com.showassistant.backend.admin.suggestion.dto.SuggestionResponse;
import com.showassistant.backend.admin.suggestion.dto.UpdateSuggestionRequest;
import com.showassistant.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端提示词 CRUD 接口
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
