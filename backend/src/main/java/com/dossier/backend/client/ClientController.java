package com.dossier.backend.client;

import com.dossier.backend.chat.ChatService;
import com.dossier.backend.chat.dto.ChatRequest;
import com.dossier.backend.common.response.ApiResponse;
import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerService;
import com.dossier.backend.owner.dto.OwnerProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Client public API — routed by owner username, supports multi-owner data isolation.
 *
 * Routes:
 *   GET  /api/owners/{username}/profile       — owner public profile
 *   GET  /api/owners/{username}/suggestions   — initial suggestions
 *   POST /api/owners/{username}/chat/stream   — SSE streaming chat
 */
@Slf4j
@RestController
@RequestMapping("/api/owners/{username}")
@RequiredArgsConstructor
public class ClientController {

    private final OwnerService ownerService;
    private final ChatService chatService;

    /** Get the public profile of the specified owner. */
    @GetMapping("/profile")
    public ApiResponse<OwnerProfileResponse> getProfile(@PathVariable String username) {
        Owner owner = ownerService.getOwnerByUsername(username);
        return ApiResponse.ok(ownerService.getOwnerProfile(owner.getId()));
    }

    /** Get the initial suggestion list for the specified owner. */
    @GetMapping("/suggestions")
    public ApiResponse<List<String>> getSuggestions(@PathVariable String username) {
        Owner owner = ownerService.getOwnerByUsername(username);
        return ApiResponse.ok(ownerService.getInitialSuggestions(owner.getId()));
    }

    /**
     * SSE streaming chat entry point for the specified owner.
     * All conversation data (sessions, messages, RAG retrieval) is isolated to this owner.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
        @PathVariable String username,
        @Valid @RequestBody ChatRequest req) {

        Owner owner = ownerService.getOwnerByUsername(username);
        log.debug("Chat stream: username={}, ownerId={}, messageLength={}",
            username, owner.getId(), req.message().length());

        SseEmitter emitter = chatService.createEmitter();
        chatService.handleStream(req, emitter, owner.getId());
        return emitter;
    }
}
