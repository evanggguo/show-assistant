package com.dossier.backend.superadmin;

import com.dossier.backend.common.exception.BusinessException;
import com.dossier.backend.common.exception.ResourceNotFoundException;
import com.dossier.backend.conversation.ConversationRepository;
import com.dossier.backend.conversation.DynamicSuggestionRepository;
import com.dossier.backend.conversation.MessageRepository;
import com.dossier.backend.document.DocumentRepository;
import com.dossier.backend.knowledge.KnowledgeRepository;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import com.dossier.backend.owner.PromptSuggestionRepository;
import com.dossier.backend.superadmin.dto.CreateOwnerRequest;
import com.dossier.backend.superadmin.dto.OwnerSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 超级管理服务 — owner 账号 CRUD
 * 初始密码统一为 888888
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private static final String DEFAULT_PASSWORD = "888888";

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final DynamicSuggestionRepository dynamicSuggestionRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final PromptSuggestionRepository promptSuggestionRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<OwnerSummaryResponse> listOwners() {
        return ownerRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public OwnerSummaryResponse createOwner(CreateOwnerRequest request) {
        if (ownerRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_TAKEN", "用户名已被占用");
        }

        Owner owner = Owner.builder()
            .name(request.getUsername())
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
            .build();

        Owner saved = ownerRepository.save(owner);
        log.info("SuperAdmin created owner id={}, username={}", saved.getId(), saved.getUsername());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOwner(Long id) {
        if (!ownerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Owner", id);
        }
        // Delete in FK dependency order
        dynamicSuggestionRepository.deleteByMessage_Conversation_OwnerId(id);
        messageRepository.deleteByConversation_OwnerId(id);
        conversationRepository.deleteByOwnerId(id);
        promptSuggestionRepository.deleteByOwnerId(id);
        knowledgeRepository.deleteByOwnerId(id);
        documentRepository.deleteByOwnerId(id);
        ownerRepository.deleteById(id);
        log.info("SuperAdmin deleted owner id={}", id);
    }

    private OwnerSummaryResponse mapToResponse(Owner owner) {
        return OwnerSummaryResponse.builder()
            .id(owner.getId())
            .username(owner.getUsername())
            .name(owner.getName())
            .createdAt(owner.getCreatedAt())
            .build();
    }
}
