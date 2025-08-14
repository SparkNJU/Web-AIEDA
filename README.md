# Web-AIEDA

## 项目概述

Web-AIEDA是一个基于Web的AI驱动的电子设计自动化（Electronic Design Automation）平台，旨在为集成电路设计工程师提供智能化的设计辅助、问题解答和设计优化建议。该平台集成了用户管理系统和大语言模型（LLM）对话功能，支持实时的AI助手服务。

## 技术栈

### 后端 (AIEDA-backend)
- **框架**: Spring Boot 3.5.3
- **语言**: Java 17
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA + Hibernate
- **安全**: Spring Security + JWT认证
- **API文档**: Swagger/OpenAPI 3.0 + Knife4j
- **构建工具**: Maven

### 前端 (AIEDA-frontend)
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI组件**: Element Plus
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **Markdown渲染**: markdown-it (支持KaTeX数学公式)

### 外部服务
- **LLM服务**: 通过HTTP API集成外部大语言模型服务
- **实时通信**: Server-Sent Events (SSE) 实现流式对话

## 项目结构

```
Web-AIEDA/
├── AIEDA-backend/          # Spring Boot后端
│   ├── src/main/java/
│   │   └── org/example/aiedabackend/
│   │       ├── config/     # 配置类
│   │       ├── controller/ # REST API控制器
│   │       ├── dao/        # 数据访问层
│   │       ├── po/         # 实体类
│   │       ├── service/    # 业务逻辑层
│   │       ├── vo/         # 视图对象
│   │       ├── util/       # 工具类
│   │       └── interceptor/ # 拦截器
│   ├── src/main/resources/
│   │   ├── application.properties # 配置文件
│   │   └── createTable.sql        # 数据库初始化脚本
│   └── pom.xml             # Maven依赖配置
├── AIEDA-frontend/         # Vue.js前端
│   ├── src/
│   │   ├── api/            # API接口封装
│   │   ├── components/     # 通用组件
│   │   ├── views/          # 页面组件
│   │   ├── router/         # 路由配置
│   │   ├── utils/          # 工具函数
│   │   └── constants/      # 常量定义
│   ├── public/             # 静态资源
│   ├── package.json        # npm依赖配置
│   └── vite.config.ts      # Vite构建配置
└── docs/                   # 项目文档
    └── User API.md         # API文档
```

## 核心功能

### 1. 用户管理系统
- **用户注册**: 支持手机号注册，包含验证码验证
- **用户登录**: 基于JWT的身份认证
- **个人信息管理**: 用户资料查看和编辑
- **权限控制**: 支持管理员和普通用户角色

### 2. AI对话系统
- **会话管理**: 创建、删除、编辑会话标题
- **实时对话**: 基于SSE的流式AI回复
- **消息历史**: 完整的对话记录保存和展示
- **Markdown渲染**: 支持代码高亮、数学公式等富文本内容
- **折叠显示**: 工具调用和引用内容的可折叠展示

### 3. EDA专业功能
- **芯片设计问答**: 针对集成电路设计的专业问题解答
- **功耗优化建议**: 提供功耗分析和优化方案
- **时序分析**: 协助解决时序违例问题
- **布局布线**: 信号完整性和布线策略指导

## 环境要求

### 开发环境
- **Java**: 17+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Maven**: 3.6+
- **npm/yarn**: 最新版本

### 生产环境
- **服务器**: Linux (推荐 Ubuntu 20.04+)
- **Web服务器**: Nginx (用于前端静态文件服务和反向代理)
- **数据库**: MySQL 8.0
- **Java运行时**: OpenJDK 17

## 快速开始

### 1. 环境准备

确保已安装以下软件：
- Java 17
- MySQL 8.0
- Node.js 16+
- Maven 3.6+

### 2. 数据库配置

```sql
# 创建数据库
CREATE DATABASE ai_eda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入表结构和初始数据
mysql -u root -p ai_eda < AIEDA-backend/src/main/resources/createTable.sql
```

### 3. 后端启动

```bash
cd AIEDA-backend

# 配置数据库连接
# 编辑 src/main/resources/application.properties
# 修改数据库用户名、密码等配置

# 编译运行
mvn clean compile
mvn spring-boot:run

# 或者打包运行
mvn clean package
java -jar target/AIEDA-backend-0.0.1-SNAPSHOT.jar
```

后端将在 `http://localhost:8080` 启动

### 4. 前端启动

```bash
cd AIEDA-frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 或者构建生产版本
npm run build
npm run preview
```

前端开发服务器将在 `http://localhost:5173` 启动

### 5. API文档访问

启动后端后，可通过以下地址访问API文档：
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Knife4j UI: `http://localhost:8080/doc.html`

## 配置说明

### 后端配置 (application.properties)

```properties
# 服务端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/ai_eda?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password

# JWT配置
jwt.secret=your_jwt_secret
jwt.expiration=864000000

# 外部LLM服务配置 (需要单独部署)
# LLM服务应提供以下端点：
# POST http://localhost:8000/api/v1/user/input - 提交用户输入
# GET http://localhost:8000/api/v1/stream - 获取流式回复
```

### 前端配置

前端配置主要在 `src/utils/request.ts` 中设置API基础URL：

```typescript
const baseURL = 'http://localhost:8080'  // 后端服务地址
```

## 部署指南

### Docker部署 (推荐)

```bash
# 构建后端镜像
cd AIEDA-backend
docker build -t aieda-backend .

# 构建前端镜像
cd ../AIEDA-frontend
docker build -t aieda-frontend .

# 使用docker-compose启动所有服务
docker-compose up -d
```

### 传统部署

#### 后端部署

```bash
# 打包应用
mvn clean package

# 部署到服务器
scp target/AIEDA-backend-0.0.1-SNAPSHOT.jar user@server:/opt/aieda/

# 使用systemd管理服务
sudo systemctl start aieda-backend
sudo systemctl enable aieda-backend
```

#### 前端部署

```bash
# 构建生产版本
npm run build

# 部署到Nginx
sudo cp -r dist/* /var/www/html/aieda/

# 配置Nginx反向代理
# 参考 nginx.conf 配置文件
```

## API接口

### 用户管理
- `POST /api/accounts/register` - 用户注册
- `POST /api/accounts/login` - 用户登录
- `GET /api/accounts/info` - 获取用户信息
- `PUT /api/accounts/info` - 更新用户信息

### 会话管理
- `GET /api/chats/sessions/{uid}` - 获取用户会话列表
- `POST /api/chats/sessions` - 创建新会话
- `PUT /api/chats/sessions/{sid}` - 更新会话标题
- `DELETE /api/chats/sessions/{sid}` - 删除会话

### 对话功能
- `GET /api/chats/sessions/{sid}/records` - 获取会话消息记录
- `POST /api/chats/messages` - 发送消息 (SSE流式响应)

## 开发指南

### 添加新的API端点

1. 在 `controller` 包中创建控制器类
2. 在 `service` 包中实现业务逻辑
3. 在 `dao` 包中添加数据访问方法
4. 更新前端 `api` 目录中的接口定义

### 前端组件开发

1. 在 `src/components` 中创建通用组件
2. 在 `src/views` 中创建页面组件
3. 使用 TypeScript 确保类型安全
4. 遵循 Vue 3 Composition API 最佳实践

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 MySQL 服务是否启动
   - 验证数据库连接配置
   - 确认数据库用户权限

2. **前端无法连接后端**
   - 检查后端服务是否正常启动
   - 验证API基础URL配置
   - 检查跨域配置

3. **LLM服务连接失败**
   - 确认外部LLM服务是否可用
   - 检查网络连接和防火墙设置
   - 验证API端点配置

### 日志查看

```bash
# 查看后端日志
tail -f logs/spring.log

# 查看前端开发服务器日志
npm run dev

# 查看Nginx日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 贡献指南

1. Fork项目仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目维护者: SparkNJU
- 项目地址: https://github.com/SparkNJU/Web-AIEDA

## 更新日志

### v1.0.0 (2024-01-17)
- 初始版本发布
- 实现用户管理系统
- 实现AI对话功能
- 支持流式响应和富文本显示
- 添加EDA专业功能示例
