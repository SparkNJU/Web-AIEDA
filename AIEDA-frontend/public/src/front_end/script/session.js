// ===========================
// Session Management Module
// ===========================

// ===========================
// 连接状态管理
// ===========================

/**
 * 检查连接状态
 */
function checkConnectionStatus() {
    fetch(`${API_BASE}/api/health`)
    .then(response => {
        if (response.ok) {
            updateConnectionStatus(true);
        } else {
            updateConnectionStatus(false);
        }
    })
    .catch(error => {
        console.error('Connection check failed:', error);
        updateConnectionStatus(false);
    });
}

/**
 * 更新连接状态显示
 * @param {boolean} connected - 连接状态
 */
function updateConnectionStatus(connected) {
    isConnected = connected;
    const statusElement = document.getElementById('connectionStatus');
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');
    
    if (connected) {
        statusElement.textContent = '已连接';
        statusElement.classList.add('connected');
        statusDot.classList.add('ready');
        statusText.textContent = '就绪';
    } else {
        statusElement.textContent = '未连接';
        statusElement.classList.remove('connected');
        statusDot.classList.remove('ready', 'processing');
        statusText.textContent = '未连接';
    }
}

// ===========================
// 会话基础操作
// ===========================

/**
 * 创建新会话
 */
function createNewSession() {
    fetch(`${API_BASE}/api/session/create`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.session_id) {
            currentSessionId = data.session_id;
            currentTurnid = data.turn_id,
            updateConnectionStatus(true); // 设置为已连接
            updateSessionInfo();
            showToast('新会话已创建', 'success');
             // 刷新session列表
            refreshSessionList();
            // 更新左侧文件列表
            updateSidebarFilesList();
        } else {
            updateConnectionStatus(false);
            showToast('创建会话失败', 'error');
        }
    })
    .catch(error => {
        console.error('Create session error:', error);
        updateConnectionStatus(false);
        showToast('创建会话失败', 'error');
    });
}


/**
 * 获取会话信息
 */
function getSessionInfo() {
    const infoDiv = document.getElementById('getSessionInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取会话信息...', 'info');
    
    fetch(`${API_BASE}/api/session/${currentSessionId}`)
    .then(response => response.json())
    .then(data => {
        if (data.session_id) {
            const info = `会话ID: ${data.session_id}\n创建时间: ${data.created_at}\n状态: ${data.state}\n任务数: ${data.tasks.length}`;
            // 更新侧边栏的会话信息显示
            const infoElement = document.getElementById('sessionInfo');
            if (infoElement) {
                infoElement.textContent = info;
            }
            showTempInfo(infoDiv, '会话信息已更新', 'success');
        } else {
            showTempInfo(infoDiv, '获取会话信息失败', 'error');
        }
    })
    .catch(error => {
        console.error('Get session info error:', error);
        showTempInfo(infoDiv, '获取会话信息失败', 'error');
    });
}

/**
 * 更新会话信息显示
 */
function updateSessionInfo() {
    const infoElement = document.getElementById('sessionInfo');
    if (currentSessionId) {
        // 尝试获取详细的会话信息
        fetch(`${API_BASE}/api/session/${currentSessionId}`)
        .then(response => response.json())
        .then(data => {
            if (data.session_id) {
                const shortId = data.session_id.substring(0, 8) + '...';
                const time = new Date(data.created_at).toLocaleTimeString();
                const status = isConnected ? '✓' : '✗';
                infoElement.innerHTML = `<div class="compact-info"><span>ID: ${shortId}</span><span>时间: ${time}</span><span>状态: ${status}</span><span>任务: ${data.tasks.length}</span></div>`;
            } else {
                // 如果获取详细信息失败，显示基本信息
                const shortId = currentSessionId.substring(0, 8) + '...';
                const status = isConnected ? '✓' : '✗';
                infoElement.innerHTML = `<div class="compact-info"><span>ID: ${shortId}</span><span>状态: ${status}</span></div>`;
            }
        })
        .catch(error => {
            // 如果请求失败，显示基本信息
            const shortId = currentSessionId.substring(0, 8) + '...';
            const status = isConnected ? '✓' : '✗';
            infoElement.innerHTML = `<div class="compact-info"><span>ID: ${shortId}</span><span>状态: ${status}</span></div>`;
        });
    } else {
        infoElement.innerHTML = '<div class="compact-info"><span>等待创建会话...</span></div>';
    }
}

/**
 * 更新会话统计信息
 */
function updateSessionStats() {
    const statsElement = document.getElementById('sessionStats');
    const successRate = generationCount > 0 ? Math.round((successCount / generationCount) * 100) : 0;
    statsElement.innerHTML = `<div class="compact-stats"><span>消息: ${messageCount}</span><span>生成: ${generationCount}</span><span>成功率: ${successRate}%</span></div>`;
}

// ===========================
// 会话列表管理
// ===========================

// 列出所有会话
function listAllSessions() {
    const infoDiv = document.getElementById('listSessionsInfo');
    showTempInfo(infoDiv, '正在获取会话列表...', 'info');
    
    fetch(`${API_BASE}/api/sessions`)
    .then(response => response.json())
    .then(data => {
        if (Array.isArray(data)) {
            if (data.length === 0) {
                showTempInfo(infoDiv, '暂无会话', 'info');
                return;
            }
            
            let sessionList = `找到 ${data.length} 个会话`;
            showTempInfo(infoDiv, sessionList, 'success');
            
            // 仍然显示详细的会话列表弹窗
            let detailList = '会话列表:\n';
            data.forEach(session => {
                detailList += `- ${session.session_id} (${new Date(session.created_at).toLocaleString()})\n`;
            });
            showFloatingModal('会话列表', detailList);
        } else {
            showTempInfo(infoDiv, '获取会话列表失败', 'error');
        }
    })
    .catch(error => {
        console.error('List sessions error:', error);
        showTempInfo(infoDiv, '获取会话列表失败', 'error');
    });
}



// Session选择功能 - 自动刷新会话列表
function refreshSessionList() {
    const select = document.getElementById('sessionSelect');
    if (!select) return; // 如果元素不存在，直接返回
    
    fetch(`${API_BASE}/api/sessions`)
    .then(response => response.json())
    .then(data => {
        // 清空现有选项，保留默认选项
        select.innerHTML = '<option value="">-- 选择已有会话 --</option>';
        
        if (Array.isArray(data) && data.length > 0) {
            data.forEach(session => {
                const option = document.createElement('option');
                option.value = session.session_id;
                option.textContent = `${session.session_id.substring(0, 8)}... (${new Date(session.created_at).toLocaleString()})`;
                
                // 如果是当前会话，标记为选中
                if (session.session_id === currentSessionId) {
                    option.selected = true;
                }
                
                select.appendChild(option);
            });
        }
    })
    .catch(error => {
        console.error('Refresh sessions error:', error);
    });
}

/**
 * 显示会话列表
 * @param {Array} sessions - 会话列表数据
 */
function displaySessionsList(sessions) {
    const sessionsList = document.getElementById('sessionsList');
    if (!sessionsList) return;
    
    sessionsList.innerHTML = '';
    
    if (sessions.length === 0) {
        sessionsList.innerHTML = '<div class="no-sessions">暂无会话</div>';
        return;
    }
    
    sessions.forEach(session => {
        const sessionItem = document.createElement('div');
        sessionItem.className = 'session-item';
        if (session.session_id === currentSessionId) {
            sessionItem.classList.add('active');
        }
        
        const sessionInfo = document.createElement('div');
        sessionInfo.className = 'session-info';
        
        const sessionId = document.createElement('div');
        sessionId.className = 'session-id';
        sessionId.textContent = session.session_id;
        
        const sessionTime = document.createElement('div');
        sessionTime.className = 'session-time';
        sessionTime.textContent = new Date(session.created_at).toLocaleString();
        
        const sessionActions = document.createElement('div');
        sessionActions.className = 'session-actions';
        
        const switchBtn = document.createElement('button');
        switchBtn.className = 'btn btn-primary btn-sm';
        switchBtn.textContent = '切换';
        switchBtn.onclick = () => switchSession(session.session_id);
        
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.textContent = '删除';
        deleteBtn.onclick = () => deleteSession(session.session_id);
        
        sessionInfo.appendChild(sessionId);
        sessionInfo.appendChild(sessionTime);
        sessionActions.appendChild(switchBtn);
        sessionActions.appendChild(deleteBtn);
        sessionItem.appendChild(sessionInfo);
        sessionItem.appendChild(sessionActions);
        sessionsList.appendChild(sessionItem);
    });
}

// ===========================
// 会话切换与删除
// ===========================
function switchToSelectedSession() {
    const select = document.getElementById('sessionSelect');
    const sessionId = select.value;
    
    if (!sessionId) {
        return; // 用户选择了默认选项，不做任何操作
    }
    
    // 验证会话是否存在并切换
    fetch(`${API_BASE}/api/session/${sessionId}`)
    .then(response => {
        if (response.ok) {
            return response.json();
        } else if (response.status === 404) {
            throw new Error('会话不存在');
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    })
    .then(data => {
        currentSessionId = sessionId;
        updateSessionInfo();
        showToast(`已切换到会话: ${sessionId.substring(0, 8)}...`, 'success');
        
        // 清空聊天记录显示
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            chatMessages.innerHTML = '';
        }
        
        // 刷新文件列表
        listSessionFiles();
    })
    .catch(error => {
        console.error('Switch session error:', error);
        showToast(`切换会话失败: ${error.message}`, 'error');
    });
}


// 删除当前会话
function deleteCurrentSession() {
    const infoDiv = document.getElementById('deleteSessionInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    if (!confirm(`确定要删除会话 "${currentSessionId}" 吗？`)) {
        return;
    }
    
    showTempInfo(infoDiv, '正在删除会话...', 'info');
    
    fetch(`${API_BASE}/api/session/${currentSessionId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.message) {
            const deletedId = currentSessionId;
            currentSessionId = null;
            messageHistory = [];
            // 清空聊天记录显示
            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages) {
                chatMessages.innerHTML = '';
            }
            updateSessionInfo();
            showTempInfo(infoDiv, `会话 ${deletedId} 已删除`, 'success');
        } else {
            showTempInfo(infoDiv, '删除会话失败', 'error');
        }
    })
    .catch(error => {
        console.error('Delete session error:', error);
        showTempInfo(infoDiv, '删除会话失败', 'error');
    });
}

/**
 * 切换会话
 * @param {string} sessionId - 会话ID
 */
function switchSession(sessionId) {
    if (sessionId === currentSessionId) {
        return;
    }
    
    // 验证会话是否存在并切换
    fetch(`${API_BASE}/api/session/${sessionId}`)
    .then(response => {
        if (response.ok) {
            return response.json();
        } else if (response.status === 404) {
            throw new Error('会话不存在');
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    })
    .then(data => {
        currentSessionId = sessionId;
        updateSessionInfo();
        
        // 清空当前聊天内容
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            chatMessages.innerHTML = '';
        }
        
        // 重置消息历史
        messageHistory = [];
        
        // 更新文件列表
        updateSidebarFilesList();
        
        showToast(`已切换到会话: ${sessionId}`, 'success');
    })
    .catch(error => {
        console.error('Switch session error:', error);
        showToast(`切换会话失败: ${error.message}`, 'error');
        
        // 刷新会话列表，移除不存在的会话
        refreshSessionList();
    });
}

/**
 * 删除会话
 * @param {string} sessionId - 会话ID
 */
function deleteSession(sessionId) {
    if (!confirm(`确定要删除会话 ${sessionId} 吗？此操作不可撤销。`)) {
        return;
    }
    
    fetch(`${API_BASE}/api/session/${sessionId}/delete`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('会话删除成功', 'success');
            
            // 如果删除的是当前会话，清空当前会话ID
            if (sessionId === currentSessionId) {
                currentSessionId = null;
                updateSessionInfo();
                
                // 清空聊天内容
                const chatMessages = document.getElementById('chatMessages');
                if (chatMessages) {
                    chatMessages.innerHTML = '';
                }
                messageHistory = [];
            }
            
            // 刷新会话列表
            refreshSessionList();
        } else {
            showToast('删除会话失败', 'error');
        }
    })
    .catch(error => {
        console.error('Delete session error:', error);
        showToast('删除会话失败', 'error');
    });
}

/**
 * 清空当前会话
 */
function clearCurrentSession() {
    if (!currentSessionId) {
        showToast('当前没有活动会话', 'error');
        return;
    }
    
    if (!confirm('确定要清空当前会话的聊天记录吗？此操作不可撤销。')) {
        return;
    }
    
    // 清空聊天界面
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.innerHTML = '';
    }
    
    // 重置消息历史
    messageHistory = [];
    messageCount = 0;
    
    // 重置统计
    updateSessionStats();
    
    showToast('会话已清空', 'success');
}

// ===========================
// 会话任务管理
// ===========================

/**
 * 获取会话任务
 */
function getSessionTasks() {
    const infoDiv = document.getElementById('getSessionTasksInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取会话任务...', 'info');
    
    fetch(`${API_BASE}/api/session/${currentSessionId}/tasks`)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            displaySessionTasks(data.tasks);
            showTempInfo(infoDiv, `找到 ${data.tasks.length} 个任务`, 'success');
        } else {
            showTempInfo(infoDiv, '获取会话任务失败', 'error');
        }
    })
    .catch(error => {
        console.error('Get session tasks error:', error);
        showTempInfo(infoDiv, '获取会话任务失败', 'error');
    });
}

/**
 * 显示会话任务
 * @param {Array} tasks - 任务列表数据
 */
function displaySessionTasks(tasks) {
    const taskList = document.getElementById('sessionTasksList');
    taskList.innerHTML = '';
    
    if (tasks.length === 0) {
        taskList.innerHTML = '<div class="no-tasks">暂无任务</div>';
        return;
    }
    
    tasks.forEach(task => {
        const taskItem = document.createElement('div');
        taskItem.className = 'task-item';
        
        const taskInfo = document.createElement('div');
        taskInfo.className = 'task-info';
        
        const taskName = document.createElement('div');
        taskName.className = 'task-name';
        taskName.textContent = task.name;
        
        const taskStatus = document.createElement('div');
        taskStatus.className = `task-status ${task.status}`;
        taskStatus.textContent = task.status;
        
        taskInfo.appendChild(taskName);
        taskInfo.appendChild(taskStatus);
        
        taskItem.appendChild(taskInfo);
        taskList.appendChild(taskItem);
    });
}

// ===========================
// 会话状态管理
// ===========================

/**
 * 获取会话状态
 */
function getSessionStatus() {
    const infoDiv = document.getElementById('getSessionStatusInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取会话状态...', 'info');
    
    fetch(`${API_BASE}/api/session/${currentSessionId}/status`)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            displaySessionStatus(data.status);
            showTempInfo(infoDiv, '会话状态获取成功', 'success');
        } else {
            showTempInfo(infoDiv, '获取会话状态失败', 'error');
        }
    })
    .catch(error => {
        console.error('Get session status error:', error);
        showTempInfo(infoDiv, '获取会话状态失败', 'error');
    });
}

/**
 * 显示会话状态
 * @param {Object} status - 状态数据
 */
function displaySessionStatus(status) {
    const statusInfo = document.getElementById('sessionStatusInfo');
    statusInfo.innerHTML = `
        <div class="status-item">
            <span class="status-label">状态:</span>
            <span class="status-value ${status.state}">${status.state}</span>
        </div>
        <div class="status-item">
            <span class="status-label">创建时间:</span>
            <span class="status-value">${new Date(status.created_at).toLocaleString()}</span>
        </div>
        <div class="status-item">
            <span class="status-label">最后活动:</span>
            <span class="status-value">${new Date(status.last_activity).toLocaleString()}</span>
        </div>
    `;
}

// ===========================
// 会话输出管理
// ===========================

/**
 * 获取会话输出
 */
function getSessionOutput() {
    const infoDiv = document.getElementById('getSessionOutputInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取会话输出...', 'info');
    
    fetch(`${API_BASE}/session/${currentSessionId}/output`)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            displaySessionOutput(data.output);
            showTempInfo(infoDiv, '会话输出获取成功', 'success');
        } else {
            showTempInfo(infoDiv, '获取会话输出失败', 'error');
        }
    })
    .catch(error => {
        console.error('Get session output error:', error);
        showTempInfo(infoDiv, '获取会话输出失败', 'error');
    });
}

/**
 * 显示会话输出
 * @param {Array} output - 输出数据
 */
function displaySessionOutput(output) {
    const outputArea = document.getElementById('sessionOutputArea');
    outputArea.innerHTML = '';
    
    if (!output || output.length === 0) {
        outputArea.innerHTML = '<div class="no-output">暂无输出</div>';
        return;
    }
    
    output.forEach(item => {
        const outputItem = document.createElement('div');
        outputItem.className = 'output-item';
        
        const timestamp = document.createElement('div');
        timestamp.className = 'output-timestamp';
        timestamp.textContent = new Date(item.timestamp).toLocaleString();
        
        const content = document.createElement('div');
        content.className = 'output-content';
        content.textContent = item.content;
        
        outputItem.appendChild(timestamp);
        outputItem.appendChild(content);
        outputArea.appendChild(outputItem);
    });
    
    // 滚动到底部
    outputArea.scrollTop = outputArea.scrollHeight;
}