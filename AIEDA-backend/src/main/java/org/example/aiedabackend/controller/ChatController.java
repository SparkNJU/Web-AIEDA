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
import java.util.Map;

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

    @Operation(summary = "发送消息并流式获取AI回复", description = "向指定会话发送用户消息（支持文件引用和不同输入类型），并通过SSE流式获取AI回复")
    @PostMapping(value = "/messages/{sid}/stream", produces = "text/event-stream")
    public SseEmitter sendMessageSSE(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid,
            @Parameter(description = "聊天消息请求对象", required = true)
            @RequestBody ChatRequestVO request) {
        // 确保请求中的sid与路径参数一致
        request.setSid(sid);
        
        // 获取inputType，如果没有指定则默认为question
        String inputType = request.getInputType() != null ? request.getInputType() : "question";
        
        // 根据inputType选择不同的处理方式
        switch (inputType) {
            case "config":
                return handleConfigMessage(request);
            case "intervention":
                return handleInterventionMessage(request);
            case "delete":
                return handleDeleteMessage(request);
            default: // "question" 或其他
                return handleQuestionMessage(request);
        }
    }

    @Operation(summary = "发送非流式消息", description = "向指定会话发送非流式消息（用于config、delete、intervention类型），不建立SSE连接")
    @PostMapping("/messages/{sid}/input")
    public Response<Boolean> sendMessageInput(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid,
            @Parameter(description = "聊天消息请求对象", required = true)
            @RequestBody ChatRequestVO request) {
        // 确保请求中的sid与路径参数一致
        request.setSid(sid);
        
        // 获取inputType，如果没有指定则默认为question（但这个接口主要用于非question类型）
        String inputType = request.getInputType() != null ? request.getInputType() : "question";
        
        boolean result = chatService.sendMessageInput(request.getUid(), sid, request.getContent(), request.getMetadata(), inputType);
        return Response.buildSuccess(result);
    }
    
    /**
     * 处理配置消息
     */
    private SseEmitter handleConfigMessage(ChatRequestVO request) {
        // 从metadata中提取配置信息
        if (request.getMetadata() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> llmConfig = (Map<String, Object>) request.getMetadata().get("llm_config");
            if (llmConfig != null) {
                String apiKey = (String) llmConfig.get("api_key");
                String baseUrl = (String) llmConfig.get("base_url");
                String model = (String) llmConfig.get("model");
                String agentType = (String) request.getMetadata().getOrDefault("agent_type", "orchestrator");
                return chatService.sendConfigMessage(request.getUid(), request.getSid(), agentType, apiKey, baseUrl, model);
            }
        }
        // 使用默认配置
        String agentType = getAgentTypeFromMetadata(request.getMetadata());
        return chatService.sendConfigMessage(request.getUid(), request.getSid(), agentType);
    }
    
    /**
     * 处理干预消息
     */
    private SseEmitter handleInterventionMessage(ChatRequestVO request) {
        // 直接使用带metadata的通用方法
        return chatService.sendMessageSSE(request.getUid(), request.getSid(), request.getContent(), request.getMetadata());
    }
    
    /**
     * 处理删除消息
     */
    private SseEmitter handleDeleteMessage(ChatRequestVO request) {
        // 直接使用带metadata的通用方法
        return chatService.sendMessageSSE(request.getUid(), request.getSid(), request.getContent(), request.getMetadata());
    }
    
    /**
     * 处理普通问题消息
     */
    private SseEmitter handleQuestionMessage(ChatRequestVO request) {
        if (request.getFileReferences() != null && !request.getFileReferences().isEmpty()) {
            return chatService.sendMessageWithFilesSSE(request.getUid(), request.getSid(), 
                                                      request.getContent(), request.getFileReferences(), request.getMetadata());
        } else {
            return chatService.sendMessageSSE(request.getUid(), request.getSid(), request.getContent(), request.getMetadata());
        }
    }
    
    /**
     * 从metadata中获取agent_type，默认为orchestrator
     */
    private String getAgentTypeFromMetadata(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("agent_type")) {
            return (String) metadata.get("agent_type");
        }
        return "orchestrator";
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

    @Operation(summary = "停止会话超时监控", description = "停止指定会话的SSE连接超时监控，用于硬干预")
    @PostMapping("/sessions/{sid}/timeout/stop")
    public Response<Boolean> stopSessionTimeout(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid) {
        boolean result = chatService.stopSessionTimeout(sid);
        return Response.buildSuccess(result);
    }

    @Operation(summary = "重启会话超时监控", description = "重启指定会话的SSE连接超时监控，用于软干预后恢复")
    @PostMapping("/sessions/{sid}/timeout/restart")
    public Response<Boolean> restartSessionTimeout(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Integer sid) {
        boolean result = chatService.restartSessionTimeout(sid);
        return Response.buildSuccess(result);
    }
}
