package com.showassistant.backend.admin.knowledge;

import com.showassistant.backend.admin.knowledge.dto.CreateKnowledgeRequest;
import com.showassistant.backend.admin.knowledge.dto.UpdateKnowledgeRequest;
import com.showassistant.backend.common.response.ApiResponse;
import com.showassistant.backend.knowledge.dto.KnowledgeEntryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端知识库 CRUD 接口
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final AdminKnowledgeService service;

    @GetMapping
    public ApiResponse<List<KnowledgeEntryDto>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeEntryDto> create(@Valid @RequestBody CreateKnowledgeRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeEntryDto> update(
            @PathVariable Long id,
            @RequestBody UpdateKnowledgeRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
