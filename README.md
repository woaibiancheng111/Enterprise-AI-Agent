# Enterprise AI Agent

企业级 HR & 行政事务智能助手，基于 Spring Boot + Spring AI Alibaba 构建，提供智能问答、RAG 知识库检索、会话记忆与结构化工单生成能力。

> 当前版本：**0.0.1-SNAPSHOT**

---

## 功能特性

- **智能问答** - 基于企业知识库的自然语言问答，覆盖请假、报销、福利、考勤、离职、绩效、办公设施、信息安全等场景
- **RAG 知识库** - 混合检索（向量 + BM25 + RRF 重排序），支持文档上传、删除与动态重载
- **查询改写** - 同义词扩展、拼写纠正、停用词过滤，提升检索召回率
- **意图识别** - 自动识别 6 类用户意图：政策查询、流程申请、状态咨询、意见反馈、一般咨询
- **会话记忆** - Kryo 序列化文件持久化，支持跨重启的对话连续性
- **敏感词过滤** - 敏感词拦截、请求响应日志、查询增强等拦截器链
- **流式输出** - 支持 SSE 流式响应，提升交互体验
- **结构化工单** - 将对话内容转换为结构化 `EmployeeTicket` 对象，便于接入工单系统

---

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5.11 |
| 运行时 | Java 21 |
| AI 集成 | Spring AI 1.0.0-M6 + Spring AI Alibaba 1.0.0-M6.1 |
| 大模型 | 阿里云 DashScope（Qwen 系列） |
| 文档解析 | Spring AI Markdown 文档加载器 |
| 向量存储 | Spring AI SimpleVectorStore（内存向量数据库） |
| 序列化 | Kryo 5.6.2 |
| API 文档 | Springdoc OpenAPI + Knife4j |
| 工具库 | Hutool 5.8.38 |
| 构建工具 | Maven |

---

## 项目结构

```
src/main/java/com/shixi/
├── EnterpriseAiAgentApplication.java     # Spring Boot 启动类
├── app/
│   └── EnterpriseApp.java               # 核心聊天逻辑（ChatResponse）
├── controller/
│   ├── EnterpriseController.java        # 基础对话接口
│   ├── KnowledgeBaseController.java     # 知识库管理接口
│   └── EnhancedRagController.java       # 增强 RAG 接口
├── rag/
│   ├── model/
│   │   ├── HybridSearchConfig.java      # 混合检索配置模型
│   │   ├── SearchResult.java            # 检索结果模型
│   │   └── RetrievalContext.java        # RAG 上下文模型
│   └── service/
│       ├── EnhancedRagService.java      # RAG 核心编排
│       ├── HybridSearchService.java     # 混合检索（向量 + BM25 + RRF）
│       ├── RerankService.java           # 结果重排序
│       └── QueryRewriter.java           # 查询改写与意图识别
├── advisor/
│   ├── BlockedWordAdvisor.java         # 敏感词拦截
│   ├── MyLogAdvisor.java               # 请求日志
│   └── ReReadingAdvisor.java            # 查询重复增强
├── memory/
│   └── FileBasedChatMemory.java         # Kryo 文件会话存储
└── service/
    └── DocumentUploadService.java       # 文档上传处理
```

---

## API 接口

### 基础对话

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/enterprise/health` | 健康检查 |
| GET | `/api/enterprise/chat` | 基础对话（含记忆） |
| GET | `/api/enterprise/rag-chat` | 知识库增强对话 |
| GET | `/api/enterprise/ticket` | 生成结构化工单 |
| GET | `/api/enterprise/chat/stream` | 流式对话 |
| GET | `/api/enterprise/rag-chat/stream` | 流式知识库对话 |

### 增强 RAG

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/rag/chat` | 完整 RAG 流程对话 |
| GET | `/api/rag/chat/stream` | 流式 RAG 对话 |
| GET | `/api/rag/search` | 独立知识库检索 |
| GET | `/api/rag/intent` | 意图识别测试 |
| GET | `/api/rag/rewrite` | 查询改写测试 |

### 知识库管理

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/knowledge/list` | 列出所有文档 |
| POST | `/api/knowledge/upload` | 上传文档 |
| DELETE | `/api/knowledge/file/{filename}` | 删除文档 |
| POST | `/api/knowledge/reload` | 重载知识库 |

> API 文档地址：`http://localhost:8123/swagger-ui.html`

---

## 知识库

预置文档位于 `src/main/resources/documents/`，涵盖以下场景：

| 文档 | 说明 |
|------|------|
| `hr-leave-rules.md` | 请假制度（年假、病假、事假、婚假、产假等） |
| `employee-benefits.md` | 员工福利（薪酬、保险、体检、补贴、培训） |
| `expense-reimbursement.md` | 费用报销制度 |
| `attendance-rules.md` | 考勤打卡规则 |
| `resignation-procedure.md` | 离职流程指引 |
| `performance-review.md` | 绩效考核制度 |
| `office-facilities.md` | 办公设施使用说明 |
| `information-security.md` | 信息安全与 IT 规范 |

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- 阿里云 DashScope API Key（[获取地址](https://dashscope.console.aliyun.com/)）

### 配置

创建或修改 `src/main/resources/application-local.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
      chat-options:
        model: qwen-flash
```

### 构建与运行

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/enterprise-ai-agent-0.0.1-SNAPSHOT.jar
```

服务启动后访问：
- API 地址：http://localhost:8123/api
- Swagger 文档：http://localhost:8123/swagger-ui.html

### 示例请求

```bash
# 基础对话
curl "http://localhost:8123/api/enterprise/chat?message=%E5%B9%B4%E5%81%87%E5%A4%9A%E5%B0%91%E5%A4%A9"

# 知识库对话
curl "http://localhost:8123/api/enterprise/rag-chat?message=%E5%A6%82%E4%BD%95%E7%94%B3%E8%AF%B7%E5%B9%B4%E5%81%87"

# 意图识别
curl "http://localhost:8123/api/rag/intent?query=%E6%88%91%E6%83%B3%E7%94%B3%E8%AF%B7%E4%B8%80%E5%A4%A9%E5%B9%B4%E5%81%87"

# 查询改写
curl "http://localhost:8123/api/rag/rewrite?query=%E5%B9%B4%E5%81%87%E6%80%8E%E4%B9%88%E7%94%B3"

# 上传文档
curl -X POST "http://localhost:8123/api/knowledge/upload" \
  -F "file=@/path/to/your/document.md"
```

---

## RAG 工作流程

```
用户查询
    │
    ▼
┌─────────────┐     ┌─────────────────┐
│ QueryRewriter │ ──▶│ 意图识别 + 查询改写 │
└─────────────┘     └────────┬────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
      ┌──────────────┐ ┌──────────┐ ┌──────────────┐
      │ 向量相似度检索 │ │BM25 关键词│ │  参考资料生成 │
      └──────┬───────┘ └────┬─────┘ └──────────────┘
             │               │
             └───────┬───────┘
                     ▼
            ┌────────────────┐
            │ RRF 结果融合   │
            └───────┬────────┘
                    ▼
            ┌────────────────┐
            │  Rerank 重排序  │
            └───────┬────────┘
                    ▼
            ┌────────────────┐
            │ LLM 生成回复    │
            └───────┬────────┘
                    ▼
              流式响应 / 结构化工单
```

---

## 会话存储

对话历史通过 Kryo 序列化存储于 `data/chat-memory/` 目录，每个会话对应一个独立的 `.kryo` 文件，基于会话 ID 进行线程安全隔离。

---

## 系统提示词

助手扮演专业 HR 助理角色，具备以下能力：
- 回答公司制度相关问题
- 引导员工完成各项流程申请
- 对薪资、机密、领导隐私等敏感信息保密
- 生成结构化工单以便后续处理

---

## License

Private Enterprise Project - All Rights Reserved
