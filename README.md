# 智能客服监控系统 (Monitor Agent)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen)
![React](https://img.shields.io/badge/React-18.3.1-blue)
![Java](https://img.shields.io/badge/Java-17-red)
![AgentScope](https://img.shields.io/badge/AgentScope-1.0.5-orange)

> 基于 AgentScope 框架的智能客服监控系统，结合 RAG（检索增强生成）与 DevOps（运维监控）功能，提供实时监控、智能问答和自动告警能力。

## 功能特性

- **RAG 智能问答** - 基于本地知识库自动回答业务问题
- **实时状态监控** - 持续监听 API 状态，检测异常
- **自动告警通知** - 通过飞书 Webhook 发送富文本卡片告警
- **故障文档生成** - 自动创建故障记录文档到 Apifox
- **自我感知能力** - 引用真实监控日志数据回答系统状态问题
- **流式实时响应** - 支持 SSE（Server-Sent Events）实时流式输出
- **可视化调试** - 集成 AgentScope Studio 可视化工具
- **ReAct Agent** - 推理-行动模式的智能代理

## 技术栈

### 后端
- **框架**: Spring Boot 4.0.1 (WebFlux)
- **语言**: Java 17
- **AI 框架**: AgentScope 1.0.5
- **响应式**: Spring WebFlux + Reactor
- **LLM 集成**: OpenAI Java SDK
- **HTTP 客户端**: OkHttp4

### 前端
- **框架**: React 18.3.1
- **语言**: TypeScript 5.7.2
- **构建工具**: Vite 6.0.7
- **UI 组件**: Ant Design 5.12.0
- **Markdown**: react-markdown + remark-gfm

### AI 能力
- **代理模式**: ReActAgent（推理-行动）
- **知识检索**: Agentic RAG（Agent 自主检索）
- **记忆管理**: InMemory Memory
- **工具集成**: 飞书、Apifox、监控检查

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+

### Docker 部署（推荐）

使用 Docker Compose 一键启动完整的应用栈：

```bash
# 1. 复制环境变量配置文件
cp .env.example .env

# 2. 编辑 .env 文件，配置必需的环境变量
# 至少需要配置 LLM_API_KEY 和 EMBEDDING_API_KEY

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f

# 访问应用
# 前端: http://localhost:5173
# 后端健康检查: http://localhost:8081/api/health
```

**停止服务**:

```bash
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

**重新构建并启动**:

```bash
docker-compose up -d --build
```

### 本地部署

**后端启动**：

```bash
cd backend

# 配置环境变量
export LLM_API_KEY="your-openai-api-key"
export LLM_BASE_URL="https://api.openai.com/v1"
export LLM_MODEL_NAME="gpt-3.5-turbo"
export EMBEDDING_API_KEY="your-openai-api-key"
export EMBEDDING_BASE_URL="https://api.openai.com/v1"
export EMBEDDING_MODEL_NAME="text-embedding-3-small"
export FEISHU_WEBHOOK_URL="your-feishu-webhook-url"
export APIFOX_API_URL="https://api.apifox.com"
export APIFOX_API_TOKEN="your-apifox-token"
export APIFOX_PROJECT_ID="your-project-id-here"

# 运行应用
mvn spring-boot:run
```

服务将在 `http://localhost:8081` 启动。

**前端启动**：

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:5173` 启动。

### Docker 部署

**独立构建镜像**：

```bash
# 构建后端镜像
cd backend
docker build -t monitor-agent-backend:latest .

# 构建前端镜像
cd ../frontend
docker build -t monitor-agent-frontend:latest .

# 运行后端容器
docker run -d \
  --name monitor-backend \
  -p 8081:8081 \
  -e LLM_API_KEY="your-api-key" \
  -e LLM_BASE_URL="https://api.openai.com/v1" \
  monitor-agent-backend:latest

# 运行前端容器
docker run -d \
  --name monitor-frontend \
  -p 80:80 \
  --link monitor-backend:backend \
  monitor-agent-frontend:latest
```

**使用 Docker Compose（推荐）**：

```bash
# 1. 复制环境变量配置文件
cp .env.example .env

# 2. 编辑 .env 文件，配置必需的环境变量
# 至少需要配置 LLM_API_KEY

# 3. 启动所有服务（包括后端、前端、Redis）
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看日志
docker-compose logs -f

# 6. 访问应用
# 前端: http://localhost
# 后端健康检查: http://localhost:8081/api/health
```

**Docker Compose 常用命令**：

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v

# 重新构建并启动
docker-compose up -d --build

# 查看某个服务的日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 重启某个服务
docker-compose restart backend

# 查看资源使用情况
docker-compose top
```

## 配置说明

### 环境变量

| 变量 | 说明                       | 默认值 | 是否必需 |
|------|--------------------------|--------|------|
| `LLM_API_KEY` | OpenAI API 密钥（用于 Chat）   | - | 是    |
| `LLM_BASE_URL` | LLM 基础 URL (兼容OpenAI规范)  | `https://api.openai.com/v1` | 是    |
| `LLM_MODEL_NAME` | LLM 模型名称                 | `gpt-3.5-turbo` | 是    |
| `EMBEDDING_API_KEY` | Embedding API 密钥（用于 RAG） | - | 是    |
| `EMBEDDING_BASE_URL` | Embedding 基础 URL         | `https://api.openai.com/v1` | 是    |
| `EMBEDDING_MODEL_NAME` | Embedding 模型名称           | `text-embedding-3-small` | 是    |
| `FEISHU_WEBHOOK_URL` | 飞书 Webhook URL           | - | 是    |
| `APIFOX_API_URL` | Apifox API URL           | `https://api.apifox.com` | 是    |
| `APIFOX_API_TOKEN` | Apifox API Token         | - | 是    |
| `APIFOX_PROJECT_ID` | Apifox 项目 ID             | - | 是    |
| `APIFOX_FOLDER_ID` | Apifox 文件夹 ID            | - | 否    |
| `APIFOX_MODULE_ID` | Apifox 模块 ID             | - | 否    |

### 应用配置

主要配置文件位于 `backend/src/main/resources/application.properties`：

```properties
# 服务端口
server.port=8081

# LLM 配置（用于 Chat）
agentscope.llm.api-key=${LLM_API_KEY:}
agentscope.llm.base-url=${LLM_BASE_URL:http://localhost:11434/v1}
agentscope.llm.model-name=${LLM_MODEL_NAME:gpt-3.5-turbo}

# Embedding 配置（用于 RAG）
agentscope.embedding.api-key=${EMBEDDING_API_KEY:}
agentscope.embedding.base-url=${EMBEDDING_BASE_URL:http://localhost:11434/v1}
agentscope.embedding.model-name=${EMBEDDING_MODEL_NAME:text-embedding-3-small}
agentscope.embedding.enabled=true

# 飞书 Webhook
monitor.feishu.webhook-url=${FEISHU_WEBHOOK_URL:}

# Apifox API
monitor.apifox.api-url=${APIFOX_API_URL:https://api.apifox.com}
monitor.apifox.project-id=${APIFOX_PROJECT_ID:}
monitor.apifox.api-token=${APIFOX_API_TOKEN:}

# Studio 支持
monitor.studio.enabled=false
```

## 项目结构

```
monitor-agent/
├── backend/                    # 后端服务
│   ├── Dockerfile              # 后端 Docker 镜像构建文件
│   ├── pom.xml               # Maven 依赖配置
│   └── src/main/java/com/oneagent/monitor/
│       ├── agent/            # Agent 配置
│       ├── config/           # 配置类
│       ├── controller/       # REST API 控制器
│       ├── hook/             # Hook 钩子
│       ├── model/            # 数据模型
│       ├── service/          # 业务服务
│       ├── tool/             # Agent 工具
│       ├── studio/           # Studio 集成
│       └── util/             # 工具类
│   └── src/main/resources/
│       ├── application.properties
│       └── knowledge/        # 知识库文档
├── frontend/                   # 前端应用
│   ├── Dockerfile              # 前端 Docker 镜像构建文件
│   ├── nginx.conf             # Nginx 配置文件
│   ├── package.json             # NPM 配置
│   └── src/
│       ├── components/        # UI 组件
│       ├── services/         # API 服务
│       ├── types/            # TypeScript 类型
│       └── *.css             # 样式文件
├── docker-compose.yml          # Docker Compose 配置
├── .env.example              # 环境变量示例
├── docs/                     # 文档目录
│   ├── ARCHITECTURE.md       # 架构文档
│   └── API.md               # API 文档
└── 智能客服监控 Agent.md      # 项目说明
```

## 核心模块

### Agent 配置

- **ReActAgent**: 使用推理-行动模式的智能代理
- **RAG 集成**: Agentic 模式的知识检索，自动从知识库获取相关信息
- **工具注册**:
  - `FeishuWebhookTool` - 飞书告警
  - `ApifoxApiTool` - 故障文档生成
  - `MonitorCheckTool` - 状态检查

### 服务层

#### ChatService
处理用户查询与 Agent 交互，构建 API 上下文信息，处理告警触发逻辑。

#### MonitorService
实时监控 API 状态，判断是否需要告警（status != 200 OK），管理监控日志。

#### KnowledgeBaseService
加载 Markdown/TXT 知识库文档，与 AgentScope RAG 集成，进行文档分块和向量化。

### 工具层

#### FeishuWebhookTool
发送富文本卡片告警到飞书，支持时间戳、错误代码、延迟信息格式。

#### ApifoxApiTool
自动创建故障记录文档，文档标题格式：`[故障记录] YYYY-MM-DD HH:mm:ss`。

#### MonitorCheckTool
- `check_monitor_status`: 检查系统状态
- `get_monitor_logs`: 获取监控日志
- `is_api_healthy`: 检查 API 健康状况

## 使用场景

### 1. 智能问答

用户可以向系统提问业务相关问题，Agent 会从知识库中检索相关信息并生成回答。

**示例问题**：
- "胜算云平台有哪些计费模式？"
- "如何调用 AI 模型 API？"
- "平台支持哪些功能？"

### 2. 状态监控

系统持续监控关键 API 的状态，当检测到异常时自动触发告警流程。

**监控指标**：
- API 响应状态码
- 响应时间
- 错误率

### 3. 自动告警

当 API 状态异常时，系统会：
1. 调用 `FeishuWebhookTool` 发送告警到飞书
2. 调用 `ApifoxApiTool` 创建故障记录文档
3. 记录监控日志

### 4. 系统状态查询

用户可以查询系统状态，Agent 会基于真实的监控日志数据回答。

**示例问题**：
- "系统最近有故障吗？"
- "API 状态如何？"
- "最近一次告警是什么时候？"

## API 文档

详细的 API 文档请参考 [API 文档](docs/API.md)。

| 端点 | 方法 | 描述 |
|-------|------|------|
| `/api/process` | POST | 流式处理请求（SSE） |
| `/api/health` | GET | 健康检查 |
| `/api/monitor/status` | GET | 获取监控状态 |
| `/api/session/reset/{caseId}` | POST | 重置指定会话 |

## 开发

### 后端开发

```bash
cd backend

# 运行测试
mvn test

# 打包
mvn package

# 跳过测试打包
mvn package -DskipTests
```

**ResultServiceTest 测试指南**：

详细的测试执行说明请参考 [ResultServiceTest 测试指南](docs/ResultServiceTest测试指南.md)，包含：
- 测试用例说明
- Maven 测试命令详解
- 测试结果查看
- 常见问题解决
- CI/CD 集成示例

### 前端开发

```bash
cd frontend

# 运行测试
npm test

# 构建
npm run build

# 预览构建结果
npm run preview
```

### 知识库管理

知识库文件位于 `backend/src/main/resources/knowledge/` 目录。

支持的文件格式：
- Markdown (.md)
- 纯文本 (.txt)

添加新知识后需要重启后端服务以重新加载。

## 常见问题

### 1. LLM 响应很慢

- 检查 `LLM_BASE_URL` 配置是否正确
- 尝试使用更快的模型（如 `gpt-4-turbo` 而非 `gpt-4`）
- 考虑使用本地部署的 LLM（如 Ollama）

### 2. 飞书告警未收到

- 确认 `FEISHU_WEBHOOK_URL` 配置正确
- 检查网络连接和防火墙设置
- 查看后端日志确认是否成功调用

### 3. 知识库检索不准确

- 检查知识库文档内容是否清晰准确
- 考虑调整分块大小和重叠度
- 优化 Embedding 模型选择

### 4. 前端无法连接后端

- 确认后端服务已启动
- 检查 CORS 配置（已默认配置允许所有来源）
- 确认前端 API 地址配置正确

## 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送 Pull Request
- 联系项目维护者

## 相关资源

- [AgentScope 官方文档](https://github.com/agentscope-ai/agentscope-java)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [React 文档](https://react.dev/)
- [Ant Design 组件库](https://ant.design/)
