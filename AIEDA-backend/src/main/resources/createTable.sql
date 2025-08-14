USE ai_eda;
DROP TABLE IF EXISTS records;
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS users;


CREATE TABLE users (
    uid INT AUTO_INCREMENT PRIMARY KEY ,
    username VARCHAR(50),
    phone VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    role INT NOT NULL DEFAULT 1
);

INSERT INTO users (username, phone, password, description, role) VALUES
('admin', '1234567890', '123456', 'Administrator', 1);

-- 会话表
DROP TABLE IF EXISTS sessions;
CREATE TABLE sessions (
    sid INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    uid INT NOT NULL COMMENT '用户ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 对话记录表
DROP TABLE IF EXISTS records;
CREATE TABLE records (
    rid INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    sid INT NOT NULL COMMENT '会话ID',
    uid INT NOT NULL COMMENT '用户ID',
    direction BOOLEAN NOT NULL COMMENT '消息方向：true=用户消息，false=AI消息',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    sequence INT NOT NULL COMMENT '消息顺序',
    type INT NOT NULL COMMENT '消息类型：1=user，2=llm_generation，3=llm_toconfirmed，4=tool_execution_result',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    FOREIGN KEY (sid) REFERENCES sessions(sid) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

-- 文件管理表
DROP TABLE IF EXISTS files;
CREATE TABLE files (
    file_id VARCHAR(36) PRIMARY KEY COMMENT '文件ID (UUID)',
    uid INT NOT NULL COMMENT '用户ID',
    sid INT NOT NULL COMMENT '会话ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    saved_name VARCHAR(255) NOT NULL COMMENT '保存的文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件在大模型服务中的路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型/扩展名',
    upload_time DATETIME NOT NULL COMMENT '上传时间',
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (sid) REFERENCES sessions(sid) ON DELETE CASCADE,
    INDEX idx_uid_sid (uid, sid),
    INDEX idx_upload_time (upload_time)
);


-- 插入集成电路设计相关的测试会话
INSERT INTO sessions (uid, title, create_time, update_time) VALUES
    (1, '芯片功耗优化方案', '2024-01-15 09:30:00', '2024-01-15 11:45:00'),
    (1, '时序分析问题排查', '2024-01-16 13:20:00', '2024-01-16 15:10:00'),
    (1, '布局布线策略讨论', '2024-01-17 10:15:00', '2024-01-17 16:30:00');

-- 插入测试文件记录
INSERT INTO files (file_id, uid, sid, original_name, saved_name, file_path, file_size, file_type, upload_time) VALUES
    ('f1e2d3c4-b5a6-9c8d-7e6f-1a2b3c4d5e6f', 1, 1, 'power_analysis.pdf', '20240115_power_analysis.pdf', 'temp/uploads/user1/session1/20240115_power_analysis.pdf', 2048000, '.pdf', '2024-01-15 09:25:00'),
    ('a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6', 1, 2, 'timing_report.txt', '20240116_timing_report.txt', 'temp/uploads/user1/session2/20240116_timing_report.txt', 512000, '.txt', '2024-01-16 13:15:00'),
    ('x1y2z3a4-b5c6-d7e8-f9g0-h1i2j3k4l5m6', 1, 3, 'layout_design.dxf', '20240117_layout_design.dxf', 'temp/uploads/user1/session3/20240117_layout_design.dxf', 8192000, '.dxf', '2024-01-17 10:10:00');

-- 插入集成电路设计相关的对话记录
INSERT INTO records (sid, uid, direction, content, sequence, type, create_time) VALUES
-- 第一个会话：芯片功耗优化方案
(1, 1, true, '我设计的32位MCU在7nm工艺下功耗超标15%，主要是待机模式下的漏电问题，有什么优化方案吗？', 1, 1, '2024-01-15 09:31:00'),
(1, 1, false, '7nm工艺下的漏电问题确实需要多维度优化，建议从以下方面入手：\n1. 采用多阈值电压单元库，关键路径用低阈值，非关键路径用高阈值\n2. 优化电源门控设计，将待机时不工作的模块完全断电\n3. 调整体偏置电压，降低亚阈值漏电\n需要我为您提供具体的电路结构示例吗？', 2, 2, '2024-01-15 09:33:00'),
(1, 1, true, '请重点讲解电源门控的实现方式，我们目前使用的是分布式PMOS开关', 3, 1, '2024-01-15 09:37:00'),
(1, 1, false, '分布式PMOS开关可以优化IR drop，建议这样改进：\n1. 增加header开关的尺寸，降低导通电阻\n2. 实现多级电源门控，按模块重要性分级断电\n3. 加入Retention Flip-Flop保存关键状态\n\n推荐的开关控制电路结构：\n[PMOS开关阵列] + [控制逻辑] + [状态保持单元]', 4, 3, '2024-01-15 09:40:00'),
(1, 1, true, '能否提供一个电源门控的Verilog代码示例？', 5, 1, '2024-01-15 09:42:00'),

-- 第二个会话：时序分析问题排查
(2, 1, true, '在进行1GHz主频的处理器核时序分析时，发现ALU模块存在 Setup Time Violation，裕量-12ps，该如何解决？', 1, 1, '2024-01-16 13:21:00'),
(2, 1, false, '1GHz下的-12ps违例需要优先处理，建议的解决步骤：\n\n1. 检查时钟树：确认是否存在时钟偏移过大的问题\n2. 优化关键路径：\n   - 重定时(Retiming)移动寄存器位置\n   - 流水线化(Pipelining)拆分长路径\n   - 逻辑重构减少级数\n3. 调整约束：确认是否有不合理的多周期路径设置\n\n需要我帮您分析典型的ALU关键路径结构吗？', 2, 2, '2024-01-16 13:23:00'),
(2, 1, true, '已经确认时钟树没问题，请提供一个加法器的流水线优化方案', 3, 1, '2024-01-16 13:28:00'),
(2, 1, false, '32位加法器的流水线优化可以这样实现：\n\n```verilog\nmodule pipelined_adder (\n    input clk,\n    input [31:0] a,\n    input [31:0] b,\n    output reg [32:0] sum\n);\n    reg [15:0] sum_low, a_low, b_low;\n    reg [16:0] carry_high;\n    reg [15:0] a_high, b_high;\n    \n    // 第一级流水线：计算低16位\n    always @(posedge clk) begin\n        a_low <= a[15:0];\n        b_low <= b[15:0];\n        sum_low <= a[15:0] + b[15:0];\n        a_high <= a[31:16];\n        b_high <= b[31:16];\n    end\n    \n    // 第二级流水线：计算高16位和进位\n    always @(posedge clk) begin\n        carry_high <= a_high + b_high + (sum_low[16] ? 1"b1 : 1"b0);\n    end\n    \n    // 第三级流水线：合并结果\n    always @(posedge clk) begin\n        sum <= {carry_high, sum_low[15:0]};\n    end\nendmodule\n```\n\n这种三级流水线结构可将关键路径缩短至原来的1/3', 4, 2, '2024-01-16 13:31:00'),

-- 第三个会话：布局布线策略讨论
(3, 1, true, '我们的AI加速芯片在布局布线后出现严重的信号完整性问题，尤其是高速数据总线，串扰噪声超过200mV，该如何解决？', 1, 1, '2024-01-17 10:16:00'),
(3, 1, false, '高速数据总线的串扰问题主要源于耦合电容，建议从以下方面优化：\n\n1. 物理布线优化：\n   - 增加信号线间距（至少3倍线宽）\n   - 插入屏蔽线并接地\n   - 采用差分对布线\n\n2. 时序优化：\n   - 调整驱动强度\n   - 增加缓冲器隔离\n   - 优化信号跳变时间\n\n您的总线频率和数据宽度是多少？使用的是哪种拓扑结构？', 2, 2, '2024-01-17 10:18:00'),
(3, 1, true, '总线频率800MHz，32位宽，目前是菊花链拓扑，使用的是0.18μm工艺', 3, 1, '2024-01-17 10:22:00'),
(3, 1, false, '800MHz在0.18μm工艺下建议改为树状拓扑，并采用以下策略：\n1. 总线分组：将32位分成4组，每组8位，组间增加隔离\n2. 布线规则：\n   - 线宽增加到2μm，间距4μm\n   - 长度匹配控制在±5%以内\n   - 参考平面连续，避免分割\n3. 终端匹配：在总线两端添加50Ω匹配电阻\n\n这些措施可将串扰降低60%以上，需要我提供具体的布线规则设置吗？', 4, 2, '2024-01-17 10:25:00');