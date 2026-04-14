package com.showassistant.backend.admin.document;

import com.showassistant.backend.ai.EmbeddingService;
import com.showassistant.backend.common.exception.BusinessException;
import com.showassistant.backend.common.exception.ResourceNotFoundException;
import com.showassistant.backend.document.Document;
import com.showassistant.backend.document.DocumentRepository;
import com.showassistant.backend.document.DocumentStatus;
import com.showassistant.backend.knowledge.KnowledgeEntry;
import com.showassistant.backend.knowledge.KnowledgeRepository;
import com.showassistant.backend.knowledge.KnowledgeType;
import com.showassistant.backend.owner.Owner;
import com.showassistant.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档解析和向量化处理服务
 * 支持 PDF / TXT / DOCX，将内容分块后生成向量存入 knowledge_entries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final Long DEFAULT_OWNER_ID = 1L;

    private final DocumentRepository documentRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final OwnerRepository ownerRepository;
    private final EmbeddingService embeddingService;

    @Async("sseTaskExecutor")
    @Transactional
    public void processAsync(Long documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        log.info("Start processing documentId={}, filename={}", documentId, doc.getFilename());

        try {
            String text = extractText(doc);
            if (text.isBlank()) {
                log.warn("Document id={} has no extractable text", documentId);
                doc.setStatus(DocumentStatus.COMPLETED);
                documentRepository.save(doc);
                return;
            }

            List<String> chunks = splitIntoChunks(text);
            Owner owner = ownerRepository.findById(DEFAULT_OWNER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", DEFAULT_OWNER_ID));

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                float[] embedding = embeddingService.embed(chunk);

                KnowledgeEntry entry = KnowledgeEntry.builder()
                    .owner(owner)
                    .type(KnowledgeType.DOCUMENT_CHUNK)
                    .title(doc.getFilename() + " [" + (i + 1) + "/" + chunks.size() + "]")
                    .content(chunk)
                    .embedding(embedding.length > 0 ? embedding : null)
                    .sourceDoc(documentId)
                    .build();

                knowledgeRepository.save(entry);
                log.debug("Saved chunk {}/{} for documentId={}", i + 1, chunks.size(), documentId);
            }

            doc.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(doc);
            log.info("Completed processing documentId={}, chunks={}", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process documentId={}", documentId, e);
            doc.setStatus(DocumentStatus.FAILED);
            documentRepository.save(doc);
        }
    }

    private String extractText(Document doc) throws Exception {
        Path path = Paths.get(doc.getFilePath());
        return switch (doc.getFileType().toLowerCase()) {
            case "pdf" -> extractPdf(path);
            case "txt" -> Files.readString(path, StandardCharsets.UTF_8);
            case "docx" -> extractDocx(path);
            default -> throw new BusinessException("UNSUPPORTED_TYPE",
                "不支持的文件类型: " + doc.getFileType());
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
