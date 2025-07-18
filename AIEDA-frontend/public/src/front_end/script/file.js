// 文件管理模块

// 当前上传的文件列表
let currentUploadedFiles = [];

// 处理文件上传
async function handleFileUpload(event) {
    const files = event.target.files;
    if (!files.length) return;
    
    // 立即清空文件输入，避免重复选择问题
    const fileInput = event.target;
    const selectedFiles = Array.from(files);
    fileInput.value = '';
    
    if (!currentSessionId) {
        showToast('请先创建会话', 'error');
        return;
    }
    
    try {
        for (const file of selectedFiles) {
            // 添加到当前上传文件列表
            const fileInfo = {
                name: file.name,
                size: file.size,
                file: file,
                path: '',  // 初始化空路径
                uploaded: false  // 初始化上传状态为false
            };
            
            
            // 更新显示
            updateUploadedFilesDisplay();
            
            // 上传文件
            const formData = new FormData();
            formData.append('files', file);
            
            const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/upload_file`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const result = await response.json();
            console.log('上传文件结果:', result);
            fileInfo.path = result.files.file_path
            // 标记为已上传
            fileInfo.uploaded = true;
            currentUploadedFiles.push(fileInfo);
            updateUploadedFilesDisplay();
            
            showToast(`文件 ${file.name} 上传成功`, 'success');
        }
        
        // 刷新文件列表
        await listSessionFiles();
        await updateSidebarFilesList();
        
    } catch (error) {
        console.error('文件上传失败:', error);
        showToast('文件上传失败: ' + error.message, 'error');
    }
}

// 更新文件上传显示
function updateFileUploadDisplay() {
    updateUploadedFilesDisplay();
}

// 更新已上传文件显示
function updateUploadedFilesDisplay() {
    const displayDiv = document.getElementById('uploadedFilesDisplay');
    if (currentUploadedFiles.length === 0) {
        displayDiv.style.display = 'none';
        return;
    }
    displayDiv.style.display = 'flex';

    displayDiv.innerHTML = currentUploadedFiles.map((fileInfo, index) => {
        const sizeText = formatFileSize(fileInfo.size);
        const statusIcon = fileInfo.uploaded ? '✓' : '⏳';
        
        // 截取文件名，最多10个字符
        let displayName = fileInfo.name;
        if (displayName.length > 10) {
            displayName = displayName.substring(0, 10);
        }
        
        return `
            <div class="uploaded-file-card">
                <div class="file-icon-card">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14,2 14,8 20,8"/>
                        <line x1="16" y1="13" x2="8" y2="13"/>
                        <line x1="16" y1="17" x2="8" y2="17"/>
                        <polyline points="10,9 9,9 8,9"/>
                    </svg>
                    <span class="status-icon">${statusIcon}</span>
                </div>
                <div class="file-name-card" title="${fileInfo.name}">
                    ${displayName}
                </div>
                <div class="file-size-card">${sizeText}</div>
                <button class="file-remove-card" onclick="removeUploadedFile(${index})" title="移除文件">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>
            </div>
        `;
    }).join('');
}


// 移除已上传的文件
function removeUploadedFile(fileName) {
    const index = currentUploadedFiles.findIndex(f => f.name === fileName);
    if (index > -1) {
        currentUploadedFiles.splice(index, 1);
        updateUploadedFilesDisplay();
    }
}

// 清空当前上传的文件
function clearCurrentUploadedFiles() {
    currentUploadedFiles = [];
    updateUploadedFilesDisplay();
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

// 快速下载最新的网表文件
async function quickDownloadNetlist() {
    if (!currentSessionId) {
        showToast('请先创建会话', 'error');
        return;
    }
    
    try {
        showToast('正在获取最新网表...', 'info');
        
        // 获取会话文件列表
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/files`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        const files = data.files || [];
        
        // 查找网表文件（通常以.net, .cir, .sp等结尾）
        const netlistFile = files.find(file => 
            file.name.match(/\.(net|cir|sp|spice)$/i)
        );
        
        if (netlistFile) {
            console.log(netlistFile)
            await downloadFile(netlistFile.path);
            showToast(`网表文件 ${netlistFile.name} 下载成功`, 'success');
        } else {
            showToast('未找到网表文件', 'warning');
        }
        
    } catch (error) {
        console.error('下载网表失败:', error);
        showToast('下载网表失败: ' + error.message, 'error');
    }
}

// 下载文件
async function downloadFile(filepath, filename = null) {
    if (!currentSessionId) {
        showToast('请先创建会话', 'warning');
        return;
    }
    
    // 如果只传了一个参数，且看起来像文件名（不包含路径分隔符），则作为兼容处理
    if (filename === null && !filepath.includes('/')) {
        filename = filepath;
    } else if (filename === null) {
        // 从路径中提取文件名
        filename = filepath.split('/').pop();
    }
    
    try {
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/download/${encodeURIComponent(filepath)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        showToast(`文件 ${filename} 下载成功`, 'success');
        
    } catch (error) {
        console.error('下载文件失败:', error);
        showToast('下载文件失败: ' + error.message, 'error');
    }
}

// 获取流式数据
async function getStreamData() {
    const infoDiv = document.getElementById('getStreamDataInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取流式数据...', 'info');
    
    try {
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/output`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        document.getElementById('dataInfo').textContent = JSON.stringify(data, null, 2);
        showTempInfo(infoDiv, '流式数据获取成功', 'success');
        
    } catch (error) {
        console.error('获取流式数据失败:', error);
        showTempInfo(infoDiv, '获取流式数据失败: ' + error.message, 'error');
    }
}
// 清空流式数据
function clearStreamData() {
    const infoDiv = document.getElementById('clearStreamDataInfo');
    document.getElementById('dataInfo').textContent = '暂无数据';
    showTempInfo(infoDiv, '流式数据已清空', 'success');
}
// 列出会话文件
async function listSessionFiles() {
    const infoDiv = document.getElementById('listSessionFilesInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    showTempInfo(infoDiv, '正在获取文件列表...', 'info');
    
    try {
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/files`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        const files = data.files || [];
        const fileInfoElement = document.getElementById('fileInfo');
        
        if (files.length === 0) {
            fileInfoElement.innerHTML = '<div class="compact-info"><span>暂无文件</span></div>';
            showTempInfo(infoDiv, '暂无文件', 'info');
        } else {
            let fileListHtml = '<div class="compact-info">';
            files.forEach(file => {
                const fileSize = file.size ? formatFileSize(file.size) : '未知大小';
                fileListHtml += `
                    <div style="display: flex; align-items: center; justify-content: space-between; margin: 5px 0; padding: 5px; border: 1px solid #ddd; border-radius: 4px;">
                        <span>📄 ${file.name} (${fileSize})</span>
                        <button onclick="downloadFile('${file.path}', '${file.name}')" style="background: #007bff; color: white; border: none; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 12px;">下载</button>
                    </div>
                `;
            });
            fileListHtml += '</div>';
            fileInfoElement.innerHTML = fileListHtml;
            showTempInfo(infoDiv, `找到 ${files.length} 个文件`, 'success');
        }
        
    } catch (error) {
        console.error('获取文件列表失败:', error);
        showTempInfo(infoDiv, '获取文件列表失败: ' + error.message, 'error');
    }
}


// 更新左侧文件列表显示
async function updateSidebarFilesList() {
    const uploadedFilesListDiv = document.getElementById('uploadedFilesList');
    
    if (!currentSessionId) {
        uploadedFilesListDiv.innerHTML = '暂无上传文件';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/api/session/${currentSessionId}/files`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        const files = data.files || [];
        
        if (files.length === 0) {
            uploadedFilesListDiv.innerHTML = '暂无上传文件';
        } else {
            let fileListHtml = '';
            files.forEach(file => {
                const fileSize = file.size ? formatFileSize(file.size) : '未知大小';
                fileListHtml += `
                    <div class="file-item" style="display: flex; align-items: center; justify-content: space-between; padding: 5px; margin: 2px 0; border: 1px solid #eee; border-radius: 3px;">
                        <span>📄 ${file.name} (${fileSize})</span>
                        <button onclick="downloadFile('${file.path}', '${file.name}')" style="background: #28a745; color: white; border: none; padding: 1px 6px; border-radius: 2px; cursor: pointer; font-size: 10px;">下载</button>
                    </div>
                `;
            });
            uploadedFilesListDiv.innerHTML = fileListHtml;
        }
        
    } catch (error) {
        console.error('更新文件列表失败:', error);
        uploadedFilesListDiv.innerHTML = '获取文件列表失败';
    }
}

// 清理旧文件
async function clearOldFiles() {
    const infoDiv = document.getElementById('clearOldFilesInfo');
    if (!currentSessionId) {
        showTempInfo(infoDiv, '请先创建会话', 'error');
        return;
    }
    
    if (!confirm('确定要清理旧文件吗？此操作不可撤销。')) {
        return;
    }
    
    try {
        // 这里可以实现清理逻辑，比如删除超过一定时间的文件
        showTempInfo(infoDiv, '文件清理功能待实现', 'info');
        
    } catch (error) {
        console.error('清理文件失败:', error);
        showTempInfo(infoDiv, '清理文件失败: ' + error.message, 'error');
    }
}

// 显示文件列表
function displayFilesList(files) {
    const fileListDiv = document.getElementById('fileList');
    if (!fileListDiv) return;
    
    fileListDiv.innerHTML = '';
    
    if (files.length === 0) {
        fileListDiv.innerHTML = '<p style="color: var(--text-secondary); font-size: 12px;">暂无文件</p>';
        return;
    }
    
    files.forEach(file => {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        fileItem.style.cssText = `
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px;
            margin: 4px 0;
            background: var(--bg-primary);
            border: 1px solid var(--border-color);
            border-radius: 6px;
            font-size: 12px;
        `;
        
        const fileName = document.createElement('span');
        fileName.textContent = file.name || file;
        fileName.style.cssText = `
            flex: 1;
            color: var(--text-primary);
            word-break: break-all;
        `;
        
        const downloadBtn = document.createElement('button');
        downloadBtn.textContent = '下载';
        downloadBtn.className = 'btn btn-primary';
        downloadBtn.style.cssText = `
            padding: 4px 8px;
            font-size: 10px;
            min-width: auto;
            margin: 0;
        `;
        downloadBtn.onclick = () => downloadFile(file.name || file);
        
        fileItem.appendChild(fileName);
        fileItem.appendChild(downloadBtn);
        fileListDiv.appendChild(fileItem);
    });
}


// 更新文件列表
function updateFilesList() {
    listSessionFiles();
}