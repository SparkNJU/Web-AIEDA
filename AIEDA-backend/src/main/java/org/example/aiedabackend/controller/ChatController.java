package org.example.aiedabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aiedabackend.service.ChatService;
import org.example.aiedabackend.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "聊天管理", description = "聊天相关接口")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Operation(summary = "获取用户会话列表", description = "根据用户ID获取所有聊天会话")
    @GetMapping("/sessions/{uid}")
    public Response<List<SessionVO>> getSessions(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Integer uid) {
        List<SessionVO> sessions = chatService.getSessions(uid);
        return Response.buildSuccess(sessions);
    }

    @Operation(summary = "创建新会话", description = "创建一个新的聊天会话")
    @PostMapping("/sessions")
    public Response<SessionVO> createSession(
            @Parameter(description = "创建会话请求对象", required = true)
            @RequestBody CreateSessionRequestVO request) {
        SessionVO sessionVO = chatService.createSession(request.getUid(), request.getTitle());
        return Response.buildSuccess(sessionVO);
    }

    @Operation(summary = "获取会话记录", description = "根据会话ID获取所有聊天记录")
    @GetMapping("/sessions/{sid}/records")
    public Response<List<RecordVO>> getRecords(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid) {
        List<RecordVO> records = chatService.getRecords(sid);
        return Response.buildSuccess(records);
    }

    @Operation(summary = "发送消息并获取AI回复", description = "向指定会话发送用户消息，并获取AI回复")
    @PostMapping("/messages")
    public Response<RecordVO> sendMessage(
            @Parameter(description = "聊天消息请求对象", required = true)
            @RequestBody ChatRequestVO request) {
        RecordVO replyRecord = chatService.sendMessage(request.getUid(), request.getSid(), request.getContent());
        return Response.buildSuccess(replyRecord);
    }

    @Operation(summary = "发送消息并流式获取AI回复", description = "向指定会话发送用户消息，并通过SSE流式获取AI回复")
    @PostMapping(value = "/messages/stream", produces = "text/event-stream")
    public SseEmitter sendMessageSSE(
            @Parameter(description = "聊天消息请求对象", required = true)
            @RequestBody ChatRequestVO request) {
        return chatService.sendMessageSSE(request.getUid(), request.getSid(), request.getContent());
    }

    @Operation(summary = "更新会话标题", description = "更新指定会话的标题")
    @PutMapping("/sessions/{sid}")
    public Response<Boolean> updateSessionTitle(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid,
            @Parameter(description = "更新会话请求对象，包含用户ID和新标题", required = true)
            @RequestBody UpdateSessionRequestVO request) {
        boolean result = chatService.updateSessionTitle(request.getUid(), sid, request.getTitle());
        return Response.buildSuccess(result);
    }

    @Operation(summary = "删除会话", description = "删除指定会话及其所有记录")
    @DeleteMapping("/sessions/{sid}")
    public Response<Boolean> deleteSession(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid,
            @Parameter(description = "用户ID", required = true)
            @RequestBody DeleteSessionRequestVO request) {
        boolean result = chatService.deleteSession(request.getUid(), sid);
        return Response.buildSuccess(result);
    }
}
