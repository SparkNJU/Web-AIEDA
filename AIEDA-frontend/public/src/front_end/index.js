// 主入口文件 - 整合所有模块

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('应用正在初始化...');
    
    // 初始化应用设置
    initializeApp();
    
    // 绑定事件监听器
    bindEventListeners();
    
    // 添加全局按钮点击监听器
    document.addEventListener('click', function(event) {
        if (event.target.tagName === 'BUTTON') {
            lastClickedButton = event.target;
        }
    });
    
    console.log('应用初始化完成');
});

// 初始化应用
function initializeApp() {
    // 初始化主题
    initializeTheme();
    
    // 检查是否有消息，没有消息时显示欢迎页面
    const chatMessages = document.getElementById('chatMessages');
    if (!welcomeShown && (!chatMessages || chatMessages.children.length === 0)) {
        addWelcomeMessage();
        welcomeShown = true;
    }
    
    // 检查连接状态
    checkConnectionStatus();
    
    // 创建会话
    createNewSession();
    
    // 调整文本区域高度
    adjustTextareaHeight();
    
    // 加载应用设置（来自 ui.js）
    loadAppSettings();
    
    // 初始化响应式布局
    handleResponsiveLayout();
    
    // 更新会话信息
    updateSessionInfo();
    
    // 更新文件列表
    updateSidebarFilesList();
    
    // 延迟刷新session列表，确保页面元素已加载
    setTimeout(() => {
        refreshSessionList();
    }, 1000);
}

// 绑定事件监听器
function bindEventListeners() {
    // 键盘事件
    const userInput = document.getElementById('userInput');
    if (userInput) {
        userInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        
        // 输入框自适应
        userInput.addEventListener('input', adjustTextareaHeight);
    }
    
    // 发送按钮
    const sendBtn = document.getElementById('sendBtn');
    if (sendBtn) {
        sendBtn.addEventListener('click', function() {
            if (isGenerating) {
                stopGeneration();
            } else {
                sendMessage();
            }
        });
    }
    
    // 主题切换
    const themeToggle = document.querySelector('.theme-toggle');
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }
    
    // 数据管理按钮
    const getStreamDataBtn = document.getElementById('getStreamDataBtn');
    if (getStreamDataBtn) {
        getStreamDataBtn.addEventListener('click', getStreamData);
    }
    
    const clearStreamDataBtn = document.getElementById('clearStreamDataBtn');
    if (clearStreamDataBtn) {
        clearStreamDataBtn.addEventListener('click', clearStreamData);
    }
    
    const exportChatBtn = document.getElementById('exportChatBtn');
    if (exportChatBtn) {
        exportChatBtn.addEventListener('click', exportChatHistory);
    }
    
    // 文件管理按钮
    const listFilesBtn = document.getElementById('listFilesBtn');
    if (listFilesBtn) {
        listFilesBtn.addEventListener('click', listSessionFiles);
    }
    
    const cleanFilesBtn = document.getElementById('cleanFilesBtn');
    if (cleanFilesBtn) {
        cleanFilesBtn.addEventListener('click', clearOldFiles);
    }
    
    const downloadLatestBtn = document.getElementById('downloadLatestBtn');
    if (downloadLatestBtn) {
        downloadLatestBtn.addEventListener('click', quickDownloadNetlist);
    }
    
    // 文件上传功能
    const attachmentBtn = document.getElementById('attachmentBtn');
    const fileUpload = document.getElementById('fileUpload');
    
    if (attachmentBtn && fileUpload) {
        let isUploading = false;
        
        // 点击回形针按钮触发文件选择
        attachmentBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (isUploading) {
                showToast('文件正在上传中，请稍候...', 'warning');
                return;
            }
            fileUpload.click();
        });
        
        // 文件选择后自动上传
        fileUpload.addEventListener('change', async (event) => {
            if (isUploading) return;
            isUploading = true;
            attachmentBtn.style.opacity = '0.5';
            attachmentBtn.style.pointerEvents = 'none';
            
            try {
                await handleFileUpload(event);
            } finally {
                isUploading = false;
                attachmentBtn.style.opacity = '1';
                attachmentBtn.style.pointerEvents = 'auto';
            }
        });
    }
    
    // 侧边栏切换
    const toggleLeftSidebarBtn = document.getElementById('toggleLeftSidebar');
    if (toggleLeftSidebarBtn) {
        toggleLeftSidebarBtn.addEventListener('click', toggleLeftSidebar);
    }
    
    const toggleRightSidebarBtn = document.getElementById('toggleRightSidebar');
    if (toggleRightSidebarBtn) {
        toggleRightSidebarBtn.addEventListener('click', toggleRightSidebar);
    }
    
    // 自动滚动切换
    const autoScrollSelect = document.getElementById('autoScroll');
    if (autoScrollSelect) {
        autoScrollSelect.addEventListener('change', function() {
            const enabled = this.value === 'true';
            setAutoScroll(enabled);
        });
    }
    
    // 流模式切换
    const streamToggle = document.getElementById('streamToggle');
    if (streamToggle) {
        streamToggle.addEventListener('click', toggleStreamMode);
    }
    
    // 会话管理按钮
    const createSessionBtn = document.getElementById('createSessionBtn');
    if (createSessionBtn) {
        createSessionBtn.addEventListener('click', createNewSession);
    }
    
    const getSessionInfoBtn = document.getElementById('getSessionInfoBtn');
    if (getSessionInfoBtn) {
        getSessionInfoBtn.addEventListener('click', getSessionInfo);
    }
    
    const listSessionsBtn = document.getElementById('listSessionsBtn');
    if (listSessionsBtn) {
        listSessionsBtn.addEventListener('click', listAllSessions);
    }
    
    const refreshSessionsBtn = document.getElementById('refreshSessionsBtn');
    if (refreshSessionsBtn) {
        refreshSessionsBtn.addEventListener('click', refreshSessionList);
    }
    
    const clearSessionBtn = document.getElementById('clearSessionBtn');
    if (clearSessionBtn) {
        clearSessionBtn.addEventListener('click', clearCurrentSession);
    }
    
    // 会话任务和状态
    const getSessionTasksBtn = document.getElementById('getSessionTasksBtn');
    if (getSessionTasksBtn) {
        getSessionTasksBtn.addEventListener('click', getSessionTasks);
    }
    
    const getSessionStatusBtn = document.getElementById('getSessionStatusBtn');
    if (getSessionStatusBtn) {
        getSessionStatusBtn.addEventListener('click', getSessionStatus);
    }
    
    const getSessionOutputBtn = document.getElementById('getSessionOutputBtn');
    if (getSessionOutputBtn) {
        getSessionOutputBtn.addEventListener('click', getSessionOutput);
    }
    
    // 注意：quickDownloadBtn 和其他文件管理按钮已在上面声明过了
    
    // 窗口大小变化事件
    window.addEventListener('resize', handleResponsiveLayout);
    
    // 点击外部关闭侧边栏（移动端）
    document.addEventListener('click', handleOutsideClick);
    
    // 键盘快捷键
    document.addEventListener('keydown', handleGlobalKeyPress);
}

// 处理全局键盘事件
function handleGlobalKeyPress(event) {
    // Ctrl/Cmd + Enter 发送消息
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        sendMessage();
    }
    
    // Escape 关闭模态框
    if (event.key === 'Escape') {
        closeFloatingModal();
    }
    
    // Ctrl/Cmd + / 切换主题
    if ((event.ctrlKey || event.metaKey) && event.key === '/') {
        event.preventDefault();
        toggleTheme();
    }
}

// 错误处理
window.addEventListener('error', function(event) {
    console.error('全局错误:', event.error);
    showToast('应用出现错误，请刷新页面重试', 'error');
});

// 未处理的 Promise 拒绝
window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的 Promise 拒绝:', event.reason);
    showToast('网络请求失败，请检查连接', 'error');
});

// 导出主要函数供全局使用
window.appFunctions = {
    // 消息相关
    sendMessage,
    stopGeneration,
    handleKeyDown,
    exportChatHistory,
    
    // 会话相关
    createNewSession,
    switchSession,
    deleteSession,
    clearCurrentSession,
    
    // 文件相关
    downloadFile,
    quickDownloadNetlist,
    listSessionFiles,
    
    // UI相关
    toggleTheme,
    toggleLeftSidebar,
    toggleRightSidebar,
    showToast,
    showFloatingModal,
    closeFloatingModal,
    
    // 设置相关
    toggleAutoScroll,
    toggleStreamMode
};

// 应用状态管理
const AppState = {
    // 获取当前状态
    getState() {
        return {
            currentSessionId,
            currentTurnid,
            isGenerating,
            isConnected,
            messageCount,
            generationCount,
            successCount,
            autoScroll,
            streamMode,
            theme: document.documentElement.getAttribute('data-theme')
        };
    },
    
    // 重置状态
    reset() {
        currentSessionId = null;
        currentTurnid = null;
        isGenerating = false;
        messageHistory = [];
        messageCount = 0;
        generationCount = 0;
        successCount = 0;
        currentUploadedFiles = [];
        
        // 更新UI
        updateSessionInfo();
        updateUploadedFilesDisplay();
        
        const chatMessages = document.getElementById('chatMessages');
        if (chatMessages) {
            chatMessages.innerHTML = '';
        }
    },
    
    // 保存状态到本地存储
    save() {
        const state = this.getState();
        localStorage.setItem('appState', JSON.stringify({
            autoScroll: state.autoScroll,
            streamMode: state.streamMode,
            theme: state.theme
        }));
    },
    
    // 从本地存储加载状态
    load() {
        try {
            const saved = localStorage.getItem('appState');
            if (saved) {
                const state = JSON.parse(saved);
                if (typeof state.autoScroll === 'boolean') {
                    setAutoScroll(state.autoScroll);
                }
                if (typeof state.streamMode === 'boolean') {
                    streamMode = state.streamMode;
                    updateStreamModeToggle();
                }
                if (state.theme) {
                    document.documentElement.setAttribute('data-theme', state.theme);
                    updateThemeToggleButton(state.theme);
                }
            }
        } catch (error) {
            console.error('加载应用状态失败:', error);
        }
    }
};

// 页面卸载时保存状态
window.addEventListener('beforeunload', function() {
    AppState.save();
});

// 页面可见性变化处理
document.addEventListener('visibilitychange', function() {
    if (document.visibilityState === 'visible') {
        // 页面变为可见时检查连接状态
        checkConnectionStatus();
    }
});

// 调试工具（仅在开发环境中使用）
if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    window.debugApp = {
        state: AppState,
        functions: window.appFunctions,
        logs: {
            enable() {
                window.DEBUG_MODE = true;
                console.log('调试模式已启用');
            },
            disable() {
                window.DEBUG_MODE = false;
                console.log('调试模式已禁用');
            }
        }
    };
    
    console.log('调试工具已加载，使用 window.debugApp 访问');
}