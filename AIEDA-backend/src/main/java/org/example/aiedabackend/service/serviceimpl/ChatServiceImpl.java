package org.example.aiedabackend.service.serviceimpl;

import org.example.aiedabackend.constant.MessageTypeConstant;
import org.example.aiedabackend.dao.RecordRepository;
import org.example.aiedabackend.dao.SessionRepository;
import org.example.aiedabackend.dao.FileRepository;
import org.example.aiedabackend.po.Record;
import org.example.aiedabackend.po.Session;
import org.example.aiedabackend.po.File;
import org.example.aiedabackend.service.ChatService;
import org.example.aiedabackend.vo.RecordVO;
import org.example.aiedabackend.vo.SessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private FileRepository fileRepository;

    // LLM配置参数
    @Value("${llm.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${llm.openai.base-url}")
    private String openaiBaseUrl;
    
    @Value("${llm.openai.model}")
    private String openaiModel;
    
    @Value("${llm.execution.model}")
    private String executionModel;
    
    @Value("${llm.temperature}")
    private String temperature;
    
    @Value("${llm.max-tokens}")
    private String maxTokens;

    private RestTemplate restTemplate = new RestTemplate();

    // 时间格式化器，用于显示毫秒级时间
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // SSE连接超时管理
    private final ConcurrentHashMap<SseEmitter, AtomicLong> sseLastActivityMap = new ConcurrentHashMap<>();
    // 会话级别的SSE连接管理：每个会话ID对应一个SSE连接
    private final ConcurrentHashMap<Integer, SseEmitter> sessionSseMap = new ConcurrentHashMap<>();
    // 会话ID到超时检查任务的映射，用于停止和重启超时计时
    private final ConcurrentHashMap<Integer, java.util.concurrent.ScheduledFuture<?>> sessionTimeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(2);
    private static final long INACTIVITY_TIMEOUT = 120000L; // 120秒无活动则超时

    /**
     * 获取当前时间戳字符串
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * 获取当前HTTP响应对象，用于设置SSE相关头信息
     */
    private HttpServletResponse getCurrentHttpResponse() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getResponse();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 注册SSE连接并开始超时监控
     */
    private void registerSseConnection(SseEmitter emitter, Integer sid) {
        long currentTime = System.currentTimeMillis();
        sseLastActivityMap.put(emitter, new AtomicLong(currentTime));
        
        // 检查是否已有该会话的SSE连接，如果有则关闭旧连接
        SseEmitter oldEmitter = sessionSseMap.get(sid);
        if (oldEmitter != null) {
            log.warn("[{}] 🔄 会话{}已存在SSE连接，关闭旧连接", getCurrentTimestamp(), sid);
            closeSseConnection(oldEmitter, "新连接替换");
        }
        
        // 注册新连接
        sessionSseMap.put(sid, emitter);
        log.info("[{}] 🔗 注册SSE连接超时监控 - 会话ID: {}", getCurrentTimestamp(), sid);
        
        // 停止旧的超时检查任务（如果存在）
        stopSessionTimeoutMonitoring(sid);
        
        // 启动新的超时检查任务
        java.util.concurrent.ScheduledFuture<?> timeoutTask = timeoutScheduler.scheduleAtFixedRate(() -> {
            checkSseTimeout(emitter);
        }, INACTIVITY_TIMEOUT, 5000L, TimeUnit.MILLISECONDS); // 每5秒检查一次
        
        // 保存任务引用，用于后续的停止和重启操作
        sessionTimeoutTasks.put(sid, timeoutTask);
    }

    /**
     * 取消注册SSE连接
     */
    private void unregisterSseConnection(SseEmitter emitter) {
        sseLastActivityMap.remove(emitter);
        log.info("[{}] 🚫 取消注册SSE连接", getCurrentTimestamp());
    }

    /**
     * 更新SSE连接的最后活跃时间
     */
    private void updateSseActivity(SseEmitter emitter) {
        AtomicLong lastActivity = sseLastActivityMap.get(emitter);
        if (lastActivity != null) {
            lastActivity.set(System.currentTimeMillis());
        }
    }

    /**
     * 停止指定会话的超时监控
     * 用于硬干预时暂停超时计时
     */
    private void stopSessionTimeoutMonitoring(Integer sid) {
        java.util.concurrent.ScheduledFuture<?> timeoutTask = sessionTimeoutTasks.get(sid);
        if (timeoutTask != null) {
            timeoutTask.cancel(false); // false表示不中断正在执行的任务
            sessionTimeoutTasks.remove(sid);
            log.info("[{}] ⏸️  停止会话{}的超时监控", getCurrentTimestamp(), sid);
        } else {
            log.debug("[{}] 会话{}无超时监控任务需要停止", getCurrentTimestamp(), sid);
        }
    }

    /**
     * 重启指定会话的超时监控
     * 用于软干预后恢复超时计时
     */
    private void restartSessionTimeoutMonitoring(Integer sid) {
        // 首先停止现有的监控任务（如果存在）
        stopSessionTimeoutMonitoring(sid);
        
        // 获取该会话对应的SSE连接
        SseEmitter emitter = sessionSseMap.get(sid);
        if (emitter != null) {
            // 重置活跃时间
            AtomicLong lastActivity = sseLastActivityMap.get(emitter);
            if (lastActivity != null) {
                lastActivity.set(System.currentTimeMillis());
            }
            
            // 启动新的超时检查任务
            java.util.concurrent.ScheduledFuture<?> timeoutTask = timeoutScheduler.scheduleAtFixedRate(() -> {
                checkSseTimeout(emitter);
            }, INACTIVITY_TIMEOUT, 5000L, TimeUnit.MILLISECONDS);
            
            sessionTimeoutTasks.put(sid, timeoutTask);
            log.info("[{}] 🔄 重启会话{}的超时监控", getCurrentTimestamp(), sid);
        } else {
            log.warn("[{}] 无法重启会话{}的超时监控，SSE连接不存在", getCurrentTimestamp(), sid);
        }
    }

    /**
     * 检查SSE连接是否超时
     */
    private void checkSseTimeout(SseEmitter emitter) {
        AtomicLong lastActivity = sseLastActivityMap.get(emitter);
        if (lastActivity == null) {
            return; // 连接已被移除
        }

        long currentTime = System.currentTimeMillis();
        long lastActivityTime = lastActivity.get();
        
        if (currentTime - lastActivityTime > INACTIVITY_TIMEOUT) {
            log.warn("[{}] ⏰ SSE连接因120秒无活动而超时，准备关闭", getCurrentTimestamp());
            closeSseConnection(emitter, "无活动超时");
        }
    }

    /**
     * 关闭SSE连接并清理资源
     */
    private void closeSseConnection(SseEmitter emitter, String reason) {
        try {
            // 找到对应的会话ID并清理超时任务
            Integer sidToRemove = null;
            for (java.util.Map.Entry<Integer, SseEmitter> entry : sessionSseMap.entrySet()) {
                if (entry.getValue().equals(emitter)) {
                    sidToRemove = entry.getKey();
                    break;
                }
            }
            
            if (sidToRemove != null) {
                // 停止超时监控任务
                stopSessionTimeoutMonitoring(sidToRemove);
                // 从会话映射中移除
                sessionSseMap.remove(sidToRemove);
            }
            
            // 清理超时监控
            sseLastActivityMap.remove(emitter);
            
            emitter.complete();
            log.info("[{}] � SSE连接已关闭 - 原因: {}", getCurrentTimestamp(), reason);
        } catch (Exception e) {
            log.error("[{}] ❌ 关闭SSE连接失败 - 原因: {}", getCurrentTimestamp(), reason, e);
        }
    }

    /**
     * 检查SSE连接是否仍然有效
     * 注意：这个方法现在主要用于日志记录，不影响实际的数据发送
     * @param emitter SSE发射器
     * @return 连接是否有效（总是返回true，让实际发送时处理异常）
     */
    private boolean isEmitterActive(SseEmitter emitter) {
        if (emitter == null) {
            log.warn("[{}] ❌ SSE发射器为null", getCurrentTimestamp());
            return false;
        }
        
        // 简单检查：如果emitter不为null，就认为可能有效
        // 实际的连接状态将在发送数据时检查
        log.debug("[{}] ✅ SSE发射器存在，假定连接有效", getCurrentTimestamp());
        return true;
    }

    @Override
    public SseEmitter createSessionSSE(Integer uid, Integer sid) {
        log.info("[{}] 🔗 为会话创建独立SSE连接 - uid: {}, sid: {}", getCurrentTimestamp(), uid, sid);

        // 检查是否已有该会话的SSE连接，如果有则关闭旧连接
        if (sessionSseMap.containsKey(sid)) {
            log.info("[{}] ⚠️  会话 {} 已存在SSE连接，关闭旧连接", getCurrentTimestamp(), sid);
            SseEmitter oldEmitter = sessionSseMap.remove(sid);
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.warn("[{}] 关闭旧SSE连接时出现异常: {}", getCurrentTimestamp(), e.getMessage());
            }
        }

        // 创建新的SSE连接，设置较长但有限的超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时，避免无限连接
        
        // 为SSE连接设置服务器环境优化的响应头
        try {
            // 通过HttpServletResponse设置SSE相关头信息（针对服务器环境优化）
            HttpServletResponse response = getCurrentHttpResponse();
            if (response != null) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                response.setHeader("Connection", "keep-alive");
                response.setHeader("X-Accel-Buffering", "no"); // 禁用Nginx缓冲
                response.setCharacterEncoding("UTF-8");
                response.flushBuffer(); // 立即刷新响应头
                log.debug("[{}] ✅ 已设置SSE优化响应头", getCurrentTimestamp());
            }
        } catch (Exception e) {
            log.debug("[{}] 📝 无法设置SSE响应头: {}", getCurrentTimestamp(), e.getMessage());
        }

        // 注册SSE连接，实现会话级管理
        registerSseConnection(emitter, sid);
        sessionSseMap.put(sid, emitter);

        // 设置连接完成和异常处理
        emitter.onCompletion(() -> {
            log.info("[{}] 🔚 会话 {} 的SSE连接正常完成", getCurrentTimestamp(), sid);
            sessionSseMap.remove(sid);
            unregisterSseConnection(emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("[{}] ⏰ 会话 {} 的SSE连接超时", getCurrentTimestamp(), sid);
            sessionSseMap.remove(sid);
            unregisterSseConnection(emitter);
        });

        emitter.onError((ex) -> {
            log.error("[{}] ❌ 会话 {} 的SSE连接出现异常: {}", getCurrentTimestamp(), sid, ex.getMessage());
            sessionSseMap.remove(sid);
            unregisterSseConnection(emitter);
        });

        // 发送连接确认消息
        try {
            String confirmMessage = "{\"type\":\"connection\",\"message\":\"会话SSE连接已建立\",\"sessionId\":" + sid + ",\"userId\":" + uid + "}";
            emitter.send(SseEmitter.event()
                .name("connection")
                .data(confirmMessage)
                .reconnectTime(3000));
                
            // 强制刷新确认消息
            try {
                java.lang.reflect.Field field = emitter.getClass().getDeclaredField("handler");
                field.setAccessible(true);
                Object handler = field.get(emitter);
                if (handler != null) {
                    java.lang.reflect.Method flushMethod = handler.getClass().getMethod("flush");
                    flushMethod.invoke(handler);
                }
            } catch (Exception flushEx) {
                log.debug("[{}] 无法强制刷新连接确认消息: {}", getCurrentTimestamp(), flushEx.getMessage());
            }
            
            log.info("[{}] 📤 会话 {} SSE连接确认消息已发送", getCurrentTimestamp(), sid);
        } catch (IOException e) {
            log.error("[{}] ❌ 发送SSE连接确认消息失败: {}", getCurrentTimestamp(), e.getMessage());
        }

        return emitter;
    }

    @Override
    public List<SessionVO> getSessions(Integer uid) {
        List<Session> sessions = sessionRepository.findByUid(uid);
        // 按updateTime倒序排列，确保最新活跃的会话显示在最上面
        return sessions.stream()
                .sorted((s1, s2) -> s2.getUpdateTime().compareTo(s1.getUpdateTime()))
                .map(Session::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新会话的最后活跃时间
     * 在发送消息时调用，确保最新发送消息的会话显示在列表最上面
     */
    private void updateSessionTime(Integer uid, Integer sid) {
        try {
            var optional = sessionRepository.findById(sid);
            if (optional.isPresent()) {
                Session session = optional.get();
                // 验证会话属于该用户
                if (session.getUid().equals(uid)) {
                    session.setUpdateTime(LocalDateTime.now());
                    sessionRepository.save(session);
                    log.info("更新会话{}的活跃时间", sid);
                } else {
                    log.warn("用户{}尝试更新不属于自己的会话{}", uid, sid);
                }
            } else {
                log.warn("尝试更新不存在的会话{}", sid);
            }
        } catch (Exception e) {
            log.error("更新会话时间失败，sid: {}, uid: {}", sid, uid, e);
        }
    }

    @Override
    public SessionVO createSession(Integer uid, String title) {
        LocalDateTime now = LocalDateTime.now();
        Session session = new Session(uid, title, now, now);
        session = sessionRepository.save(session);
        return session.toVO();
    }

    @Override
    public List<RecordVO> getRecords(Integer sid) {
        List<Record> records = recordRepository.findBySidOrderBySequenceAsc(sid);
        return records.stream().map(Record::toVO).collect(Collectors.toList());
    }

    @Override
    public boolean updateSessionTitle(Integer uid, Integer sid, String title) {
        var optional = sessionRepository.findById(sid);
        if (optional.isPresent() && optional.get().getUid().equals(uid)) {
            Session session = optional.get();
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.save(session);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean deleteSession(Integer uid, Integer sid) {
        var optional = sessionRepository.findById(sid);
        if (optional.isPresent() && optional.get().getUid().equals(uid)) {
            // 删除所有关联的记录
            recordRepository.deleteBySid(sid);
            // 删除会话
            sessionRepository.deleteById(sid);
            return true;
        }
        return false;
    }

    @Override
    public SseEmitter sendMessageSSE(Integer uid, Integer sid, String content) {
        return sendMessageSSE(uid, sid, content, null); // 使用空metadata
    }

    @Override
    public SseEmitter sendMessageSSE(Integer uid, Integer sid, String content, java.util.Map<String, Object> metadata) {
        // 调用统一的内部方法，不传文件引用
        return sendMessageSSEInternal(uid, sid, content, null, metadata);
    }
    
    /**
     * 从metadata中获取字符串值的辅助方法
     */
    private String getStringFromMetadata(java.util.Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata != null && metadata.containsKey(key)) {
            Object value = metadata.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        return defaultValue;
    }

    @Override
    public SseEmitter sendMessageWithFilesSSE(Integer uid, Integer sid, String content, List<String> fileReferences) {
        return sendMessageWithFilesSSE(uid, sid, content, fileReferences, null); // 使用空metadata
    }

    @Override
    public SseEmitter sendMessageWithFilesSSE(Integer uid, Integer sid, String content, List<String> fileReferences, java.util.Map<String, Object> metadata) {
        // 调用统一的内部方法，传递文件引用
        return sendMessageSSEInternal(uid, sid, content, fileReferences, metadata);
    }

    /**
     * 统一的SSE消息发送内部方法
     * 处理带文件和不带文件的消息发送，确保操作步骤一致
     * @param uid 用户ID
     * @param sid 会话ID
     * @param content 消息内容
     * @param fileReferences 文件引用列表，可以为null
     * @param metadata 元数据
     * @return SSE发射器
     */
    private SseEmitter sendMessageSSEInternal(Integer uid, Integer sid, String content, List<String> fileReferences, java.util.Map<String, Object> metadata) {
        // 根据是否有文件来决定日志信息
        if (fileReferences != null && !fileReferences.isEmpty()) {
            log.info("[{}] 📨 开始处理带文件的SSE消息请求 - uid: {}, sid: {}, fileReferences: {}, metadata: {}", 
                    getCurrentTimestamp(), uid, sid, fileReferences, metadata);
        } else {
            log.info("[{}] 📨 开始处理SSE消息请求 - uid: {}, sid: {}, metadata: {}", 
                    getCurrentTimestamp(), uid, sid, metadata);
        }
        
        // 从metadata中提取agent_type和input_type，如果没有则使用默认值
        String agentType = getStringFromMetadata(metadata, "agent_type", "orchestrator");
        String inputType = getStringFromMetadata(metadata, "input_type", "question");
        
        // 🔗 获取或创建会话级SSE连接
        SseEmitter emitter = getOrCreateSessionSSE(uid, sid);

        // 更新会话时间，确保最新发送消息的会话显示在最上面
        updateSessionTime(uid, sid);

        // 保存用户消息
        List<Record> existing = recordRepository.findBySidOrderBySequenceAsc(sid);
        int nextSeq = existing.isEmpty() ? 1 : existing.size() + 1;
        LocalDateTime now = LocalDateTime.now();
        Record userRecord = new Record(sid, uid, true, content, nextSeq, MessageTypeConstant.USER, now);
        Record savedRecord = recordRepository.save(userRecord);
        
        // 🔗 如果有文件引用，则关联文件与当前记录
        if (fileReferences != null && !fileReferences.isEmpty()) {
            try {
                fileRepository.updateRidByFileIds(fileReferences, savedRecord.getRid());
                log.info("[{}] ✅ 成功关联 {} 个文件到记录 {} - fileIds: {}", 
                        getCurrentTimestamp(), fileReferences.size(), savedRecord.getRid(), fileReferences);
            } catch (Exception e) {
                log.error("[{}] ❌ 关联文件到记录失败 - recordId: {}, fileIds: {}, error: {}", 
                        getCurrentTimestamp(), savedRecord.getRid(), fileReferences, e.getMessage());
            }
        }

        // 异步处理AI流式响应，使用会话级SSE连接，传递文件引用和metadata
        final SseEmitter finalEmitter = emitter;
        CompletableFuture.runAsync(() -> {
            // 根据是否有文件引用来选择不同的处理方法
            if (fileReferences != null && !fileReferences.isEmpty()) {
                processAIStreamResponseWithFiles(finalEmitter, uid, sid, content, fileReferences, nextSeq, agentType, inputType, metadata);
            } else {
                processAIStreamResponse(finalEmitter, uid, sid, content, nextSeq, agentType, inputType, metadata);
            }
        });

        return emitter;
    }

    /**
     * 获取或创建会话级SSE连接
     * 如果会话已有连接则复用，否则创建新连接
     */
    private SseEmitter getOrCreateSessionSSE(Integer uid, Integer sid) {
        // 检查是否已有该会话的SSE连接
        SseEmitter existingEmitter = sessionSseMap.get(sid);
        if (existingEmitter != null) {
            log.info("[{}] ♻️  复用会话 {} 的现有SSE连接", getCurrentTimestamp(), sid);
            return existingEmitter;
        }

        // 创建新的会话级SSE连接
        log.info("[{}] 🆕 为会话 {} 创建新的SSE连接", getCurrentTimestamp(), sid);
        return createSessionSSE(uid, sid);
    }

    /**
     * 处理AI流式回复的完整流程（支持Agent类型和输入类型）
     */
    private void processAIStreamResponse(SseEmitter emitter, Integer uid, Integer sid, String content, int nextSeq, String agentType, String inputType) {
        // 委托给支持metadata的完整版本，使用空metadata
        processAIStreamResponse(emitter, uid, sid, content, nextSeq, agentType, inputType, null);
    }

    /**
     * 处理AI流式回复的完整流程（支持Agent类型、输入类型和metadata）
     */
    private void processAIStreamResponse(SseEmitter emitter, Integer uid, Integer sid, String content, int nextSeq, String agentType, String inputType, java.util.Map<String, Object> requestMetadata) {
        StringBuilder aiReply = new StringBuilder();
        String userInputUrl = "http://localhost:8000/api/v1/user/input";

        try {
            // 1. 提交用户输入
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", content);
            inputRequest.put("input_type", inputType != null ? inputType : "question"); // 使用传入的inputType
            
            // 添加metadata，包含agent_type
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("agent_type", agentType != null ? agentType : "orchestrator");
            
            // 合并请求中的metadata（如干预类型信息）
            if (requestMetadata != null && !requestMetadata.isEmpty()) {
                metadata.putAll(requestMetadata);
                log.info("[{}] 📋 合并请求metadata: {}", getCurrentTimestamp(), requestMetadata);
            }
            
            // 如果是config类型，添加LLM配置
            String finalInputType = inputType != null ? inputType : "question";
            if ("config".equals(finalInputType)) {
                var llmConfig = new java.util.HashMap<String, Object>();
                llmConfig.put("api_key", openaiApiKey);
                llmConfig.put("base_url", openaiBaseUrl);
                llmConfig.put("model", openaiModel);
                metadata.put("llm_config", llmConfig);
                
                // 添加其他配置参数
                metadata.put("execution_model", executionModel);
                metadata.put("temperature", temperature);
                metadata.put("max_tokens", maxTokens);
            }
            
            inputRequest.put("metadata", metadata);

            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            if (inputResponse == null || !"success".equals(inputResponse.get("status"))) {
                sendErrorToFrontend(emitter, "提交用户输入失败");
                return;
            }

            // 立即发送开始信号
            sendMessageToFrontend(emitter, "start", "AI正在思考...", null);

            // 2. 立即开始获取AI流式回复，实时转发
            String sessionId = sid.toString();
            String finalReply = getAIStreamResponse(emitter, sessionId, aiReply);

            // 保存完整的AI回复
            Record aiRecord = null;
            if (finalReply != null && !finalReply.trim().isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                aiRecord = new Record(sid, uid, false, finalReply, nextSeq + 1, MessageTypeConstant.LLM_GENERATION, now);
                recordRepository.save(aiRecord);
            }

            // 发送完成信号
            sendCompleteToFrontend(emitter, "回复完成", aiRecord != null ? aiRecord.getRid() : -1);

            System.out.println("流式处理完成，已发送complete事件");

        } catch (Exception e) {
            log.error("SSE流式处理失败", e);
            handleProcessError(emitter, e, uid, sid, nextSeq);
        } finally {
            // 确保连接被正确关闭
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("关闭SSE连接失败", e);
            }
        }
    }

    /**
     * 获取AI流式回复
     */
    private String getAIStreamResponse(SseEmitter emitter, String sessionId, StringBuilder aiReply) throws Exception {
        // 使用会话ID参数化的流式端点
        String sseStreamUrl = "http://localhost:8000/api/v1/stream?session_id=" + sessionId;
        boolean generationFinished = false;
        boolean[] foundFinishStart = {false}; // 使用数组来保持状态
        StringBuilder fullDelta = new StringBuilder(); // 用于累积不完整的标签

        // 建立SSE连接
        URI uri = URI.create(sseStreamUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setConnectTimeout(10000); // 10秒连接超时
        connection.setReadTimeout(60000); // 60秒读取超时

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {

            log.info("[{}] 🔗 SSE连接已建立，开始读取大模型流式数据 - sessionId: {}, URL: {}", 
                getCurrentTimestamp(), sessionId, sseStreamUrl);
            
            String line;
            // StringBuilder currentEvent = new StringBuilder(); // 不再需要该变量
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null && !generationFinished) {
                lineCount++;
                
                // 每100行检查一次连接状态
                if (lineCount % 100 == 0) {
                    if (!isEmitterActive(emitter)) {
                        log.warn("[{}] ⚠️  SSE连接已关闭，停止处理流式数据 - sessionId: {}, 行号: {}", 
                            getCurrentTimestamp(), sessionId, lineCount);
                        break;
                    }
                }
                
                // 处理SSE数据行
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6); // 移除"data: "前缀

                    if (jsonData.trim().isEmpty()) {
                        log.debug("[{}] 📄 收到空数据行 - sessionId: {}, 行号: {}", getCurrentTimestamp(), sessionId, lineCount);
                        continue;
                    }

                    log.info("[{}] 📨 从大模型收到SSE数据 - sessionId: {}, 行号: {}, 数据长度: {}", 
                        getCurrentTimestamp(), sessionId, lineCount, jsonData.length());

                    try {
                        // 解析并处理事件数据，立即转发到前端
                        log.info("[{}] 🔄 开始处理SSE事件数据... - sessionId: {}", getCurrentTimestamp(), sessionId);
                        generationFinished = processSSEEventData(emitter, jsonData, sessionId,
                            aiReply, fullDelta, foundFinishStart);
                        
                        if (generationFinished) {
                            log.info("[{}] 🏁 检测到生成结束信号 - sessionId: {}, 总处理行数: {}", 
                                getCurrentTimestamp(), sessionId, lineCount);
                            break;
                        }

                    } catch (Exception parseError) {
                        log.error("[{}] ❌ 解析SSE事件数据失败 - sessionId: {}, 行号: {}, 数据: {}", 
                            getCurrentTimestamp(), sessionId, lineCount, 
                            jsonData.length() > 200 ? jsonData.substring(0, 200) + "..." : jsonData, 
                            parseError);
                        // 即使解析失败，也尝试继续处理其他事件
                    }
                } else if (line.trim().isEmpty()) {
                    // 空行表示事件结束，可以在这里做一些清理工作
                    log.debug("[{}] 📄 收到空行 - 事件分隔符 - sessionId: {}", getCurrentTimestamp(), sessionId);
                    continue;
                } else {
                    // 处理其他SSE头部信息（如event:, id:等）
                    log.info("[{}] 📋 SSE头部信息 - sessionId: {}: {}", getCurrentTimestamp(), sessionId, line);
                }
                
                // 确保及时响应前端，避免缓冲延迟
                if (aiReply.length() > 0) {
                    // 每隔一定数量的字符就主动flush一次
                    if (aiReply.length() % 50 == 0) {
                        try {
                            // 发送心跳或者刷新连接
                            Thread.sleep(10); // 短暂延迟确保数据传输
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        return aiReply.length() > 0 ? aiReply.toString() : "AI回复为空";
    }

    /**
     * 处理SSE事件数据
     */
    private boolean processSSEEventData(SseEmitter emitter, String jsonData, String sessionId,
                                       StringBuilder aiReply, StringBuilder fullDelta, boolean[] foundFinishStart) throws Exception {

        log.info("[{}] 🔍 开始解析SSE事件数据 - 数据长度: {}", getCurrentTimestamp(), jsonData.length());
        
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> eventData = mapper.readValue(jsonData, java.util.Map.class);

        // 打印接收到的事件数据用于调试
        String eventType = (String) eventData.get("type");
        log.info("[{}] 📊 解析SSE事件成功 - 事件类型: {}, 会话ID: {}", getCurrentTimestamp(), eventType, sessionId);
        
        if (log.isTraceEnabled()) {
            log.trace("[{}] SSE事件详细内容: {}", getCurrentTimestamp(), jsonData);
        }

        // 检查是否是连接确认消息
        if ("connection".equals(eventData.get("type"))) {
            log.info("[{}] 🔗 SSE连接已建立: {}", getCurrentTimestamp(), eventData.get("message"));
            return false;
        }

        // 检查是否是新格式的事件数据（包含events数组）
        if (eventData.containsKey("events")) {
            @SuppressWarnings("unchecked")
            var events = (java.util.List<java.util.Map<String, Object>>) eventData.get("events");
            if (events != null) {
                log.info("[{}] 📦 处理批量事件 - 事件数量: {}", getCurrentTimestamp(), events.size());
                for (int i = 0; i < events.size(); i++) {
                    var event = events.get(i);
                    log.info("[{}] 🔄 处理批量事件 {}/{}", getCurrentTimestamp(), i + 1, events.size());
                    if (processSingleEvent(emitter, event, sessionId, aiReply, fullDelta, foundFinishStart)) {
                        log.info("[{}] 🏁 在批量事件中检测到生成结束", getCurrentTimestamp());
                        return true; // 生成完成
                    }
                }
            }
            return false;
        }

        // 处理旧格式的事件类型数据
        if ("event".equals(eventData.get("type"))) {
            log.info("[{}] 📨 处理旧格式事件数据", getCurrentTimestamp());
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) eventData.get("data");
            if (data != null) {
                @SuppressWarnings("unchecked")
                var innerData = (java.util.Map<String, Object>) data.get("data");
                if (innerData != null && Boolean.TRUE.equals(innerData.get("batch"))) {
                    // 处理批量事件
                    @SuppressWarnings("unchecked")
                    var events = (java.util.List<java.util.Map<String, Object>>) innerData.get("events");
                    if (events != null) {
                        log.info("[{}] 📦 处理旧格式批量事件 - 事件数量: {}", getCurrentTimestamp(), events.size());
                        for (int i = 0; i < events.size(); i++) {
                            var event = events.get(i);
                            log.info("[{}] 🔄 处理旧格式事件 {}/{}", getCurrentTimestamp(), i + 1, events.size());
                            if (processSingleEvent(emitter, event, sessionId, aiReply, fullDelta, foundFinishStart)) {
                                log.info("[{}] 🏁 在旧格式批量事件中检测到生成结束", getCurrentTimestamp());
                                return true; // 生成完成
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 处理单个事件
     */
    private boolean processSingleEvent(SseEmitter emitter, java.util.Map<String, Object> event, String sessionId,
                                     StringBuilder aiReply, StringBuilder fullDelta, boolean[] foundFinishStart) throws Exception {

        // 尝试从顶层和data字段获取session_id和user_id
        String eventSessionId = (String) event.get("session_id");
        String userId = (String) event.get("user_id");
        String eventType = (String) event.get("event");
        String tool = (String) event.get("tool");

        // 如果顶层没有，尝试从data字段获取
        if (eventSessionId == null || userId == null || eventType == null) {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.get("data");
            if (data != null) {
                if (eventSessionId == null) eventSessionId = (String) data.get("session_id");
                if (userId == null) userId = (String) data.get("user_id");
                if (eventType == null) eventType = (String) data.get("event");
                if (tool == null) tool = (String) data.get("tool");
            }
        }

        // 打印事件详情到控制台
        log.info("[{}] 🔍 处理单个事件 - session_id: {}, user_id: {}, event: {}, tool: {}", 
            getCurrentTimestamp(), eventSessionId, userId, eventType, tool);

        // 🔒 关键修复：验证事件的会话ID是否与当前SSE连接的会话ID匹配
        if (eventSessionId != null && !eventSessionId.equals(sessionId)) {
            log.warn("[{}] ⚠️  事件会话ID({})与当前SSE连接会话ID({})不匹配，跳过处理", 
                getCurrentTimestamp(), eventSessionId, sessionId);
            return false; // 跳过不属于当前会话的事件
        }

        // 检查是否是结束事件（agent.loop_end）
        if ("agent.loop_end".equals(eventType)) {
            log.info("[{}] 🏁 检测到agent.loop_end事件，AI回复生成完成 - sessionId: {}", getCurrentTimestamp(), eventSessionId);
            return true; // 生成完成
        }

        // 处理流式输出事件（llm_tool.chunk）
        if ("llm_tool.chunk".equals(eventType)) {
            log.info("[{}] 📝 处理流式输出事件 - sessionId: {}, tool: {}", getCurrentTimestamp(), eventSessionId, tool);
            // 首先尝试从顶层获取payload
            @SuppressWarnings("unchecked")
            var payload = (java.util.Map<String, Object>) event.get("payload");
            
            // 如果顶层没有，尝试从data字段获取
            if (payload == null) {
                @SuppressWarnings("unchecked")
                var data = (java.util.Map<String, Object>) event.get("data");
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    var dataPayload = (java.util.Map<String, Object>) data.get("payload");
                    payload = dataPayload; // 赋值给外层变量
                }
            }
            
            if (payload != null && payload.get("delta") != null) {
                String delta = payload.get("delta").toString();

                // 添加详细的接收日志
                log.info("[{}] 🔵 从大模型接收Delta内容 - 长度: {}, 内容: {}", 
                    getCurrentTimestamp(), 
                    delta.length(),
                    delta.length() > 100 ? delta.substring(0, 100) + "..." : delta);

                if (!delta.isEmpty()) {
                    // 记录处理前的状态
                    log.info("[{}] 🔄 开始处理Delta内容 - 原始长度: {}", getCurrentTimestamp(), delta.length());
                    
                    // 处理delta内容，过滤finish标签但保留其他标签
                    String processedDelta = processAndFilterDelta(delta, fullDelta);
                    
                    if (!processedDelta.isEmpty()) {
                        // 记录处理后的状态
                        log.info("[{}] ✅ Delta处理完成 - 处理后长度: {}, 将发送到前端: {}", 
                            getCurrentTimestamp(), 
                            processedDelta.length(),
                            processedDelta.length() > 50 ? processedDelta.substring(0, 50) + "..." : processedDelta);
                        
                        // 发送处理后的内容到前端
                        long startTime = System.currentTimeMillis();
                        sendDeltaToFrontend(emitter, processedDelta);
                        long endTime = System.currentTimeMillis();
                        
                        aiReply.append(processedDelta);
                        log.info("[{}] 📤 Delta发送完成 - 耗时: {}ms, 累计回复长度: {}", 
                            getCurrentTimestamp(), 
                            (endTime - startTime), 
                            aiReply.length());
                    } else {
                        log.info("[{}] ⚠️  Delta处理后为空，跳过发送 - 原始内容: {}", 
                            getCurrentTimestamp(), 
                            delta.length() > 30 ? delta.substring(0, 30) + "..." : delta);
                    }
                } else {
                    log.info("[{}] ⚠️  接收到空的Delta内容，跳过处理", getCurrentTimestamp());
                }
            } else {
                log.debug("[{}] ⚠️  payload为空或无delta字段", getCurrentTimestamp());
            }
        }

        return false;
    }

    /**
     * 处理并过滤delta内容
     * 等待完整标签出现，过滤finish标签，保留其他标签
     */
    private String processAndFilterDelta(String delta, StringBuilder fullDelta) {
        log.info("[{}] 🔧 开始过滤Delta内容 - 输入长度: {}, 累积缓冲区长度: {}", 
            getCurrentTimestamp(), delta.length(), fullDelta.length());
        
        fullDelta.append(delta);
        String accumulated = fullDelta.toString();
        StringBuilder result = new StringBuilder();
        StringBuilder remaining = new StringBuilder();
        
        int tagCount = 0;
        int filteredTagCount = 0;
        int i = 0;
        
        // 如果缓冲区过大（超过50B），强制清空并输出内容，避免无限积累
        if (accumulated.length() > 50) {
            log.warn("[{}] ⚠️  缓冲区过大({} 字符)，强制清空并输出内容", 
                getCurrentTimestamp(), accumulated.length());
            fullDelta.setLength(0);
            // 简单过滤finish标签后直接返回
            String filteredContent = accumulated.replaceAll("(?i)</?finish>", "");
            log.info("[{}] 🔄 强制输出内容 - 长度: {}", getCurrentTimestamp(), filteredContent.length());
            return filteredContent;
        }
        
        while (i < accumulated.length()) {
            char c = accumulated.charAt(i);
            
            if (c == '<') {
                // 寻找完整的标签
                int tagStart = i;
                int tagEnd = findCompleteTag(accumulated, i);
                
                if (tagEnd == -1) {
                    // 检查是否真的是HTML标签开始，还是只是数学比较符号
                    String potentialTag = accumulated.substring(i, Math.min(i + 50, accumulated.length()));
                    
                    // 如果看起来不像HTML标签（没有字母跟在<后面，或者包含数学运算符），当作普通内容处理
                    if (!potentialTag.matches("<\\s*[a-zA-Z/].*") || potentialTag.matches(".*[<>=+\\-*/].*")) {
                        log.info("[{}] 🔍 检测到非HTML标签的<符号，当作普通内容处理: {}", 
                            getCurrentTimestamp(), potentialTag);
                        result.append(c);
                        i++;
                        continue;
                    }
                    
                    // 如果缓冲区中的不完整内容超过100个字符，可能不是真正的标签
                    if (accumulated.substring(i).length() > 100) {
                        log.warn("[{}] ⚠️  疑似非标签内容过长，强制输出: {}", 
                            getCurrentTimestamp(), accumulated.substring(i, Math.min(i + 30, accumulated.length())) + "...");
                        result.append(accumulated.substring(i));
                        break;
                    }
                    
                    // 标签不完整，保留到remaining中等待更多内容
                    remaining.append(accumulated.substring(i));
                    log.info("[{}] 📦 发现不完整标签，保留到缓冲区: {}", 
                        getCurrentTimestamp(), accumulated.substring(i, Math.min(i + 20, accumulated.length())) + "...");
                    break;
                } else {
                    // 找到完整标签
                    String tag = accumulated.substring(tagStart, tagEnd + 1);
                    tagCount++;
                    
                    // 检查是否是finish标签
                    if (tag.matches("(?i)</?finish>")) {
                        // 是finish标签，跳过不添加到结果中
                        filteredTagCount++;
                        log.info("[{}] 🚫 过滤finish标签: {}", getCurrentTimestamp(), tag);
                    } else {
                        // 不是finish标签，添加到结果中
                        result.append(tag);
                        log.info("[{}] ✅ 保留标签: {}", getCurrentTimestamp(), tag);
                    }
                    i = tagEnd + 1;
                }
            } else {
                // 普通字符，直接添加
                result.append(c);
                i++;
            }
        }
        
        // 更新fullDelta为剩余未处理的内容
        fullDelta.setLength(0);
        fullDelta.append(remaining);
        
        String resultStr = result.toString();
        log.info("[{}] 🏁 Delta过滤完成 - 处理标签: {}, 过滤标签: {}, 输出长度: {}, 剩余缓冲: {}", 
            getCurrentTimestamp(), tagCount, filteredTagCount, resultStr.length(), remaining.length());
        
        return resultStr;
    }

    /**
     * 寻找完整标签的结束位置
     */
    private int findCompleteTag(String text, int startPos) {
        if (startPos >= text.length() || text.charAt(startPos) != '<') {
            return -1;
        }
        
        // 寻找对应的'>'
        int pos = startPos + 1;
        boolean inQuotes = false;
        char quoteChar = 0;
        
        while (pos < text.length()) {
            char c = text.charAt(pos);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes && c == '>') {
                return pos; // 找到标签结束
            }
            
            pos++;
        }
        
        return -1; // 标签不完整
    }

    /**
     * 发送错误信息到前端
     */
    private void sendErrorToFrontend(SseEmitter emitter, String errorMessage) {
        try {
            String errorJson = String.format(
                "{\"type\":\"error\",\"message\":\"%s\"}",
                errorMessage.replace("\"", "\\\""));
            emitter.send(SseEmitter.event()
                .name("message")
                .data(errorJson)
                .reconnectTime(3000));
                
            // 强制刷新，确保数据立即发送
            try {
                java.lang.reflect.Field field = emitter.getClass().getDeclaredField("handler");
                field.setAccessible(true);
                Object handler = field.get(emitter);
                if (handler != null) {
                    java.lang.reflect.Method flushMethod = handler.getClass().getMethod("flush");
                    flushMethod.invoke(handler);
                }
            } catch (Exception flushEx) {
                log.debug("[{}] 无法强制刷新SSE缓冲区: {}", getCurrentTimestamp(), flushEx.getMessage());
            }
            
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 发送消息到前端
     */
    private void sendMessageToFrontend(SseEmitter emitter, String type, String message, Object extraData) {
        try {
            String messageJson;
            if (extraData != null) {
                messageJson = String.format(
                    "{\"type\":\"%s\",\"message\":\"%s\",\"data\":%s}",
                    type, message.replace("\"", "\\\""), extraData.toString());
            } else {
                messageJson = String.format(
                    "{\"type\":\"%s\",\"message\":\"%s\"}",
                    type, message.replace("\"", "\\\""));
            }
            
            // 使用统一的事件名和添加重连时间
            emitter.send(SseEmitter.event()
                .name("message")
                .data(messageJson)
                .reconnectTime(3000));
                
            // 强制刷新，确保数据立即发送
            try {
                java.lang.reflect.Field field = emitter.getClass().getDeclaredField("handler");
                field.setAccessible(true);
                Object handler = field.get(emitter);
                if (handler != null) {
                    java.lang.reflect.Method flushMethod = handler.getClass().getMethod("flush");
                    flushMethod.invoke(handler);
                }
            } catch (Exception flushEx) {
                log.debug("[{}] 无法强制刷新SSE缓冲区: {}", getCurrentTimestamp(), flushEx.getMessage());
            }
            
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 发送增量内容到前端
     */
    private void sendDeltaToFrontend(SseEmitter emitter, String deltaContent) {
        log.info("[{}] 📤 准备向前端发送增量内容 - 长度: {}, 内容: {}", 
            getCurrentTimestamp(), 
            deltaContent.length(),
            deltaContent.length() > 50 ? deltaContent.substring(0, 50) + "..." : deltaContent);
        
        try {
            // 清理和转义内容
            String cleanContent = deltaContent
                .replace("\\", "\\\\")   // 转义反斜杠
                .replace("\"", "\\\"")   // 转义双引号
                .replace("\n", "\\n")    // 转义换行符
                .replace("\r", "\\r")    // 转义回车符
                .replace("\t", "\\t");   // 转义制表符
            
            String deltaJson = String.format(
                "{\"type\":\"delta\",\"content\":\"%s\"}",
                cleanContent);
            
            log.debug("[{}] 🔄 准备发送SSE事件 - JSON长度: {}", getCurrentTimestamp(), deltaJson.length());
            
            // 发送SSE事件，使用delta事件名以提高兼容性
            emitter.send(SseEmitter.event()
                .name("message")
                .data(deltaJson)
                .reconnectTime(3000));  // 添加重连时间，提高稳定性
            
            // 强制刷新，确保数据立即发送（重要：解决服务器环境缓冲问题）
            try {
                // 通过反射访问响应对象进行强制刷新
                java.lang.reflect.Field field = emitter.getClass().getDeclaredField("handler");
                field.setAccessible(true);
                Object handler = field.get(emitter);
                if (handler != null) {
                    java.lang.reflect.Method flushMethod = handler.getClass().getMethod("flush");
                    flushMethod.invoke(handler);
                    log.debug("[{}] 🔄 强制刷新SSE缓冲区成功", getCurrentTimestamp());
                }
            } catch (Exception flushEx) {
                // 刷新失败不影响主流程，只记录debug日志
                log.debug("[{}] 📝 无法强制刷新SSE缓冲区: {}", getCurrentTimestamp(), flushEx.getMessage());
            }
            
            // 更新SSE活跃时间
            updateSseActivity(emitter);
            
            // 添加成功日志
            log.info("[{}] ✅ SSE增量数据发送成功 - 内容长度: {}, JSON长度: {}", 
                getCurrentTimestamp(), deltaContent.length(), deltaJson.length());
            
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("ResponseBodyEmitter has already completed")) {
                log.warn("[{}] ❌ SSE连接已完成，跳过发送增量内容 - 内容长度: {}", 
                    getCurrentTimestamp(), deltaContent.length());
            } else {
                log.warn("[{}] ❌ SSE连接状态异常，跳过发送增量内容 - 错误: {}, 内容长度: {}", 
                    getCurrentTimestamp(), e.getMessage(), deltaContent.length());
            }
        } catch (Exception e) {
            log.error("[{}] ❌ 发送增量内容失败 - 内容长度: {}, 错误: {}", 
                getCurrentTimestamp(), deltaContent.length(), e.getMessage(), e);
        }
    }

    /**
     * 发送完成信号到前端
     */
    private void sendCompleteToFrontend(SseEmitter emitter, String message, int recordId) {
        log.debug("[{}] 准备发送完成信号: recordId={}", getCurrentTimestamp(), recordId);
        
        try {
            String completeJson = String.format(
                "{\"type\":\"complete\",\"message\":\"%s\",\"recordId\":%d}",
                message, recordId);
            
            log.debug("[{}] 准备发送complete事件: {}", getCurrentTimestamp(), completeJson);
            emitter.send(SseEmitter.event()
                .name("message")
                .data(completeJson)
                .reconnectTime(3000));
                
            // 强制刷新，确保数据立即发送
            try {
                java.lang.reflect.Field field = emitter.getClass().getDeclaredField("handler");
                field.setAccessible(true);
                Object handler = field.get(emitter);
                if (handler != null) {
                    java.lang.reflect.Method flushMethod = handler.getClass().getMethod("flush");
                    flushMethod.invoke(handler);
                }
            } catch (Exception flushEx) {
                log.debug("[{}] 无法强制刷新SSE缓冲区: {}", getCurrentTimestamp(), flushEx.getMessage());
            }
            
            // 更新SSE活跃时间
            updateSseActivity(emitter);
            
            log.info("[{}] complete事件已成功发送，recordId: {}", getCurrentTimestamp(), recordId);
            
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("ResponseBodyEmitter has already completed")) {
                log.warn("[{}] SSE连接已完成，跳过发送完成信号: recordId={}", getCurrentTimestamp(), recordId);
            } else {
                log.warn("[{}] SSE连接状态异常，跳过发送完成信号 - 错误: {}, recordId={}", 
                    getCurrentTimestamp(), e.getMessage(), recordId);
            }
        } catch (Exception e) {
            log.error("[{}] 发送完成信号失败，recordId: {}", getCurrentTimestamp(), recordId, e);
        }
    }

    /**
     * 处理流程错误
     */
    private void handleProcessError(SseEmitter emitter, Exception e, Integer uid, Integer sid, int nextSeq) {
        try {
            // 保存错误消息
            String errMsg = "AI回复失败: " + e.getMessage();
            Record errRecord = new Record(sid, uid, false, errMsg, nextSeq + 1,
                MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
            recordRepository.save(errRecord);

            // 发送错误信息到前端  
            sendErrorToFrontend(emitter, e.getMessage());
        } catch (Exception sendError) {
            log.error("处理错误失败", sendError);
        }
    }



    /**
     * 处理带文件引用的AI流式回复（支持Agent类型和输入类型选择及metadata）
     */
    private void processAIStreamResponseWithFiles(SseEmitter emitter, Integer uid, Integer sid, String content, List<String> fileReferences, int nextSeq, String agentType, String inputType, java.util.Map<String, Object> metadata) {
        // 委托给已有的带参数的方法实现
        processAIStreamResponseWithFiles(emitter, uid, sid, content, fileReferences, nextSeq, agentType, inputType);
    }

    /**
     * 处理带文件引用的AI流式回复（支持Agent类型和输入类型选择）
     */
    private void processAIStreamResponseWithFiles(SseEmitter emitter, Integer uid, Integer sid, String content, List<String> fileReferences, int nextSeq, String agentType, String inputType) {
        StringBuilder aiReply = new StringBuilder();
        String userInputUrl = "http://localhost:8000/api/v1/user/input";

        log.info("[{}] 📁 开始处理带文件的AI流式回复 - sid: {}, agentType: {}, inputType: {}, fileCount: {}", 
                getCurrentTimestamp(), sid, agentType, inputType, fileReferences != null ? fileReferences.size() : 0);

        try {
            // 1. 准备文件引用信息
            List<String> validFileIds = new ArrayList<>();
            if (fileReferences != null && !fileReferences.isEmpty()) {
                for (String fileId : fileReferences) {
                    File file = fileRepository.findByFileId(fileId);
                    if (file != null && file.getUid().equals(uid) && file.getSid().equals(sid)) {
                        validFileIds.add(fileId);
                        log.info("[{}] ✅ 验证文件引用成功 - fileId: {}, fileName: {}", 
                                getCurrentTimestamp(), fileId, file.getOriginalName());
                    } else {
                        log.warn("[{}] ❌ 文件引用验证失败 - fileId: {}", getCurrentTimestamp(), fileId);
                    }
                }
            }

            // 2. 提交用户输入（包含文件引用）
            log.info("[{}] 📤 准备提交带文件的用户输入到后端API - sid: {}, 有效文件数: {}", 
                    getCurrentTimestamp(), sid, validFileIds.size());
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", content);
            inputRequest.put("input_type", inputType != null ? inputType : "question");
            if (!validFileIds.isEmpty()) {
                inputRequest.put("file_references", validFileIds);
            }
            
            // 添加metadata字段，包含agent_type
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("agent_type", agentType != null ? agentType : "orchestrator");
            
            // 如果是config类型，添加LLM配置
            String finalInputType = inputType != null ? inputType : "question";
            if ("config".equals(finalInputType)) {
                var llmConfig = new java.util.HashMap<String, Object>();
                llmConfig.put("api_key", openaiApiKey);
                llmConfig.put("base_url", openaiBaseUrl);
                llmConfig.put("model", openaiModel);
                metadata.put("llm_config", llmConfig);
                
                // 添加其他配置参数
                metadata.put("execution_model", executionModel);
                metadata.put("temperature", temperature);
                metadata.put("max_tokens", maxTokens);
            }
            
            inputRequest.put("metadata", metadata);

            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            log.info("[{}] 📥 带文件的用户输入提交完成 - sid: {}, response: {}", 
                    getCurrentTimestamp(), sid, inputResponse);

            if (inputResponse == null || !"success".equals(inputResponse.get("status"))) {
                log.error("[{}] ❌ 提交带文件的用户输入失败 - sid: {}, response: {}", 
                        getCurrentTimestamp(), sid, inputResponse);
                sendErrorToFrontend(emitter, "提交用户输入失败");
                return;
            }

            // 立即发送开始信号
            log.info("[{}] 🚀 发送开始信号到会话 - sid: {}", getCurrentTimestamp(), sid);
            sendMessageToFrontend(emitter, "start", "AI正在思考（正在处理" + validFileIds.size() + "个文件）...", null);

            // 3. 获取AI流式回复
            log.info("[{}] 🌐 开始建立SSE连接获取AI回复 - sid: {}", getCurrentTimestamp(), sid);
            String sessionId = sid.toString();
            String finalReply = getAIStreamResponse(emitter, sessionId, aiReply);
            log.info("[{}] 📥 带文件的AI流式回复获取完成 - sid: {}, 回复长度: {}", 
                    getCurrentTimestamp(), sid, finalReply != null ? finalReply.length() : 0);

            // 保存完整的AI回复
            Record aiRecord = null;
            if (finalReply != null && !finalReply.trim().isEmpty() && !finalReply.equals("AI回复为空")) {
                aiRecord = new Record(sid, uid, false, finalReply, nextSeq + 1,
                    MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                aiRecord = recordRepository.save(aiRecord);
                log.info("[{}] 💾 带文件的AI回复已保存 - recordId: {}, 内容长度: {}", 
                        getCurrentTimestamp(), aiRecord.getRid(), finalReply.length());
            } else {
                log.warn("[{}] ⚠️ 带文件的AI回复为空或无效，将使用默认回复 - sid: {}", getCurrentTimestamp(), sid);
                String defaultReply = aiReply.length() > 0 ? aiReply.toString() : "AI回复为空";
                aiRecord = new Record(sid, uid, false, defaultReply, nextSeq + 1,
                    MessageTypeConstant.LLM_GENERATION, LocalDateTime.now());
                aiRecord = recordRepository.save(aiRecord);
            }

            // 发送完成信号
            int recordId = aiRecord != null ? aiRecord.getRid() : -1;
            log.info("[{}] 🏁 发送完成信号到会话 - sid: {}, recordId: {}", getCurrentTimestamp(), sid, recordId);
            sendCompleteToFrontend(emitter, "回复完成", recordId);
            log.info("[{}] ✅ 带文件的流式处理完成，已发送complete事件 - sid: {}", getCurrentTimestamp(), sid);

        } catch (Exception e) {
            log.error("[{}] ❌ 带文件的SSE流式处理失败 - sid: {}, error: {}", getCurrentTimestamp(), sid, e.getMessage(), e);
            handleProcessError(emitter, e, uid, sid, nextSeq);
        } finally {
            // 对于会话级连接，不需要在这里关闭连接，由管理器统一管理
            log.info("[{}] 🔄 带文件的流式处理完成 - sid: {} (连接由会话管理器管理)", getCurrentTimestamp(), sid);
        }
    }

    @Override
    public SseEmitter sendConfigMessage(Integer uid, Integer sid, String agentType) {
        // 使用默认配置
        return sendConfigMessage(uid, sid, agentType, null, null, null);
    }

    @Override
    public SseEmitter sendConfigMessage(Integer uid, Integer sid, String agentType, String apiKey, String baseUrl, String model) {
        // 创建SSE发射器，短超时即可因为不需要等待流式回复
        SseEmitter emitter = new SseEmitter(10000L);

        // 更新会话时间
        updateSessionTime(uid, sid);

        // 异步处理配置发送
        CompletableFuture.runAsync(() -> {
            processConfigMessage(emitter, uid, sid, agentType, apiKey, baseUrl, model);
        });

        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("配置消息SSE连接超时");
            emitter.complete();
        });

        emitter.onError((throwable) -> {
            log.error("配置消息SSE连接错误", throwable);
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 处理配置消息发送（支持自定义配置，不等待流式回复）
     */
    private void processConfigMessage(SseEmitter emitter, Integer uid, Integer sid, String agentType, String customApiKey, String customBaseUrl, String customModel) {
        String userInputUrl = "http://localhost:8000/api/v1/user/input";
        int nextSeq = getNextSequence(sid);

        try {
            // 发送开始信号
            sendMessageToFrontend(emitter, "start", "正在配置LLM参数...", null);

            // 准备配置请求
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", "");
            inputRequest.put("input_type", "config");
            
            // 添加metadata，包含agent_type和LLM配置
            var metadata = new java.util.HashMap<String, Object>();
            metadata.put("agent_type", agentType != null ? agentType : "orchestrator");
            
            var llmConfig = new java.util.HashMap<String, Object>();
            // 使用自定义配置或默认配置
            llmConfig.put("api_key", customApiKey != null ? customApiKey : openaiApiKey);
            llmConfig.put("base_url", customBaseUrl != null ? customBaseUrl : openaiBaseUrl);
            llmConfig.put("model", customModel != null ? customModel : openaiModel);
            metadata.put("llm_config", llmConfig);
            
            // 添加其他配置参数
            metadata.put("execution_model", executionModel);
            metadata.put("temperature", temperature);
            metadata.put("max_tokens", maxTokens);
            
            inputRequest.put("metadata", metadata);

            // 保存配置消息到数据库
            Record configRecord = new Record();
            configRecord.setUid(uid);
            configRecord.setSid(sid);
            configRecord.setDirection(true); // 用户发起的配置
            configRecord.setContent(String.format("配置LLM: %s (Agent: %s)", 
                customModel != null ? customModel : openaiModel, 
                agentType != null ? agentType : "orchestrator"));
            configRecord.setSequence(nextSeq);
            configRecord.setType(MessageTypeConstant.CONFIG);
            configRecord.setCreateTime(LocalDateTime.now());
            recordRepository.save(configRecord);

            // 发送配置到LLM
            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            
            if (inputResponse != null && "success".equals(inputResponse.get("status"))) {
                // 配置成功
                sendCompleteToFrontend(emitter, "LLM配置已更新", configRecord.getRid());
                log.info("LLM配置发送成功 - uid: {}, sid: {}, model: {}", uid, sid, 
                    customModel != null ? customModel : openaiModel);
            } else {
                // 配置失败
                sendErrorToFrontend(emitter, "LLM配置更新失败");
                log.error("LLM配置发送失败 - uid: {}, sid: {}, response: {}", uid, sid, inputResponse);
            }

        } catch (Exception e) {
            log.error("发送配置消息失败", e);
            sendErrorToFrontend(emitter, "配置失败: " + e.getMessage());
        } finally {
            // 对于会话级连接，不需要在这里关闭连接，由管理器统一管理
            log.info("[{}] 🔄 配置消息处理完成 - sid: {} (连接由会话管理器管理)", getCurrentTimestamp(), sid);
        }
    }

    /**
     * 获取下一个序列号
     */
    private int getNextSequence(Integer sid) {
        List<Record> records = recordRepository.findBySidOrderBySequenceDesc(sid);
        return records.isEmpty() ? 1 : records.get(0).getSequence() + 1;
    }

    @Override
    public boolean sendMessageInput(Integer uid, Integer sid, String content, java.util.Map<String, Object> metadata, String inputType) {
        log.info("[{}] 📨 发送非流式消息 - uid: {}, sid: {}, inputType: {}, metadata: {}", 
                getCurrentTimestamp(), uid, sid, inputType, metadata);
        
        String userInputUrl = "http://localhost:8000/api/v1/user/input";
        
        try {
            // 更新会话时间，确保最新发送消息的会话显示在最上面
            updateSessionTime(uid, sid);

            // 构建发送给大模型的请求
            var inputRequest = new java.util.HashMap<String, Object>();
            inputRequest.put("session_id", sid.toString());
            inputRequest.put("user_id", uid.toString());
            inputRequest.put("input_text", content != null ? content : "");
            inputRequest.put("input_type", inputType != null ? inputType : "question");
            
            // 添加metadata，默认包含agent_type
            var requestMetadata = new java.util.HashMap<String, Object>();
            requestMetadata.put("agent_type", "orchestrator"); // 默认agent类型
            
            // 合并请求中的metadata
            if (metadata != null && !metadata.isEmpty()) {
                requestMetadata.putAll(metadata);
                log.info("[{}] 📋 合并请求metadata: {}", getCurrentTimestamp(), metadata);
            }
            
            // 如果是config类型，添加LLM配置
            if ("config".equals(inputType)) {
                var llmConfig = new java.util.HashMap<String, Object>();
                llmConfig.put("api_key", openaiApiKey);
                llmConfig.put("base_url", openaiBaseUrl);
                llmConfig.put("model", openaiModel);
                requestMetadata.put("llm_config", llmConfig);
                
                // 添加其他配置参数
                requestMetadata.put("execution_model", executionModel);
                requestMetadata.put("temperature", temperature);
                requestMetadata.put("max_tokens", maxTokens);
                
                log.info("[{}] 🔧 添加LLM配置到config请求", getCurrentTimestamp());
            }
            
            inputRequest.put("metadata", requestMetadata);

            log.info("[{}] 🚀 发送非流式请求到大模型 - URL: {}, Request: {}", 
                    getCurrentTimestamp(), userInputUrl, inputRequest);

            // 发送请求到大模型
            var inputResponse = restTemplate.postForObject(userInputUrl, inputRequest, java.util.Map.class);
            
            if (inputResponse == null) {
                log.error("[{}] ❌ 大模型响应为空", getCurrentTimestamp());
                return false;
            }
            
            log.info("[{}] ✅ 大模型响应: {}", getCurrentTimestamp(), inputResponse);
            
            // 检查响应状态
            if ("success".equals(inputResponse.get("status"))) {
                log.info("[{}] ✅ 非流式消息发送成功 - inputType: {}", getCurrentTimestamp(), inputType);
                return true;
            } else {
                log.error("[{}] ❌ 大模型返回错误状态: {}", getCurrentTimestamp(), inputResponse.get("status"));
                return false;
            }
            
        } catch (Exception e) {
            log.error("[{}] ❌ 发送非流式消息失败 - uid: {}, sid: {}, inputType: {}", 
                    getCurrentTimestamp(), uid, sid, inputType, e);
            return false;
        }
    }

    @Override
    public boolean stopSessionTimeout(Integer sid) {
        try {
            stopSessionTimeoutMonitoring(sid);
            log.info("[{}] 🛑 成功停止会话{}的超时监控", getCurrentTimestamp(), sid);
            return true;
        } catch (Exception e) {
            log.error("[{}] ❌ 停止会话{}超时监控失败", getCurrentTimestamp(), sid, e);
            return false;
        }
    }

    @Override
    public boolean restartSessionTimeout(Integer sid) {
        try {
            restartSessionTimeoutMonitoring(sid);
            log.info("[{}] 🔄 成功重启会话{}的超时监控", getCurrentTimestamp(), sid);
            return true;
        } catch (Exception e) {
            log.error("[{}] ❌ 重启会话{}超时监控失败", getCurrentTimestamp(), sid, e);
            return false;
        }
    }
}
