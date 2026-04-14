package com.showassistant.backend.admin.document;

import com.showassistant.backend.admin.document.dto.DocumentResponse;
import com.showassistant.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 管理端文档上传、查询、解析接口
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final AdminDocumentService documentService;

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list() {
        return ApiResponse.ok(documentService.listDocuments());
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ApiResponse.ok(documentService.uploadDocument(file));
    }

    @PostMapping("/{id}/process")
    public ApiResponse<DocumentResponse> process(@PathVariable Long id) {
        return ApiResponse.ok(documentService.triggerProcessing(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }
}
