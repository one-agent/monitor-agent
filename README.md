# 智能客服监控 Agent

基于 AgentScope Java 框架实现的具备监控感知能力的智能客服系统。

## 功能特性

- **RAG 智能问答**：基于本地知识库回答业务问题
- **状态监控**：实时监听 API 状态
- **飞书告警**：API 异常时自动发送富文本卡片告警
- **Apifox 文档同步**：自动生成故障记录文档
- **自我感知**：回答稳定性问题时引用真实的监控日志数据

## 项目结构

```
monitor-agent/
├── pom.xml                                    # Maven 依赖配置
├── src/
│   └── main/
│       ├── java/com/oneagent/monitor/
│       │   ├── MonitorAgentApplication.java      # Spring Boot 主类
│       │   ├── agent/
│       │   │   ├── AgentConfig.java           # Agent 配置类
│       │   ├── controller/
│       │   │   └── ChatController.java         # REST API 控制器
│       │   ├── service/
│       │   │   ├── ChatService.java            # 对话服务
│       │   │   ├── MonitorService.java          # 监控服务
│       │   │   ├── ResultService.java           # 结果输出服务
│       │   │   └── KnowledgeBaseService.java   # 知识库服务
│       │   ├── tool/
│       │   │   ├── FeishuWebhookTool.java     # 飞书 Webhook 工具
│       │   │   ├── ApifoxApiTool.java          # Apifox API 工具
│       │   │   ├── MonitorCheckTool.java        # 监控检查工具
│       │   │   └── KnowledgeQueryTool.java     # 知识库查询工具
│       │   └── model/
│       │       ├── dto/                        # 数据传输对象
│       │       ├── entity/                      # 实体类
│       │       └── config/                      # 配置类
│       └── resources/
│           ├── application.properties               # 应用配置
│           └── knowledge/
│               └── shengsuan_cloud_kb.md        # 胜算云知识库
├── inputs/                                    # 输入文件目录
│   └── inputs.json
└── outputs/                                   # 输出文件目录
    └── results.json
```

## 环境要求

- JDK 17+
- Maven 3.6+

## 配置说明

编辑 `src/main/resources/application.properties` 文件，配置以下参数：

### LLM 配置（必填）

```properties
llm.api-key=your-api-key-here
llm.base-url=http://your-llm-endpoint/v1
llm.model-name=your-model-name
```

### 飞书 Webhook 配置（可选）

```properties
feishu.webhook.url=https://open.feishu.cn/open-apis/bot/v2/hook/your-webhook-id
```

### Apifox API 配置（可选）

```properties
apifox.api-token=your-apifox-token-here
apifox.project-id=your-project-id-here
apifox.folder-id=your-folder-id-here
```

## 运行方式

### 方式一：使用 Maven 运行

```bash
mvn spring-boot:run
```

### 方式二：打包后运行

```bash
mvn clean package
java -jar target/monitor-agent-1.0.0.jar
```

## API 端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/chat` | POST | 简单对话 |
| `/api/process` | POST | 处理单个测试用例 |
| `/api/process-batch` | POST | 批量处理测试用例 |
| `/api/monitor/status` | GET | 获取当前监控状态 |
| `/api/health` | GET | 健康检查 |
| `/api/info` | GET | 获取 API 信息 |

### 示例：简单对话

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "你们平台的计费模式是怎样的？"}'
```

### 示例：批量处理

```bash
curl -X POST http://localhost:8080/api/process-batch
```

处理结果将写入 `outputs/results.json`。

## 输入格式

`inputs/inputs.json` 格式：

```json
[
    {
        "case_id": "C001",
        "user_query": "你们平台的计费模式是怎样的？",
        "api_status": "200 OK",
        "api_response_time": "120ms",
        "monitor_log": []
    },
    {
        "case_id": "C002",
        "user_query": "刚才模型是不是挂了？怎么一直没反应？",
        "api_status": "500 Internal Server Error",
        "api_response_time": "Timeout",
        "monitor_log": [
            {
                "timestamp": "10:00:01",
                "status": "Error",
                "msg": "Connection Refused"
            }
        ]
    }
]
```

## 输出格式

`outputs/results.json` 格式：

```json
[
    {
        "case_id": "C001",
        "reply": "根据平台文档，我们提供按量付费和包月订阅两种模式...",
        "action_triggered": null
    },
    {
        "case_id": "C002",
        "reply": "非常抱歉，检测到模型 API 在 10:00:01 出现了短暂的连接异常...",
        "action_triggered": {
            "feishu_webhook": "Sent success",
            "apifox_doc_id": "DOC_20241212_ERROR_01"
        }
    }
]
```

## 评分标准对照

| 维度 | 分值 | 说明 |
|------|------|------|
| 问答准确性 | 30 | 能够准确根据知识库回答业务问题，无幻觉 |
| 监控灵敏度 | 30 | 能准确识别 API 异常状态，不漏报、不误报 |
| 工具链集成 | 30 | 飞书 Webhook 消息格式正确；Apifox 文档成功生成 |
| 自我感知 | 10 | 在回答稳定性问题时，能正确引用监控数据 |

## 技术栈

- Spring Boot 3.2.0
- AgentScope Java 1.0.3
- Project Reactor (WebFlux)
- OkHttp4
- Lombok

## 许可证

MIT License
