package com.showassistant.backend.admin.document;

import com.showassistant.backend.admin.document.dto.DocumentResponse;
import com.showassistant.backend.common.exception.BusinessException;
import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.document.Document;
import com.showassistant.backend.document.DocumentRepository;
import com.showassistant.backend.document.DocumentStatus;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端文档上传和处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDocumentService {

    private static final Long DEFAULT_OWNER_ID = 1L;
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "docx");

    private final DocumentRepository documentRepository;
    private final OwnerRepository ownerRepository;
    private final DocumentProcessingService processingService;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments() {
        return documentRepository.findByOwnerIdOrderByCreatedAtDesc(DEFAULT_OWNER_ID)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) throws IOException {
        String originalFilename = StringUtils.cleanPath(
            Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空"));

        String extension = getExtension(originalFilename);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                "不支持的文件类型，仅支持 PDF、TXT、DOCX");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path targetPath = uploadPath.resolve(storedFilename);
        file.transferTo(targetPath);

        Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));

        Document document = Document.builder()
            .owner(owner)
            .filename(originalFilename)
            .fileType(extension)
            .fileSize(file.getSize())
            .filePath(targetPath.toString())
            .status(DocumentStatus.PENDING)
            .build();

        Document saved = documentRepository.save(document);
        log.info("Uploaded document id={}, filename={}", saved.getId(), originalFilename);
        return mapToResponse(saved);
    }

    @Transactional
    public DocumentResponse triggerProcessing(Long id) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        doc.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(doc);

        processingService.processAsync(id);
        return mapToResponse(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        // 删除物理文件
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException e) {
            log.warn("Failed to delete physical file for documentId={}: {}", id, e.getMessage());
        }

        documentRepository.deleteById(id);
        log.info("Deleted document id={}", id);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    private DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
            .id(doc.getId())
            .filename(doc.getFilename())
            .fileType(doc.getFileType())
            .fileSize(doc.getFileSize())
            .status(doc.getStatus().name())
            .createdAt(doc.getCreatedAt())
            .build();
    }
}
