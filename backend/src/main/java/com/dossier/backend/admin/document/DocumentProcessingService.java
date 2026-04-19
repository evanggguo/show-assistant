package com.dossier.backend.admin.document;

import com.dossier.backend.ai.EmbeddingService;
import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.document.Document;
import com.dossier.backend.document.DocumentRepository;
import com.dossier.backend.document.DocumentStatus;
import com.dossier.backend.knowledge.KnowledgeEntry;
import com.dossier.backend.knowledge.KnowledgeRepository;
import com.dossier.backend.knowledge.KnowledgeType;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document parsing and vectorization service.
 * Supports PDF / TXT / DOCX / PPTX; splits content into chunks and stores vectors in knowledge_entries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final DocumentRepository documentRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final OwnerRepository ownerRepository;
    private final EmbeddingService embeddingService;

    // @Transactional intentionally omitted: combining @Async + @Transactional causes the
    // catch-block status update to be rolled back on exception. Each repository.save() call
    // is transactional on its own (Spring Data default).
    @Async("sseTaskExecutor")
    public void processAsync(Long documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        log.info("Start processing documentId={}, filename={}", documentId, doc.getFilename());

        try {
            String text = extractText(doc);
            if (text.isBlank()) {
                log.warn("Document id={} has no extractable text", documentId);
                updateStatus(doc, DocumentStatus.COMPLETED);
                return;
            }

            List<String> chunks = splitIntoChunks(text);
            // Fetch owner in this async context (doc.owner is LAZY and session is already closed)
            Long ownerId = doc.getOwner().getId();
            Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

            int saved = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                float[] embedding = embeddingService.embed(chunk);

                // Skip entries with no embedding to avoid vector type mismatch in DB.
                // In Phase 2 (stub mode) embeddings are unavailable; Phase 3 will populate them.
                if (embedding.length == 0) {
                    log.debug("No embedding for chunk {}/{}, skipping knowledge entry", i + 1, chunks.size());
                    continue;
                }

                KnowledgeEntry entry = KnowledgeEntry.builder()
                    .owner(owner)
                    .type(KnowledgeType.DOCUMENT_CHUNK)
                    .title(doc.getFilename() + " [" + (i + 1) + "/" + chunks.size() + "]")
                    .content(chunk)
                    .embedding(embedding)
                    .sourceDoc(documentId)
                    .build();

                knowledgeRepository.save(entry);
                saved++;
                log.debug("Saved chunk {}/{} for documentId={}", i + 1, chunks.size(), documentId);
            }

            updateStatus(doc, DocumentStatus.COMPLETED);
            log.info("Completed processing documentId={}, chunks={}, savedEntries={}", documentId, chunks.size(), saved);

        } catch (Exception e) {
            log.error("Failed to process documentId={}", documentId, e);
            updateStatus(doc, DocumentStatus.FAILED);
        }
    }

    private void updateStatus(Document doc, DocumentStatus status) {
        doc.setStatus(status);
        documentRepository.save(doc);
    }

    private String extractText(Document doc) throws Exception {
        Path path = Paths.get(doc.getFilePath());
        return switch (doc.getFileType().toLowerCase()) {
            case "pdf" -> extractPdf(path);
            case "txt", "md" -> Files.readString(path, StandardCharsets.UTF_8);
            case "docx" -> extractDocx(path);
            case "ppt", "pptx" -> extractPptx(path);
            default -> throw new BusinessException("UNSUPPORTED_TYPE",
                "Unsupported file type: " + doc.getFileType());
        };
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(Path path) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path))) {
            return doc.getParagraphs().stream()
                .map(XWPFParagraph::getText)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining("\n"));
        }
    }

    private String extractPptx(Path path) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(path))) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int boundary = findSentenceBoundary(text, end);
                if (boundary > start) end = boundary;
            }
            String chunk = text.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            int next = end - CHUNK_OVERLAP;
            start = next <= start ? end : next;
        }
        return chunks;
    }

    private int findSentenceBoundary(String text, int around) {
        int searchStart = Math.max(0, around - 100);
        for (int i = around; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '\n' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        return around;
    }
}
