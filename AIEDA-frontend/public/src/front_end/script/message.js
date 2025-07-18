
// 停止生成
async function stopGeneration() {
    stopCurrentStream();
    if (!currentSessionId) {
        showToast('没有活动的会话', 'warning');
        return;
    }
    try {
        // Ensure 'currentTurnId' is defined and holds the correct turn ID for the stop request
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/stop?turn_id=${currentTurnId}`, {
            method: 'POST',
        });
        const data = await response.json();
        if (response.ok) {
            setGeneratingState(false);
            showToast(data.message || '生成已停止', 'info');
        } else {
            showToast(data.detail || '停止生成失败', 'error');
        }
    } catch (error) {
        console.error('Error stopping generation:', error);
        showToast('停止生成请求失败', 'error');
    } finally {
        setGeneratingState(false);
        updateSessionStats();
    }
}
// 消息处理相关功能
// 发送消息
async function sendMessage() {
    const input = document.getElementById('userInput');
    const message = input.value.trim();
    
    if (!message) {
        showToast('请输入消息内容', 'warning');
        return;
    }
    
    if (!currentSessionId) {
        showToast('请先创建或连接会话', 'warning');
        return;
    }
    
    if (isGenerating) {
        showToast('正在生成中，请稍候...', 'warning');
        return;
    }
    
    // 停止之前的流式响应
    stopCurrentStream();
    console.log('发送新消息前已停止之前的流式响应');
    
    try {
        // 准备文件信息
        const fileInfo = currentUploadedFiles.map(file => ({
            name: file.name,
            size: file.size,
            path: file.path,
            uploaded: file.uploaded
        }));
        
        // 添加用户消息到界面（包含文件附件）
        addMessage('user', message, null, fileInfo);
        messageHistory.push({role: 'user', content: message, timestamp: new Date().toISOString()});
        input.value = '';
        resetInputHeight();
        messageCount++;
        generationCount++;
        updateSessionStats();
        
        // 清空当前上传的文件显示
        clearCurrentUploadedFiles();
        
        // 设置生成状态
        setGeneratingState(true);
        
        // 添加调试信息
        console.log('当前streamMode值:', streamMode);
        console.log('streamMode类型:', typeof streamMode);
        
        if (streamMode) {
            console.log('进入流式模式处理');
            // 预先创建助手消息容器，提供即时反馈
            const assistantMessageElement = addMessage('assistant', 'connecting...');
            console.log('开始调用handleStreamResponseV3');
            
            try {
                // 先建立流式连接并等待连接确认
                await handleStreamResponseV3(currentSessionId, assistantMessageElement);
                
                // 文件信息已在开头准备
                send_message = JSON.stringify({
                    session_id: currentSessionId,
                    query: message,
                    source: "user",
                    turn_id: currentTurnid,
                    file_data: JSON.stringify(fileInfo),
                    data: {
                        stream: streamMode
                    }
                });
                console.log('send_message:', send_message);
                // 流式连接建立后，再发送process请求
                const processResponse = await fetch(`${API_BASE}/api/task/process`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: send_message
                });
               
                if (!processResponse.ok) {
                    console.warn(`Process请求HTTP错误: ${processResponse.status}`);
                } else {
                    console.log('Process请求发送成功');
                    const responseData = await processResponse.json(); // 解析JSON响应
                    if (currentTurnid === responseData.turn_id) {
                        currentTurnid = currentTurnid+1
                    } else {
                        currentTurnid = responseData.turn_id; // 从解析后的数据中获取turn_id
                    }
                    console.log('Updated currentTurnid (stream) to:', currentTurnid); // 添加日志确认更新
                }
                
            } catch (error) {
                console.error('流式处理或任务发送失败:', error);
                updateMessageContent(assistantMessageElement, '连接失败，请重试');
            }
            
        } else {
            // 非流式响应处理
            const response = await fetch(`${API_BASE}/api/task/process`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    session_id: currentSessionId,
                    query: message,
                    turn_id: currentTurnid,
                    source: "user",
                    file_data: JSON.stringify(fileInfo),
                    data: {
                        stream: streamMode
                    }
                })
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            if (data.result && data.result.content) {
                addMessage('assistant', data.result.content);
            } else if (data.result) {
                addMessage('assistant', JSON.stringify(data.result, null, 2));
            } else {
                addMessage('assistant', '任务已提交，状态: ' + data.status);
            }
            successCount++;
            currentTurnid = data.turn_id; // 从解析后的数据中获取turn_id
            console.log('Updated currentTurnid (non-stream) to:', currentTurnid); // 添加日志确认更新
        }
        
    } catch (error) {
        console.error('发送消息失败:', error);
        addMessage('system', `错误: ${error.message}`);
        showToast('发送消息失败: ' + error.message, 'error');
    } finally {
        setGeneratingState(false);
        updateSessionStats();
    }
}

// 清空当前上传的文件（发送消息后调用）
function clearCurrentUploadedFiles() {
    currentUploadedFiles = [];
    updateUploadedFilesDisplay();
}

// 停止当前流式响应的函数
function stopCurrentStream() {
    if (currentStreamReader) {
        try {
            currentStreamReader.cancel();
            console.log('已停止当前流式响应');
        } catch (error) {
            console.log('停止流式响应时出错:', error);
        }
        currentStreamReader = null;
    }
    
    if (currentStreamController) {
        try {
            currentStreamController.abort();
            console.log('已中止当前流式请求');
        } catch (error) {
            console.log('中止流式请求时出错:', error);
        }
        currentStreamController = null;
    }
}

// 处理流式响应V3

async function handleStreamResponseV3(sessionId, assistantMessageElement = null) {
    console.log('handleStreamResponseV3被调用，sessionId:', sessionId);
    console.log('assistantMessageElement:', assistantMessageElement);
    
    // 确保传入了有效的assistantMessageElement，避免重复创建
    if (!assistantMessageElement) {
        console.error('错误：handleStreamResponseV3未接收到有效的assistantMessageElement');
        throw new Error('assistantMessageElement不能为null，必须由调用者提供');
    }
    
    // 创建新的AbortController
    currentStreamController = new AbortController();
    
    try {
        console.log('开始发起流式请求到:', `${API_BASE}/api/session/${sessionId}/stream`);
        const response = await fetch(`${API_BASE}/api/session/${sessionId}/stream`, {
            method: 'GET',
            headers: {
                'Accept': 'text/event-stream',
                'Cache-Control': 'no-cache'
            },
            signal: currentStreamController.signal
        });
        
        console.log('流式请求响应状态:', response.status, response.statusText);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        // 连接建立成功，更新消息状态
        updateMessageContent(assistantMessageElement, 'connected, waiting for response...');
        console.log('流式连接已建立，准备开始数据处理');
        
        // 启动异步数据处理，不阻塞返回
        processStreamData(response, assistantMessageElement, sessionId);
        
        // 立即返回，表示连接已建立
        return Promise.resolve();
        
    } catch (error) {
        console.error('流式连接建立失败:', error);
        if (assistantMessageElement) {
            updateMessageContent(assistantMessageElement, '连接失败: ' + error.message);
        }
        throw error;
    }
}
 // 异步处理流式数据
 async function processStreamData(response, assistantMessageElement, sessionId) {
    let fullResponse = '';
    // 记录完整响应内容到日志

    // 验证消息元素是否有效
    console.log('processStreamData开始，assistantMessageElement ID:', assistantMessageElement?.dataset?.messageId);
    
    if (!assistantMessageElement) {
        console.error('processStreamData: assistantMessageElement为null，无法处理流式数据');
        return;
    }

    try {
        const reader = response.body.getReader();
        currentStreamReader = reader; // 保存当前读取器引用
        const decoder = new TextDecoder();
        
        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                console.log('流式读取完成');
                break;
            }
            
            const chunk = decoder.decode(value, { stream: true });
            // console.log('接收到数据块:', chunk);
            const lines = chunk.split('\n');
            
            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    const data = line.slice(6);
                    if (data === '') continue;
                    
                    // console.log('解析数据:', data);
                    try {
                        const parsed = JSON.parse(data);
                        //console.log('解析结果:', parsed);
                        
                        if (parsed.chunk) {
                            // console.log('接收到chunk:', parsed.chunk);
                            // 跳过主代理的消息
                            // if (parsed.source === 'main_agent') {
                            //     continue;
                            // }
                            // 如果是进度条消息，删除最后一个字符以更新进度
                            if (parsed.context === 'processing_bar') {
                                // 获取最后一行内容
                                const lines = fullResponse.split('\n');
                                // 删除最后一行
                                if (lines.length > 0) { // 确保数组不为空
                                    lines.pop(); // 删除数组的最后一个元素
                                    fullResponse = lines.join('\n');
                                }
                                // 记录进度条消息前的最后一行响应
                                fullResponse += '\n<Processing>' + parsed.chunk.replace(/\n/g, '') + '</Processing>';
                            } else {
                                fullResponse += parsed.chunk;
                            }
                            //console.log('更新流式内容，目标消息ID:', assistantMessageElement?.dataset?.messageId, 'chunk长度:', parsed.chunk.length);
                            updateMessageContent(assistantMessageElement, fullResponse, parsed.source);
                            
                            if (autoScroll) {
                                scrollToBottom();
                            }
                        } else if (parsed.error) {
                            throw new Error(parsed.error);
                        }
                    } catch (parseError) {
                        console.warn('解析流式数据失败:', parseError);
                    }
                } else if (line.startsWith('event: end')) {
                    break;
                }
            }
        }
        
        successCount++;
        console.log('流式数据处理完成');
        
    } catch (error) {
        console.error('流式数据处理失败:', error);
        if (assistantMessageElement) {
            updateMessageContent(assistantMessageElement, fullResponse + '\n\n[响应中断: ' + error.message + ']');
        }
    } finally {
        // 清理流式状态
        currentStreamReader = null;
        setGeneratingState(false);
        console.log('流式数据处理结束，已清理状态');
    }
}

// 设置生成状态
function setGeneratingState(generating) {
    isGenerating = generating;
    const sendBtn = document.getElementById('sendBtn');
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');
    
    if (generating) {
        sendBtn.disabled = false;
        sendBtn.textContent = '停止'; // 将按钮文本改为“停止”
        sendBtn.classList.add('stop-btn'); // 添加停止按钮样式 (可选)
        statusDot.classList.remove('ready');
        statusDot.classList.add('processing');
        statusText.textContent = '生成中...';
    } else {
        sendBtn.disabled = false;
        sendBtn.textContent = '发送';
        sendBtn.classList.remove('stop-btn'); // 移除停止按钮样式 (可选)
        statusDot.classList.remove('processing');
        statusDot.classList.add('ready');
        statusText.textContent = '就绪';
    }
}
// 添加消息到聊天界面
function addMessage(sender, content, timestamp = null, attachments = null) {
    const messagesContainer = document.getElementById('chatMessages');
    
    // 如果存在欢迎页面，先移除它
    const welcomeContainer = document.getElementById('welcomeContainer');
    if (welcomeContainer) {
        welcomeContainer.remove();
    }
    
    const messageElement = document.createElement('div');
    messageElement.className = `message ${sender}-message`;
    messageElement.style.whiteSpace = "pre-wrap";
    
    // 添加唯一标识用于调试
    messageElement.dataset.messageId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    console.log('创建新消息，类型:', sender, '消息ID:', messageElement.dataset.messageId);
    
    const time = timestamp || new Date().toLocaleTimeString();
    const avatar = sender === 'user' ? '👤' : sender === 'assistant' ? '🤖' : '⚠️';
    const senderName = sender === 'user' ? '用户' : sender === 'assistant' ? 'AI助手' : '系统';
    
    // 生成文件附件HTML
    let attachmentsHtml = '';
    if (attachments && attachments.length > 0) {
        attachmentsHtml = `
            <div class="message-attachments">
                ${attachments.map(file => `
                    <div class="attachment-item">
                        <div class="attachment-icon">📄</div>
                        <div class="attachment-info">
                            <div class="attachment-name">${file.name}</div>
                            <div class="attachment-size">${formatFileSize(file.size)}</div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    messageElement.innerHTML = `
        <div class="message-header">
            <div class="message-avatar ${sender}-avatar">${avatar}</div>
            <div class="message-info">
                <div class="message-sender">${senderName}</div>
                <div class="message-time">${time}</div>
            </div>
            <div class="message-actions">
                <button class="copy-message-btn" onclick="copyMessageContent(this)" title="复制消息内容">
                    <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg" class="icon-xs">
                        <path d="M12.668 10.667C12.668 9.95614 12.668 9.46258 12.6367 9.0791C12.6137 8.79732 12.5758 8.60761 12.5244 8.46387L12.4688 8.33399C12.3148 8.03193 12.0803 7.77885 11.793 7.60254L11.666 7.53125C11.508 7.45087 11.2963 7.39395 10.9209 7.36328C10.5374 7.33197 10.0439 7.33203 9.33301 7.33203H6.5C5.78896 7.33203 5.29563 7.33195 4.91211 7.36328C4.63016 7.38632 4.44065 7.42413 4.29688 7.47559L4.16699 7.53125C3.86488 7.68518 3.61186 7.9196 3.43555 8.20703L3.36524 8.33399C3.28478 8.49198 3.22795 8.70352 3.19727 9.0791C3.16595 9.46259 3.16504 9.95611 3.16504 10.667V13.5C3.16504 14.211 3.16593 14.7044 3.19727 15.0879C3.22797 15.4636 3.28473 15.675 3.36524 15.833L3.43555 15.959C3.61186 16.2466 3.86474 16.4807 4.16699 16.6348L4.29688 16.6914C4.44063 16.7428 4.63025 16.7797 4.91211 16.8027C5.29563 16.8341 5.78896 16.835 6.5 16.835H9.33301C10.0439 16.835 10.5374 16.8341 10.9209 16.8027C11.2965 16.772 11.508 16.7152 11.666 16.6348L11.793 16.5645C12.0804 16.3881 12.3148 16.1351 12.4688 15.833L12.5244 15.7031C12.5759 15.5594 12.6137 15.3698 12.6367 15.0879C12.6681 14.7044 12.668 14.211 12.668 13.5V10.667ZM13.998 12.665C14.4528 12.6634 14.8011 12.6602 15.0879 12.6367C15.4635 12.606 15.675 12.5492 15.833 12.4688L15.959 12.3975C16.2466 12.2211 16.4808 11.9682 16.6348 11.666L16.6914 11.5361C16.7428 11.3924 16.7797 11.2026 16.8027 10.9209C16.8341 10.5374 16.835 10.0439 16.835 9.33301V6.5C16.835 5.78896 16.8341 5.29563 16.8027 4.91211C16.7797 4.63025 16.7428 4.44063 16.6914 4.29688L16.6348 4.16699C16.4807 3.86474 16.2466 3.61186 15.959 3.43555L15.833 3.36524C15.675 3.28473 15.4636 3.22797 15.0879 3.19727C14.7044 3.16593 14.211 3.16504 13.5 3.16504H10.667C9.9561 3.16504 9.46259 3.16595 9.0791 3.19727C8.79739 3.22028 8.6076 3.2572 8.46387 3.30859L8.33399 3.36524C8.03176 3.51923 7.77886 3.75343 7.60254 4.04102L7.53125 4.16699C7.4508 4.32498 7.39397 4.53655 7.36328 4.91211C7.33985 5.19893 7.33562 5.54719 7.33399 6.00195H9.33301C10.022 6.00195 10.5791 6.00131 11.0293 6.03809C11.4873 6.07551 11.8937 6.15471 12.2705 6.34668L12.4883 6.46875C12.984 6.7728 13.3878 7.20854 13.6533 7.72949L13.7197 7.87207C13.8642 8.20859 13.9292 8.56974 13.9619 8.9707C13.9987 9.42092 13.998 9.97799 13.998 10.667V12.665ZM18.165 9.33301C18.165 10.022 18.1657 10.5791 18.1289 11.0293C18.0961 11.4302 18.0311 11.7914 17.8867 12.1279L17.8203 12.2705C17.5549 12.7914 17.1509 13.2272 16.6553 13.5313L16.4365 13.6533C16.0599 13.8452 15.6541 13.9245 15.1963 13.9619C14.8593 13.9895 14.4624 13.9935 13.9951 13.9951C13.9935 14.4624 13.9895 14.8593 13.9619 15.1963C13.9292 15.597 13.864 15.9576 13.7197 16.2939L13.6533 16.4365C13.3878 16.9576 12.9841 17.3941 12.4883 17.6982L12.2705 17.8203C11.8937 18.0123 11.4873 18.0915 11.0293 18.1289C10.5791 18.1657 10.022 18.165 9.33301 18.165H6.5C5.81091 18.165 5.25395 18.1657 4.80371 18.1289C4.40306 18.0962 4.04235 18.031 3.70606 17.8867L3.56348 17.8203C3.04244 17.5548 2.60585 17.151 2.30176 16.6553L2.17969 16.4365C1.98788 16.0599 1.90851 15.6541 1.87109 15.1963C1.83431 14.746 1.83496 14.1891 1.83496 13.5V10.667C1.83496 9.978 1.83432 9.42091 1.87109 8.9707C1.90851 8.5127 1.98772 8.10625 2.17969 7.72949L2.30176 7.51172C2.60586 7.0159 3.04236 6.6122 3.56348 6.34668L3.70606 6.28027C4.04237 6.136 4.40303 6.07083 4.80371 6.03809C5.14051 6.01057 5.53708 6.00551 6.00391 6.00391C6.00551 5.53708 6.01057 5.14051 6.03809 4.80371C6.0755 4.34588 6.15483 3.94012 6.34668 3.56348L6.46875 3.34473C6.77282 2.84912 7.20856 2.44514 7.72949 2.17969L7.87207 2.11328C8.20855 1.96886 8.56979 1.90385 8.9707 1.87109C9.42091 1.83432 9.978 1.83496 10.667 1.83496H13.5C14.1891 1.83496 14.746 1.83431 15.1963 1.87109C15.6541 1.90851 16.0599 1.98788 16.4365 2.17969L16.6553 2.30176C17.151 2.60585 17.5548 3.04244 17.8203 3.56348L17.8867 3.70606C18.031 4.04235 18.0962 4.40306 18.1289 4.80371C18.1657 5.25395 18.165 5.81091 18.165 6.5V9.33301Z"></path>
                    </svg>
                </button>
            </div>
        </div>
        ${attachmentsHtml}
        <div class="message-content">${formatMessageContent(content)}</div>
    `;
    
    messagesContainer.appendChild(messageElement);
    
    if (autoScroll) {
        scrollToBottom();
    }
    
    return messageElement;
}

// 更新消息内容
function updateMessageContent(messageElement, content, source = null) {
    // 添加调试信息，确保更新的是正确的消息元素
    if (!messageElement) {
        console.error('updateMessageContent: messageElement为null或undefined');
        return;
    }
    
    const contentElement = messageElement.querySelector('.message-content');
    if (!contentElement) {
        console.error('updateMessageContent: 找不到.message-content元素');
        return;
    }
    
    // 添加消息元素标识，用于调试
    if (!messageElement.dataset.messageId) {
        messageElement.dataset.messageId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
    
    console.log('更新消息内容，消息ID:', messageElement.dataset.messageId, '内容长度:', content.length);
    
    // // 如果有source信息，添加source标识
    // if (source && messageElement.classList.contains('assistant-message')) {
    //     // 排除系统消息和主代理消息
    //     if (source !== `system` && source !== `main_agent`) {
    //         // console.log(`信息来源 ${source}`)
    //         const sourceElement = messageElement.querySelector('.message-source') || createSourceElement(source);
    //         if (!messageElement.querySelector('.message-source')) {
    //             const messageHeader = messageElement.querySelector('.message-header');
    //             messageHeader.appendChild(sourceElement);
    //         }
    //         updateSourceElement(sourceElement, source);
    //     }
    // }
    
    contentElement.innerHTML = formatMessageContent(content);
}

// 创建source标识元素
function createSourceElement(source) {
    const sourceElement = document.createElement('div');
    sourceElement.className = 'message-source';
    sourceElement.textContent = source || 'system';
    sourceElement.style.cssText = `
        display: inline-block;
        padding: 2px 6px;
        margin-left: 8px;
        border-radius: 10px;
        font-size: 10px;
        font-weight: bold;
        text-transform: uppercase;
    `;
    return sourceElement;
}

// 更新source标识
function updateSourceElement(sourceElement, source) {
    sourceElement.textContent = source || 'system';
    
    // 根据不同source设置不同颜色
    const sourceColors = {
        'main_agent': '#4CAF50',
        'netlist_agent': '#2196F3', 
        'system': '#FF9800',
        'error': '#F44336'
    };
    
    const color = sourceColors[source] || '#9E9E9E';
    sourceElement.style.backgroundColor = color;
    sourceElement.style.color = 'white';
}

// 格式化消息内容
function formatMessageContent(content) {
    if (!content) return '';
    
    // 统一处理所有代码块（XML和Markdown风格）
    return processAllCodeBlocks(content);
}

// 统一的代码块处理函数
function processAllCodeBlocks(content) {
    // 1. 处理XML风格的代码块 <language>code</language>
    content = content.replace(CODE_REGEX.xmlCodeBlock, (match, language, code) => {
        return CODE_BLOCK_TEMPLATES.createCodeBlock(language, code, true);
    });
    
    // 2. 处理Markdown风格的代码块 ```language\ncode\n```
    content = content.replace(CODE_REGEX.markdownCodeBlock, (match, start, contentWithLang, end) => {
        const lines = contentWithLang.split('\n');
        const firstLine = lines[0] || '';
        const hasLanguage = firstLine.trim() && !firstLine.includes(' ') && firstLine.length < 20;
        
        if (hasLanguage) {
            const language = firstLine.trim();
            const code = lines.slice(1).join('\n');
            return CODE_BLOCK_TEMPLATES.createCodeBlock(language, code);
        }
        
        // 没有语言标识的保持简单结构
        return `<pre><code class="code-block">${contentWithLang}</code></pre>`;
    });

    // 3. 处理行内代码
    content = content.replace(CODE_REGEX.inlineCode, (match, ticks, code) => {
        return `<code class="inline-code">${code}</code>`;
    });

    return content;
}
// HTML 转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 防抖复制代码
const debouncedCopyCode = debounce(copyCode, 300);

// 优化的复制代码块内容函数
function copyCode(button) {
    // 防止重复点击
    if (button.disabled) return;
    
    const codeBlock = button.closest('.code-block');
    const preElement = codeBlock.querySelector('.code-content pre');

    if (!preElement) {
        console.warn("未找到 <pre> 元素");
        return;
    }

    // 禁用按钮防止重复点击
    button.disabled = true;
    const codeContent = preElement.textContent;

    navigator.clipboard.writeText(codeContent).then(() => {
        // 使用模板图标
        button.innerHTML = `${CODE_BLOCK_TEMPLATES.copiedIcon} 已复制`;
        
        setTimeout(() => {
            button.innerHTML = `${CODE_BLOCK_TEMPLATES.copyIcon} 复制`;
            button.disabled = false;
        }, 2000);
    }).catch(err => {
        console.error('复制失败:', err);
        showToast('复制失败', 'error');
        button.disabled = false;
    });
}

// 统一的复制内容函数
function copyUnifiedContent(button, content) {
    // 防止重复点击
    if (button.disabled) return;
    
    button.disabled = true;
    
    // 如果content是字符串，直接使用；否则从DOM获取
    let textContent = content;
    if (typeof content !== 'string') {
        const messageElement = button.closest('.message');
        const messageContent = messageElement.querySelector('.message-content');
        textContent = messageContent.textContent || messageContent.innerText;
        textContent = textContent.replace(/\s+/g, ' ').trim();
    }
    
    navigator.clipboard.writeText(textContent).then(() => {
        button.innerHTML = `${CODE_BLOCK_TEMPLATES.copiedIcon} 已复制`;
        
        setTimeout(() => {
            button.innerHTML = `${CODE_BLOCK_TEMPLATES.copyIcon} 复制`;
            button.disabled = false;
        }, 2000);
    }).catch(err => {
        console.error('复制失败:', err);
        showToast('复制失败', 'error');
        button.disabled = false;
    });
}

// 兼容旧的copyMessageContent函数
function copyMessageContent(button) {
    copyUnifiedContent(button);
}

// 滚动到底部
function scrollToBottom() {
    if (!autoScroll) return;
    
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}

// 处理键盘事件
function handleKeyDown(event) {
    if (event.key === 'Enter') {
        if (event.shiftKey) {
            // Shift+Enter 换行
            return;
        } else {
            // Enter 发送消息
            event.preventDefault();
            sendMessage();
        }
    }
}

// 重置输入框高度
function resetInputHeight() {
    const input = document.getElementById('userInput');
    input.style.height = 'auto';
    // input.style.height = Math.min(input.scrollHeight, 150) + 'px';
}


// 调整文本区域高度
function adjustTextareaHeight(textarea) {
    // textarea.style.height = 'auto';
    // textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px';
    resetInputHeight()
}


// 添加消息操作按钮
// function addMessageActions(messageElement, content) {
//     const actions = document.createElement('div');
//     actions.className = 'message-actions';
    
//     // 复制按钮
//     const copyBtn = document.createElement('button');
//     copyBtn.className = 'action-btn copy-btn';
//     copyBtn.innerHTML = `${CODE_BLOCK_TEMPLATES.copyIcon} 复制`;
//     copyBtn.title = '复制消息';
//     copyBtn.onclick = () => copyUnifiedContent(copyBtn, content);
    
//     // 查看按钮（用于长内容）
//     if (content.length > 500) {
//         const viewBtn = document.createElement('button');
//         viewBtn.className = 'action-btn';
//         viewBtn.innerHTML = '👁️';
//         viewBtn.title = '查看完整内容';
//         viewBtn.onclick = () => showFloatingModal('消息内容', content);
//         actions.appendChild(viewBtn);
//     }
    
//     actions.appendChild(copyBtn);
//     messageElement.insertBefore(actions, messageElement.firstChild);
// }

// 导出聊天历史为 Markdown
function exportChatHistory() {
    if (messageHistory.length === 0) {
        showToast('暂无聊天记录', 'warning');
        return;
    }
    
    let markdown = `# 聊天记录\n\n`;
    markdown += `**会话ID:** ${currentSessionId || '未知'}\n\n`;
    markdown += `**导出时间:** ${new Date().toLocaleString()}\n\n`;
    markdown += `---\n\n`;
    
    messageHistory.forEach((msg, index) => {
        const role = msg.role === 'user' ? '用户' : '助手';
        const timestamp = msg.timestamp ? msg.timestamp.toLocaleString() : '未知时间';
        
        markdown += `## ${role} (${timestamp})\n\n`;
        
        if (msg.attachments && msg.attachments.length > 0) {
            markdown += `**附件:**\n`;
            msg.attachments.forEach(file => {
                markdown += `- ${file.name} (${formatFileSize(file.size)})\n`;
            });
            markdown += `\n`;
        }
        
        markdown += `${msg.content}\n\n`;
        markdown += `---\n\n`;
    });
    
    // 下载文件
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `chat_history_${currentSessionId || 'unknown'}_${new Date().toISOString().slice(0, 10)}.md`;
    link.click();
    
    showToast('聊天记录已导出', 'success');
}