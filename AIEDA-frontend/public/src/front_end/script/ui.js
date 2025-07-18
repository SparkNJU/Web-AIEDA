// UI 控制相关功能

// 主题初始化和切换
function initializeTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark'; // 默认为浅色主题
    if (savedTheme === 'dark') {
        document.body.classList.remove('light-mode');
    } else {
        document.body.classList.add('light-mode');
    }
    updateThemeToggleButton(savedTheme);
}

function toggleTheme() {
    const isLightMode = document.body.classList.contains('light-mode');
    const newTheme = isLightMode ? 'dark' : 'light';
    
    if (newTheme === 'dark') {
        document.body.classList.remove('light-mode');
    } else {
        document.body.classList.add('light-mode');
    }
    localStorage.setItem('theme', newTheme);
    updateThemeToggleButton(newTheme);

    // 派发主题变更事件,供其他组件响应
    document.dispatchEvent(new CustomEvent('themeChange', {
        detail: { theme: newTheme }
    }));
}

function updateThemeToggleButton(theme) {
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        themeToggle.textContent = theme === 'dark' ? '🌞' : '🌙';
        themeToggle.title = theme === 'dark' ? '切换到浅色主题' : '切换到深色主题';
    }
}

// 侧边栏控制
function toggleLeftSidebar() {
    const leftSidebar = document.getElementById('leftSidebar');
    const toggleBtn = document.getElementById('toggleLeftSidebar');
    
    if (leftSidebar && toggleBtn) {
        const isHidden = leftSidebar.classList.contains('hidden');
        
        if (isHidden) {
            leftSidebar.classList.remove('hidden');
            toggleBtn.textContent = '◀';
            toggleBtn.title = '隐藏左侧栏';
        } else {
            leftSidebar.classList.add('hidden');
            toggleBtn.textContent = '▶';
            toggleBtn.title = '显示左侧栏';
        }
    }
}

function toggleRightSidebar() {
    const rightSidebar = document.getElementById('rightSidebar');
    const toggleBtn = document.getElementById('toggleRightSidebar');
    
    if (rightSidebar && toggleBtn) {
        const isHidden = rightSidebar.classList.contains('hidden');
        
        if (isHidden) {
            rightSidebar.classList.remove('hidden');
            toggleBtn.textContent = '▶';
            toggleBtn.title = '隐藏右侧栏';
        } else {
            rightSidebar.classList.add('hidden');
            toggleBtn.textContent = '◀';
            toggleBtn.title = '显示右侧栏';
        }
    }
}

// 响应式布局处理
function handleResponsiveLayout() {
    const leftSidebar = document.getElementById('leftSidebar');
    const rightSidebar = document.getElementById('rightSidebar');
    const chatContainer = document.getElementById('chatContainer');
    
    if (window.innerWidth <= 768) {
        // 移动端：默认隐藏侧边栏
        if (leftSidebar) leftSidebar.classList.add('mobile-hidden');
        if (rightSidebar) rightSidebar.classList.add('mobile-hidden');
        if (chatContainer) chatContainer.classList.add('mobile-full');
    } else {
        // 桌面端：恢复侧边栏
        if (leftSidebar) leftSidebar.classList.remove('mobile-hidden');
        if (rightSidebar) rightSidebar.classList.remove('mobile-hidden');
        if (chatContainer) chatContainer.classList.remove('mobile-full');
    }
}

// 点击外部关闭侧边栏（移动端）
function handleOutsideClick(event) {
    if (window.innerWidth <= 768) {
        const leftSidebar = document.getElementById('leftSidebar');
        const rightSidebar = document.getElementById('rightSidebar');
        const chatContainer = document.getElementById('chatContainer');
        
        // 如果点击的是聊天区域，且侧边栏是打开的，则关闭侧边栏
        if (chatContainer && chatContainer.contains(event.target)) {
            if (leftSidebar && !leftSidebar.classList.contains('mobile-hidden')) {
                leftSidebar.classList.add('mobile-hidden');
            }
            if (rightSidebar && !rightSidebar.classList.contains('mobile-hidden')) {
                rightSidebar.classList.add('mobile-hidden');
            }
        }
    }
}

// 显示临时信息
function showTempInfo(element, message, type = 'info', duration = 3000) {
    if (!element) return;
    
    element.textContent = message;
    element.style.display = 'block';
    
    // 清除之前的类型类
    element.className = element.className.replace(/temp-(success|error|warning|info)/g, '');
    // 添加新的类型类
    element.className = `temp-info temp-${type}`.trim();
    
    // 添加淡入动画
    element.style.animation = 'fadeIn 0.3s ease';
    
    setTimeout(() => {
        element.style.display = 'none';
        element.style.animation = '';
    }, duration);
}

// 添加欢迎消息
function addWelcomeMessage() {
    const chatMessages = document.getElementById('chatMessages');
    const welcomeHTML = `
        <div class="welcome-container" id="welcomeContainer">
            <div class="welcome-card">
                <div class="welcome-title">
                    🚀 IGBT网表生成器
                </div>
                <div class="welcome-subtitle">
                    专业的IGBT电路Ngspice网表智能生成工具
                </div>
                
                <div class="welcome-features">
                    <div class="welcome-feature">
                        <span class="welcome-feature-icon">🔧</span>
                        <div class="welcome-feature-title">智能网表生成</div>
                        <div class="welcome-feature-desc">基于AI技术，快速生成各种IGBT电路的Ngspice网表</div>
                    </div>
                    <div class="welcome-feature">
                        <span class="welcome-feature-icon">⚡</span>
                        <div class="welcome-feature-title">实时流式显示</div>
                        <div class="welcome-feature-desc">实时显示生成过程，让您了解每一步的进展</div>
                    </div>
                    <div class="welcome-feature">
                        <span class="welcome-feature-icon">📥</span>
                        <div class="welcome-feature-title">文件下载管理</div>
                        <div class="welcome-feature-desc">一键下载生成的网表文件，支持多种格式</div>
                    </div>
                    <div class="welcome-feature">
                        <span class="welcome-feature-icon">🔍</span>
                        <div class="welcome-feature-title">网表验证</div>
                        <div class="welcome-feature-desc">自动验证网表的正确性和语法规范</div>
                    </div>
                </div>
                
                <div class="welcome-prompt">
                    💡 <strong>开始使用：</strong>请在下方输入框中描述您需要的IGBT电路，例如：
                    <br>
                    "生成一个单管IGBT开关电路" 或 "创建IGBT桥式整流电路网表"
                </div>
            </div>
        </div>
    `;
    chatMessages.innerHTML = welcomeHTML;
}

// 关闭欢迎消息
function closeWelcomeMessage(button) {
    button.parentElement.remove();
}

// 显示 Toast 通知
function showToast(message, type = 'info') {
    let targetButton = null;
    
    // 优先使用最后点击的按钮
    if (lastClickedButton && document.contains(lastClickedButton)) {
        targetButton = lastClickedButton;
    } else {
        // 查找当前焦点按钮
        const activeElement = document.activeElement;
        if (activeElement && activeElement.tagName === 'BUTTON') {
            targetButton = activeElement;
        } else {
            // 根据消息内容智能选择按钮
            if (message.includes('网表') || message.includes('下载')) {
                targetButton = document.querySelector('button[onclick*="quickDownload"]');
            } else if (message.includes('会话') || message.includes('连接')) {
                targetButton = document.querySelector('button[onclick*="createSession"]');
            }
            
            // 如果还没找到，使用第一个按钮
            if (!targetButton) {
                const buttons = document.querySelectorAll('button');
                targetButton = buttons[0];
            }
        }
    }
    
    if (!targetButton) {
        console.log('No target button found for toast message:', message);
        return;
    }
    
    // 查找或创建该按钮的信息显示区域
    let infoDiv = targetButton.nextElementSibling;
    if (!infoDiv || !infoDiv.classList.contains('temp-info')) {
        infoDiv = document.createElement('div');
        infoDiv.className = 'temp-info';
        infoDiv.style.display = 'none';
        targetButton.parentNode.insertBefore(infoDiv, targetButton.nextSibling);
    }
    
    // 使用现有的showTempInfo函数
    showTempInfo(infoDiv, message, type, 3000);
}

// 显示浮动模态框
function showFloatingModal(title, content) {
    // 移除现有模态框
    const existingModal = document.querySelector('.modal-overlay');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 创建模态框
    const modalOverlay = document.createElement('div');
    modalOverlay.className = 'modal-overlay';
    
    const modalContent = document.createElement('div');
    modalContent.className = 'modal-content';
    
    const modalHeader = document.createElement('div');
    modalHeader.className = 'modal-header';
    
    const modalTitle = document.createElement('h3');
    modalTitle.textContent = title;
    
    const closeBtn = document.createElement('button');
    closeBtn.className = 'modal-close';
    closeBtn.innerHTML = '×';
    closeBtn.onclick = () => closeFloatingModal();
    
    const modalBody = document.createElement('div');
    modalBody.className = 'modal-body';
    modalBody.textContent = content;
    
    modalHeader.appendChild(modalTitle);
    modalHeader.appendChild(closeBtn);
    modalContent.appendChild(modalHeader);
    modalContent.appendChild(modalBody);
    modalOverlay.appendChild(modalContent);
    
    document.body.appendChild(modalOverlay);
    
    // 显示模态框
    setTimeout(() => {
        modalOverlay.classList.add('show');
    }, 10);
    
    // 点击背景关闭
    modalOverlay.onclick = (e) => {
        if (e.target === modalOverlay) {
            closeFloatingModal();
        }
    };
}

// 关闭浮动模态框
function closeFloatingModal() {
    const modalOverlay = document.querySelector('.modal-overlay');
    if (modalOverlay) {
        modalOverlay.classList.remove('show');
        setTimeout(() => {
            modalOverlay.remove();
        }, 300);
    }
}

// 创建可展开内容
function createExpandableContent(content, maxHeight = 200) {
    const container = document.createElement('div');
    container.className = 'expandable-content collapsed';
    container.style.maxHeight = maxHeight + 'px';
    
    const contentDiv = document.createElement('div');
    contentDiv.textContent = content;
    container.appendChild(contentDiv);
    
    // 检查是否需要展开按钮
    if (contentDiv.scrollHeight > maxHeight) {
        const overlay = document.createElement('div');
        overlay.className = 'expand-overlay';
        
        const hint = document.createElement('div');
        hint.className = 'expand-hint';
        hint.textContent = '点击展开更多内容';
        
        overlay.appendChild(hint);
        container.appendChild(overlay);
        
        overlay.onclick = () => {
            container.classList.toggle('collapsed');
            if (container.classList.contains('collapsed')) {
                container.style.maxHeight = maxHeight + 'px';
                hint.textContent = '点击展开更多内容';
                overlay.style.display = 'flex';
            } else {
                container.style.maxHeight = 'none';
                hint.textContent = '点击收起内容';
                overlay.style.display = 'none';
            }
        };
    }
    
    return container;
}

// 处理长内容
function handleLongContent(element, content, maxLength = 1000) {
    if (content.length > maxLength) {
        const expandableContent = createExpandableContent(content);
        element.appendChild(expandableContent);
    } else {
        element.textContent = content;
    }
}

// 添加代码块
function addCodeBlock(content, language = '') {
    const codeBlock = document.createElement('div');
    codeBlock.className = 'code-block';
    
    const code = document.createElement('code');
    code.textContent = content;
    
    const actions = document.createElement('div');
    actions.className = 'code-actions';
    
    const copyBtn = document.createElement('button');
    copyBtn.className = 'copy-btn';
    copyBtn.textContent = '复制';
    copyBtn.onclick = () => {
        navigator.clipboard.writeText(content).then(() => {
            showToast('代码已复制', 'success');
        }).catch(() => {
            showToast('复制失败', 'error');
        });
    };
    
    const downloadBtn = document.createElement('button');
    downloadBtn.className = 'download-btn';
    downloadBtn.textContent = '下载';
    downloadBtn.onclick = () => {
        const blob = new Blob([content], {type: 'text/plain'});
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `code_${Date.now()}.${language || 'txt'}`;
        link.click();
        showToast('代码已下载', 'success');
    };
    
    actions.appendChild(copyBtn);
    actions.appendChild(downloadBtn);
    codeBlock.appendChild(code);
    codeBlock.appendChild(actions);
    
    return codeBlock;
}

// 设置自动滚动
function setAutoScroll(enabled) {
    autoScroll = enabled;
    
    // 更新select元素的值
    const autoScrollSelect = document.getElementById('autoScroll');
    if (autoScrollSelect) {
        autoScrollSelect.value = enabled.toString();
    }
    
    // 保存设置
    localStorage.setItem('autoScroll', enabled.toString());
}

// 切换自动滚动
function toggleAutoScroll() {
    setAutoScroll(!autoScroll);
}

// 初始化设置
function initializeSettings() {
    // 初始化主题
    initializeTheme();
    
    // 初始化自动滚动设置
    const savedAutoScroll = localStorage.getItem('autoScroll');
    if (savedAutoScroll !== null) {
        setAutoScroll(savedAutoScroll === 'true');
        // 同步select元素的值
        const autoScrollSelect = document.getElementById('autoScroll');
        if (autoScrollSelect) {
            autoScrollSelect.value = savedAutoScroll;
        }
    }
    
    // 初始化流模式设置
    const savedStreamMode = localStorage.getItem('streamMode');
    if (savedStreamMode !== null) {
        streamMode = savedStreamMode === 'true';
        updateStreamModeToggle();
    }
}

// 更新流模式切换按钮
function updateStreamModeToggle() {
    const streamToggle = document.getElementById('streamToggle');
    if (streamToggle) {
        streamToggle.textContent = streamMode ? '🌊' : '📄';
        streamToggle.title = streamMode ? '切换到普通模式' : '切换到流模式';
    }
}

// 切换流模式
function toggleStreamMode() {
    streamMode = !streamMode;
    updateStreamModeToggle();
    localStorage.setItem('streamMode', streamMode.toString());
    showToast(`已切换到${streamMode ? '流' : '普通'}模式`, 'info');
}

// 加载应用设置
function loadAppSettings() {
    initializeSettings();
    
    // 检查连接状态
    checkConnectionStatus();
    
    // 设置定时检查连接
    setInterval(checkConnectionStatus, 300000); // 每5分钟检查一次
}

// 切换侧边栏（通用函数）
function toggleSidebar(sidebarId, direction = 'left') {
    const sidebar = document.getElementById(sidebarId);
    if (!sidebar) return;
    
    const isHidden = sidebar.classList.contains('hidden');
    
    if (isHidden) {
        sidebar.classList.remove('hidden');
    } else {
        sidebar.classList.add('hidden');
    }
    
    // 更新切换按钮
    const toggleBtnId = direction === 'left' ? 'toggleLeftSidebar' : 'toggleRightSidebar';
    const toggleBtn = document.getElementById(toggleBtnId);
    if (toggleBtn) {
        if (direction === 'left') {
            toggleBtn.textContent = isHidden ? '◀' : '▶';
            toggleBtn.title = isHidden ? '隐藏左侧栏' : '显示左侧栏';
        } else {
            toggleBtn.textContent = isHidden ? '▶' : '◀';
            toggleBtn.title = isHidden ? '隐藏右侧栏' : '显示右侧栏';
        }
    }
}

// 调整文本框高度
function adjustTextareaHeight() {
    resetInputHeight();
}

// 重置输入框高度
function resetInputHeight() {
    const input = document.getElementById('userInput');
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 150) + 'px';
}

