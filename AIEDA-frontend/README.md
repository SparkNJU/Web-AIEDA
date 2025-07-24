# AI EDA 系统开发进展汇报

## 一、项目概述
本次开发的 AI EDA 系统是一个集成用户管理与 LLM 对话功能的 Web 应用，采用 SpringBoot + Vue + MySQL 技术栈实现，主要用于集成电路设计相关的智能问答与协作。系统支持用户注册、登录、信息管理，并提供基于会话的 LLM 交互功能，可记录和管理完整的对话历史。

## 二、核心功能实现

### 1. 用户管理系统

#### 1.1 功能设计
- 用户注册：支持新用户创建账号，包含用户名、手机号（唯一标识）、密码等信息
- 用户登录：基于手机号和密码的身份验证，采用 JWT 生成身份令牌
- 信息管理：支持用户信息查询、更新和删除操作
- 权限控制：通过角色字段区分管理员 (1) 和普通用户 (0)

#### 1.2 数据模型
```sql
CREATE TABLE users (
    uid INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    phone VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    role INT NOT NULL DEFAULT 1
);
```

#### 1.3 核心接口
| 接口功能 | 请求方式 | 接口地址 | 说明 |
|---------|---------|---------|------|
| 用户注册 | POST | /api/accounts | 创建新用户账号 |
| 用户登录 | POST | /api/accounts/login | 身份验证并获取 Token |
| 获取用户详情 | GET | /api/accounts/{phone} | 根据手机号查询用户信息 |
| 更新用户信息 | PUT | /api/accounts | 修改用户资料 |
| 删除用户 | DELETE | /api/accounts | 根据手机号删除用户 |

#### 1.4 技术亮点
- 采用 JWT 进行身份认证，Token 自动加入后续请求 Header
- 密码安全存储，前端传输加密处理
- 手机号唯一约束，确保用户身份唯一性
- 响应统一封装为 Response<T>类型，包含 code、message 和 data 字段

### 2. 与 LLM 集成的对话系统

#### 2.1 功能设计
- 会话管理：支持创建、查询、更新和删除对话会话
- 消息交互：用户与 LLM 之间的消息发送与接收
- 历史记录：完整保存所有对话内容，支持按顺序展示

#### 2.2 数据模型
```sql
-- 会话表
CREATE TABLE sessions (
    sid INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    uid INT NOT NULL COMMENT '用户ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

-- 对话记录表
CREATE TABLE records (
    rid INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    sid INT NOT NULL COMMENT '会话ID',
    uid INT NOT NULL COMMENT '用户ID',
    direction BOOLEAN NOT NULL COMMENT '消息方向：true=用户消息，false=AI消息',
    content TEXT NOT NULL COMMENT '消息内容',
    sequence INT NOT NULL COMMENT '消息顺序',
    type INT NOT NULL COMMENT '消息类型',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

#### 2.3 消息类型设计
```java
public class MessageTypeConstant {
    public static final int USER = 0; // 用户消息
    public static final int LLM_GENERATION = 1; // LLM生成的消息
    public static final int LLM_TO_CONFIRM = 2; // 需要用户确认的消息
    public static final int TOOL_EXECUTION_RESULT = 3; // 工具执行结果
}
```

#### 2.4 核心接口
| 接口功能 | 请求方式 | 接口地址 | 说明 |
|---------|---------|---------|------|
| 创建会话 | POST | /api/chats/sessions | 新建对话会话 |
| 获取会话列表 | GET | /api/chats/sessions/{uid} | 查询用户所有会话 |
| 获取会话记录 | GET | /api/chats/sessions/{sid}/records | 查询指定会话的消息记录 |
| 发送消息 | POST | /api/chats/messages | 发送消息并获取 LLM 回复 |
| 更新会话标题 | PUT | /api/chats/sessions/{sid} | 修改会话标题 |
| 删除会话 | DELETE | /api/chats/sessions/{sid} | 删除会话及所有记录 |

#### 2.5 LLM 集成实现
- 通过 RESTful API 调用 LLM 服务（http://localhost:8000）
- 采用轮询方式获取 AI 生成结果，设置 30 秒超时机制
- 支持消息片段拼接，通过`<finish>`标签识别回复结束
- 异常处理机制，确保服务稳定性

### 3. 服务器部署

#### 3.1 部署架构
- 前端：Vue 静态资源部署在 Nginx 服务器
- 后端：SpringBoot 应用部署在 Java 容器
- 数据库：MySQL 独立部署，配置主从备份
- 服务通信：内部 API 通过 RESTful 接口交互

#### 3.2 部署步骤
**环境准备：** 安装 JDK 17、Node.js、MySQL 8.0、Nginx

**后端部署：**
- 打包 SpringBoot 应用为 jar 包
- 配置 application.properties 文件，设置数据库连接
- 使用 systemd 配置服务自启动

**前端部署：**
- 执行 npm run build 生成 dist 目录
- 配置 Nginx 指向 dist 目录
- 配置反向代理指向后端 API

**数据库部署：**
- 执行 createTable.sql 初始化数据库结构
- 配置 MySQL 远程访问权限
- 设置定期备份策略

#### 3.3 访问方式
- 系统访问地址：http://服务器IP:端口
- API 文档地址：http://服务器IP:8080/doc.html（支持在线接口测试）

## 三、系统演示

### 用户注册与登录
- 通过手机号注册新账号
- 使用账号密码登录系统，获取身份令牌

### 会话管理
- 创建新的对话会话
- 查看历史会话列表
- 重命名或删除会话

### LLM 交互
- 在会话中发送消息
- 接收并查看 AI 回复
- 查看完整对话历史

## 四、下一步工作计划
- 优化 LLM 调用方式，采用 WebSocket 实现实时消息推送
- 增加用户权限细粒度控制
- 完善系统监控与日志记录
- 优化前端交互体验，支持消息编辑与撤回
- 增加多语言支持

## 五、总结
本阶段已完成用户管理系统与 LLM 对话系统的核心功能开发，并成功部署到服务器。系统架构清晰，接口设计规范，为后续功能扩展奠定了良好基础。通过统一的响应格式和数据模型设计，保证了前后端交互的一致性和可维护性。
