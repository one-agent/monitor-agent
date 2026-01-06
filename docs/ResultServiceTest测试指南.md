# ResultServiceTest 测试指南

本文档说明如何使用 Maven 执行 ResultServiceTest 测试用例。

## 测试说明

`ResultServiceTest` 是 `ResultService` 的集成测试，用于测试批量处理功能。该测试：

- 使用真实的 Spring Boot 环境
- 不使用 mock 对象
- 测试 `ResultService.processBatch()` 方法
- 验证从输入 JSON 文件读取用例并处理生成结果 JSON 文件的功能

## 运行测试

### 前置条件

确保满足以下环境要求：

1. **Java 环境**: Java 17+
2. **Maven 环境**: Maven 3.8+
3. **项目依赖**: 已执行 `mvn clean install` 安装依赖
4. **环境变量**: 配置必需的环境变量（见下方说明）

## 环境变量配置

**重要**：在运行 Maven 测试之前，必须先设置必需的环境变量。这些变量用于配置 LLM 和 Embedding API。

### Linux/Mac 配置方法

```bash
# 方式1: 命令行临时设置（推荐用于测试）
cd backend

export LLM_API_KEY="your-llm-api-key-here"
export LLM_BASE_URL="https://router.shengsuanyun.com/api/v1"
export LLM_MODEL_NAME="bigmodel/glm-4.7"

export EMBEDDING_API_KEY="your-embedding-api-key-here"
export EMBEDDING_BASE_URL="https://router.shengsuanyun.com/api/v1"
export EMBEDDING_MODEL_NAME="openai/text-embedding-3-small"

export FEISHU_WEBHOOK_URL="your-feishu-webhook-url"
export APIFOX_API_TOKEN="your-apifox-token"
export APIFOX_PROJECT_ID="your-project-id"

# 然后运行测试
mvn test -Dtest=ResultServiceTest
```

```bash
# 方式2: 创建 .env 文件（推荐用于开发）
cd backend

# 创建 .env 文件
cat > .env << 'EOF'
LLM_API_KEY=your-llm-api-key-here
LLM_BASE_URL=https://router.shengsuanyun.com/api/v1
LLM_MODEL_NAME=bigmodel/glm-4.7

EMBEDDING_API_KEY=your-embedding-api-key-here
EMBEDDING_BASE_URL=https://router.shengsuanyun.com/api/v1
EMBEDDING_MODEL_NAME=openai/text-embedding-3-small

FEISHU_WEBHOOK_URL=your-feishu-webhook-url
APIFOX_API_TOKEN=your-apifox-token
APIFOX_PROJECT_ID=your-project-id
EOF

# 加载环境变量
export $(cat .env | xargs)

# 运行测试
mvn test -Dtest=ResultServiceTest
```

### Windows PowerShell 配置方法

```powershell
# 进入 backend 目录
cd backend

# 设置环境变量
$env:LLM_API_KEY="your-llm-api-key-here"
$env:LLM_BASE_URL="https://router.shengsuanyun.com/api/v1"
$env:LLM_MODEL_NAME="bigmodel/glm-4.7"

$env:EMBEDDING_API_KEY="your-embedding-api-key-here"
$env:EMBEDDING_BASE_URL="https://router.shengsuanyun.com/api/v1"
$env:EMBEDDING_MODEL_NAME="openai/text-embedding-3-small"

$env:FEISHU_WEBHOOK_URL="your-feishu-webhook-url"
$env:APIFOX_API_TOKEN="your-apifox-token"
$env:APIFOX_PROJECT_ID="your-project-id"

# 运行测试
mvn test -Dtest=ResultServiceTest
```

### Windows CMD 配置方法

```cmd
REM 进入 backend 目录
cd backend

REM 设置环境变量
set LLM_API_KEY=your-llm-api-key-here
set LLM_BASE_URL=https://router.shengsuanyun.com/api/v1
set LLM_MODEL_NAME=bigmodel/glm-4.7

set EMBEDDING_API_KEY=your-embedding-api-key-here
set EMBEDDING_BASE_URL=https://router.shengsuanyun.com/api/v1
set EMBEDDING_MODEL_NAME=openai/text-embedding-3-small

set FEISHU_WEBHOOK_URL=your-feishu-webhook-url
set APIFOX_API_TOKEN=your-apifox-token
set APIFOX_PROJECT_ID=your-project-id

REM 运行测试
mvn test -Dtest=ResultServiceTest
```

### 环境变量说明

| 变量名 | 说明 | 示例值 | 是否必需 |
|--------|------|--------|---------|
| `LLM_API_KEY` | LLM API 密钥 | `your-api-key-here` | 是 |
| `LLM_BASE_URL` | LLM 基础 URL | `https://router.shengsuanyun.com/api/v1` | 是 |
| `LLM_MODEL_NAME` | LLM 模型名称 | `bigmodel/glm-4.7` | 是 |
| `EMBEDDING_API_KEY` | Embedding API 密钥 | `your-embedding-api-key-here` | 是 |
| `EMBEDDING_BASE_URL` | Embedding 基础 URL | `https://router.shengsuanyun.com/api/v1` | 是 |
| `EMBEDDING_MODEL_NAME` | Embedding 模型名称 | `openai/text-embedding-3-small` | 是 |
| `FEISHU_WEBHOOK_URL` | 飞书 Webhook URL | `https://open.feishu.cn/...` | 是 |
| `APIFOX_API_TOKEN` | Apifox API Token | `your-apifox-token` | 是 |
| `APIFOX_PROJECT_ID` | Apifox 项目 ID | `your-project-id` | 是 |

### 验证环境变量

在运行测试前，可以验证环境变量是否设置成功：

```bash
# Linux/Mac
echo "LLM_API_KEY: $LLM_API_KEY"
echo "LLM_BASE_URL: $LLM_BASE_URL"

# Windows PowerShell
echo "LLM_API_KEY: $env:LLM_API_KEY"
echo "LLM_BASE_URL: $env:LLM_BASE_URL"
```

### 基础测试命令

```bash
# 进入 backend 目录
cd backend

# 运行 ResultServiceTest 测试
mvn test -Dtest=ResultServiceTest
```

### 常用测试命令

```bash
# 运行所有测试方法
mvn test -Dtest=ResultServiceTest

# 运行指定测试方法
mvn test -Dtest=ResultServiceTest#testProcessBatch_WithMultipleCases_ShouldProcessAll

# 运行测试并显示详细输出
mvn test -Dtest=ResultServiceTest -X

# 运行测试并跳过主代码编译（已编译情况下更快）
mvn test -Dtest=ResultServiceTest -Dmaven.main.skip=true

# 运行测试并生成测试报告
mvn test -Dtest=ResultServiceTest surefire-report:report

# 清理后运行测试
mvn clean test -Dtest=ResultServiceTest
```

### Maven 测试参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `-Dtest=ClassName` | 指定测试类 | `mvn test -Dtest=ResultServiceTest` |
| `-Dtest=ClassName#methodName` | 指定测试方法 | `mvn test -Dtest=ResultServiceTest#testProcessBatch_WithMultipleCases_ShouldProcessAll` |
| `-DskipTests` | 跳过所有测试 | `mvn package -DskipTests` |
| `-Dmaven.test.skip=true` | 完全跳过测试编译和执行 | `mvn package -Dmaven.test.skip=true` |
| `-Dmaven.main.skip=true` | 跳过主代码编译 | `mvn test -Dmaven.main.skip=true` |
| `-X` | 显示详细调试信息 | `mvn test -X` |

## 测试说明

### 测试文件位置

```
backend/src/test/java/com/oneagent/monitor/service/ResultServiceTest.java
```

### 测试数据文件

测试运行时会自动创建以下文件：

- **输入文件**: `backend/inputs/inputs.json` - 测试用例输入数据
- **输出文件**: `backend/outputs/results.json` - 测试处理结果

### 测试用例

#### testProcessBatch_WithMultipleCases_ShouldProcessAll

测试批量处理多个用例的功能：

1. 准备 5 个测试用例，包含不同的场景：
   - C001: 正常查询，API 正常
   - C002: 查询基础版，API 500 错误
   - C005: 查询恢复状态，API 503 错误
   - C003: 查询恢复状态，API 正常
   - C004: 查询客服电话，API 正常

2. 调用 `ResultService.processBatch()` 方法

3. 验证生成了正确数量的结果

4. 输出处理耗时信息

## 查看测试结果

### 测试输出位置

测试完成后，相关文件和报告位于：

- **测试报告**: `backend/target/surefire-reports/`
- **输出结果**: `backend/outputs/results.json`

### 查看测试报告

```bash
# 查看测试报告 XML 文件
cat target/surefire-reports/TEST-com.oneagent.monitor.service.ResultServiceTest.xml

# 查看文本格式报告
cat target/surefire-reports/com.oneagent.monitor.service.ResultServiceTest.txt

# 使用浏览器查看 HTML 报告
mvn test -Dtest=ResultServiceTest surefire-report:report
open target/site/surefire-report.html
```

## 常见问题

### 1. 未设置环境变量导致测试失败

**问题**: 测试失败，提示 LLM 或 Embedding API 密钥为空

**解决方案**:
```bash
# 确保在运行测试前设置了环境变量
export LLM_API_KEY="your-api-key-here"
export EMBEDDING_API_KEY="your-api-key-here"

# 验证环境变量是否设置成功
echo "LLM_API_KEY: $LLM_API_KEY"
echo "EMBEDDING_API_KEY: $EMBEDDING_API_KEY"

# 然后再运行测试
mvn test -Dtest=ResultServiceTest
```

### 2. 测试失败提示文件不存在

**问题**: 提示 `inputs/inputs.json` 或 `outputs/results.json` 不存在

**解决方案**:
- 测试会在运行时自动创建这些文件和目录
- 确保项目根目录有写权限

### 3. 测试超时

**问题**: 测试执行时间过长或超时

**解决方案**:
- 检查 LLM API 配置是否正确
- 检查网络连接
- 使用更快的模型

### 4. 依赖缺失

**问题**: 提示类找不到或依赖缺失

**解决方案**:
```bash
# 清理并重新安装依赖
mvn clean install
```

### 5. 测试报告乱码

**问题**: 测试报告中文字符显示异常

**解决方案**:
- 确保系统编码为 UTF-8
- 在命令行中设置编码: `export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"`

## 集成到 CI/CD

### GitHub Actions 示例

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run ResultServiceTest
        run: |
          cd backend
          mvn test -Dtest=ResultServiceTest
```

## 相关文档

- [Maven Surefire Plugin 文档](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JUnit 6 用户指南](https://docs.junit.org/6.0.1/overview.html)
- [Spring Boot 测试文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
