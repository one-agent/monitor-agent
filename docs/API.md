# API 文档

## 目录

- [概述](#概述)
- [通用说明](#通用说明)
- [API 端点](#api-端点)
- [数据模型](#数据模型)
- [错误处理](#错误处理)
- [示例代码](#示例代码)

---

## 概述

智能客服监控系统提供 RESTful API，支持流式响应和标准 HTTP 请求。所有 API 端点均基于 Spring WebFlux 实现，具有高性能的异步非阻塞特性。

### 基本信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8081` |
| API 版本 | v1 |
| 响应格式 | JSON / SSE 流 |
| 编码 | UTF-8 |

---

## 通用说明

### 认证

当前版本暂未实现认证机制，所有端点均可直接访问。生产环境建议添加以下认证方式之一：

- JWT Token 认证
- API Key 认证
- OAuth 2.0

### 响应格式

#### JSON 响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

#### SSE 流式响应

SSE（Server-Sent Events）用于实时推送数据，格式如下：

```
data: {"type": "message", "content": "Hello"}

data: {"type": "tool", "toolName": "monitor_check", "result": "OK"}

data: {"type": "done", "final": true}
```

### 请求头

| 请求头 | 说明 | 示例 |
|--------|------|------|
| `Content-Type` | 请求内容类型 | `application/json` |
| `Accept` | 接受的响应类型 | `text/event-stream` |
| `User-Agent` | 客户端标识 | `Monitor-Agent-Client/1.0` |

---

## API 端点

### 1. 流式处理请求

通过 SSE 实时推送 Agent 的响应和推理过程。

#### 端点信息

| 项目 | 值 |
|------|-----|
| URL | `/api/process` |
| 方法 | `POST` |
| 内容类型 | `application/json` |
| 响应类型 | `text/event-stream` |

#### 请求参数

**请求体**:

```json
{
  "query": "系统最近有故障吗？",
  "caseId": "session-123",
  "userId": "user-456"
}
```

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `query` | String | 是 | 用户查询内容 |
| `caseId` | String | 否 | 会话 ID，用于追踪对话上下文 |
| `userId` | String | 否 | 用户 ID |

#### 响应

SSE 流式响应包含以下类型的事件：

**1. Message 事件**

Agent 生成的消息内容

```json
data: {
  "type": "message",
  "content": "根据监控日志，系统最近运行正常。"
}
```

**2. Tool 事件**

工具调用结果

```json
data: {
  "type": "tool",
  "toolName": "monitor_check",
  "result": {
    "status": "OK",
    "logs": [...]
  }
}
```

**3. Thought 事件**

Agent 的思考过程

```json
data: {
  "type": "thought",
  "content": "用户询问系统状态，我需要调用监控工具检查"
}
```

**4. Done 事件**

响应完成

```json
data: {
  "type": "done",
  "final": true
}
```

**5. Error 事件**

错误信息

```json
data: {
  "type": "error",
  "message": "Failed to connect to LLM service"
}
```

#### 示例

**请求**:

```bash
curl -N -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{
    "query": "胜算云平台有哪些计费模式？",
    "caseId": "test-session-001"
  }'
```

**响应**:

```
data: {"type":"thought","content":"用户询问计费模式，需要检索知识库"}

data: {"type":"tool","toolName":"retrieval","result":[...]}

data: {"type":"message","content":"胜算云平台提供以下计费模式："}

data: {"type":"message","content":"1. 按量付费模式"}

data: {"type":"message","content":"2. 包月订阅模式"}

data: {"type":"done","final":true}
```

---

### 2. 健康检查

检查服务是否正常运行。

#### 端点信息

| 项目 | 值 |
|------|-----|
| URL | `/api/health` |
| 方法 | `GET` |
| 响应类型 | `application/json` |

#### 响应

**成功响应 (200 OK)**:

```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "components": {
    "llm": {
      "status": "UP",
      "details": {
        "model": "gpt-3.5-turbo",
        "baseUrl": "https://api.openai.com/v1"
      }
    },
    "database": {
      "status": "UP"
    }
  }
}
```

**失败响应 (503 Service Unavailable)**:

```json
{
  "status": "DOWN",
  "timestamp": "2024-01-15T10:30:00Z",
  "components": {
    "llm": {
      "status": "DOWN",
      "details": {
        "error": "Failed to connect to LLM service"
      }
    }
  }
}
```

#### 示例

```bash
curl http://localhost:8081/api/health
```

---

### 3. 获取监控状态

获取当前系统监控状态和日志。

#### 端点信息

| 项目 | 值 |
|------|-----|
| URL | `/api/monitor/status` |
| 方法 | `GET` |
| 响应类型 | `application/json` |

#### 查询参数

| 参数 | 类型 | 必需 | 默认值 | 说明 |
|------|------|------|--------|------|
| `limit` | Integer | 否 | 10 | 返回日志条数限制 |
| `offset` | Integer | 否 | 0 | 偏移量，用于分页 |

#### 响应

```json
{
  "status": "OK",
  "timestamp": "2024-01-15T10:30:00Z",
  "monitor": {
    "apiStatus": 200,
    "latency": 150,
    "isHealthy": true,
    "lastCheck": "2024-01-15T10:29:55Z"
  },
  "logs": [
    {
      "id": 1,
      "timestamp": "2024-01-15T10:29:55Z",
      "apiUrl": "https://api.example.com/health",
      "statusCode": 200,
      "latency": 150,
      "message": "OK"
    },
    {
      "id": 2,
      "timestamp": "2024-01-15T10:28:55Z",
      "apiUrl": "https://api.example.com/health",
      "statusCode": 200,
      "latency": 145,
      "message": "OK"
    }
  ],
  "totalLogs": 100
}
```

#### 示例

```bash
curl "http://localhost:8081/api/monitor/status?limit=5&offset=0"
```

---

### 4. 重置会话

重置指定会话的上下文和状态。

#### 端点信息

| 项目 | 值 |
|------|-----|
| URL | `/api/session/reset/{caseId}` |
| 方法 | `POST` |
| 响应类型 | `application/json` |

#### 路径参数

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `caseId` | String | 是 | 会话 ID |

#### 响应

**成功响应 (200 OK)**:

```json
{
  "code": 200,
  "message": "Session reset successfully",
  "data": {
    "caseId": "test-session-001",
    "resetAt": "2024-01-15T10:30:00Z"
  }
}
```

**失败响应 (404 Not Found)**:

```json
{
  "code": 404,
  "message": "Session not found",
  "data": null
}
```

#### 示例

```bash
curl -X POST http://localhost:8081/api/session/reset/test-session-001
```

---

## 数据模型

### InputCase

用户输入用例

```typescript
interface InputCase {
  query: string;        // 用户查询内容
  caseId?: string;      // 会话 ID（可选）
  userId?: string;      // 用户 ID（可选）
}
```

### ResultCase

结果用例

```typescript
interface ResultCase {
  caseId: string;           // 会话 ID
  query: string;            // 用户查询
  answer: string;          // Agent 回答
  actions: Action[];       // 执行的操作
  timestamp: string;       // 时间戳
}
```

### MonitorLog

监控日志

```typescript
interface MonitorLog {
  id: number;              // 日志 ID
  timestamp: string;       // 时间戳
  apiUrl: string;         // 监控的 API URL
  statusCode: number;     // HTTP 状态码
  latency: number;        // 延迟（毫秒）
  message: string;        // 状态消息
}
```

### ActionTriggered

触发的操作

```typescript
interface ActionTriggered {
  toolName: string;       // 工具名称
  action: string;         // 操作类型
  timestamp: string;      // 时间戳
  result?: any;          // 操作结果
}
```

### MonitorStatus

监控状态

```typescript
interface MonitorStatus {
  apiStatus: number;      // API 状态码
  latency: number;        // 延迟（毫秒）
  isHealthy: boolean;     // 是否健康
  lastCheck: string;      // 最后检查时间
}
```

---

## 错误处理

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

### 错误响应格式

```json
{
  "code": 400,
  "message": "Invalid request parameter",
  "details": "Query parameter is required",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 常见错误

#### 1. 参数错误 (400)

```json
{
  "code": 400,
  "message": "Invalid request parameter",
  "details": "Query parameter is required and cannot be empty"
}
```

#### 2. 服务不可用 (503)

```json
{
  "code": 503,
  "message": "Service temporarily unavailable",
  "details": "LLM service is not responding"
}
```

#### 3. 内部错误 (500)

```json
{
  "code": 500,
  "message": "Internal server error",
  "details": "Failed to process request",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 示例代码

### Python 示例

```python
import requests
import sseclient

# SSE 流式请求示例
def stream_query(query: str, case_id: str):
    url = "http://localhost:8081/api/process"
    payload = {
        "query": query,
        "caseId": case_id
    }

    response = requests.post(
        url,
        json=payload,
        stream=True,
        headers={'Accept': 'text/event-stream'}
    )

    client = sseclient.SSEClient(response)
    for event in client.events():
        print(event.data)

# 使用示例
stream_query("胜算云平台有哪些计费模式？", "test-001")
```

### JavaScript/TypeScript 示例

```typescript
// SSE 流式请求示例
async function streamQuery(query: string, caseId: string) {
  const response = await fetch('http://localhost:8081/api/process', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query, caseId }),
  });

  const reader = response.body?.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader!.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));
        console.log(data);
      }
    }
  }
}

// 使用示例
streamQuery('系统最近有故障吗？', 'test-002');
```

### cURL 示例

```bash
# SSE 流式请求
curl -N -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{
    "query": "如何调用 AI 模型 API？",
    "caseId": "test-003"
  }'

# 健康检查
curl http://localhost:8081/api/health

# 获取监控状态
curl "http://localhost:8081/api/monitor/status?limit=10"

# 重置会话
curl -X POST http://localhost:8081/api/session/reset/test-003
```

### Java 示例

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public class MonitorClient {

    private final WebClient webClient;

    public MonitorClient(String baseUrl) {
        this.webClient = WebClient.create(baseUrl);
    }

    // SSE 流式请求
    public Flux<String> streamQuery(String query, String caseId) {
        InputCase inputCase = new InputCase();
        inputCase.setQuery(query);
        inputCase.setCaseId(caseId);

        return webClient.post()
                .uri("/api/process")
                .bodyValue(inputCase)
                .retrieve()
                .bodyToFlux(String.class);
    }

    // 健康检查
    public HealthStatus checkHealth() {
        return webClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(HealthStatus.class)
                .block();
    }

    public static void main(String[] args) {
        MonitorClient client = new MonitorClient("http://localhost:8081");

        // 流式请求
        client.streamQuery("胜算云平台有哪些计费模式？", "test-001")
                .subscribe(System.out::println);

        // 健康检查
        HealthStatus health = client.checkHealth();
        System.out.println("Health: " + health.getStatus());
    }
}
```

---

## WebSocket 支持（未来计划）

当前版本使用 SSE 实现流式响应，未来计划支持 WebSocket 以提供更灵活的双向通信。

```typescript
// WebSocket 连接示例（计划功能）
const ws = new WebSocket('ws://localhost:8081/ws/chat');

ws.onopen = () => {
  ws.send(JSON.stringify({
    type: 'query',
    query: '系统状态如何？'
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};
```

---

## API 版本管理

### 当前版本

- **版本**: v1
- **状态**: 稳定

### 版本历史

| 版本 | 发布日期 | 主要变更 |
|------|----------|----------|
| v1.0.0 | 2024-01-15 | 初始版本，支持流式响应和监控功能 |

### 废弃计划

暂无废弃的 API。

---

## 速率限制

当前版本未实现速率限制。生产环境建议配置以下限制：

| 端点 | 限制 | 时间窗口 |
|------|------|----------|
| `/api/process` | 100 次 | 1 分钟 |
| `/api/monitor/status` | 1000 次 | 1 分钟 |
| `/api/session/reset` | 10 次 | 1 分钟 |

---

## 支持

如有 API 使用问题，请：

1. 查看 [常见问题](../README.md#常见问题)
2. 提交 [Issue](https://github.com/your-org/monitor-agent/issues)
3. 查看 [架构文档](./ARCHITECTURE.md) 了解更多系统细节
