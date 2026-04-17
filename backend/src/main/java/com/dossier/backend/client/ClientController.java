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
 * 客户端公开接口 — 基于 owner username 路由，支持多 owner 数据隔离
 *
 * 路由：
 *   GET  /api/owners/{username}/profile       — owner 公开简介
 *   GET  /api/owners/{username}/suggestions   — 初始提示词
 *   POST /api/owners/{username}/chat/stream   — SSE 流式对话
 */
@Slf4j
@RestController
@RequestMapping("/api/owners/{username}")
@RequiredArgsConstructor
public class ClientController {

    private final OwnerService ownerService;
    private final ChatService chatService;

    /**
     * 获取指定 owner 的公开简介
     */
    @GetMapping("/profile")
    public ApiResponse<OwnerProfileResponse> getProfile(@PathVariable String username) {
        Owner owner = ownerService.getOwnerByUsername(username);
        return ApiResponse.ok(ownerService.getOwnerProfile(owner.getId()));
    }

    /**
     * 获取指定 owner 的初始提示词列表
     */
    @GetMapping("/suggestions")
    public ApiResponse<List<String>> getSuggestions(@PathVariable String username) {
        Owner owner = ownerService.getOwnerByUsername(username);
        return ApiResponse.ok(ownerService.getInitialSuggestions(owner.getId()));
    }

    /**
     * 指定 owner 的 SSE 流式对话入口
     * 所有对话数据（会话、消息、RAG 检索）均隔离在该 owner 下
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
