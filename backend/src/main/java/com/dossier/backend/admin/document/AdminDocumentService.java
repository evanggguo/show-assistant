package com.dossier.backend.admin.document;

import com.dossier.backend.admin.document.dto.DocumentResponse;
import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.document.Document;
import com.dossier.backend.document.DocumentRepository;
import com.dossier.backend.document.DocumentStatus;
import com.dossier.backend.knowledge.KnowledgeRepository;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerContextHolder;
import com.dossier.backend.owner.OwnerRepository;
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

/** Admin document upload and processing service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "docx", "ppt", "pptx", "md");

    private final DocumentRepository documentRepository;
    private final OwnerRepository ownerRepository;
    private final OwnerContextHolder ownerContextHolder;
    private final DocumentProcessingService processingService;
    private final KnowledgeRepository knowledgeRepository;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments() {
        return documentRepository.findByOwnerIdOrderByCreatedAtDesc(ownerContextHolder.getCurrentOwnerId())
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) throws IOException {
        String originalFilename = StringUtils.cleanPath(
            Objects.requireNonNull(file.getOriginalFilename(), "Filename must not be empty"));

        String extension = getExtension(originalFilename);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                "Unsupported file type. Only PDF, TXT, DOCX, PPT, and PPTX are allowed");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path targetPath = uploadPath.resolve(storedFilename);
        file.transferTo(targetPath);

        Owner owner = ownerContextHolder.getCurrentOwner();

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

        // Delete associated knowledge entries first
        knowledgeRepository.deleteBySourceDoc(id);
        log.info("Deleted knowledge entries for documentId={}", id);

        // Delete the physical file
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
