# Enterprise AI Agent

企业级 HR 与行政事务智能助手，基于 Spring Boot、Spring AI Alibaba、Vue 3 和 MCP 构建。项目面向真实员工服务台场景，支持自然语言咨询、制度检索、结构化工单、业务工具调用、多 Agent 协作和 MCP 工具集成。

> 当前版本：**0.0.1-SNAPSHOT**

---

## 项目亮点

- **数字团队协作**：通过“综合办理”入口自动判断用户诉求，编排知识库、业务工具、工单整理等能力，返回可解释的处理过程。
- **智能工具调用**：支持员工信息、假期余额、请假申请、报销、工作日计算等企业业务工具，并提供确定性编排兜底，提升调用稳定性。
- **MCP 集成**：内置企业 MCP 工具服务，可查询服务状态、工具清单，并通过统一接口执行工具调用。
- **RAG 知识库问答**：混合检索（向量 + BM25 + RRF 重排序），支持查询改写、意图识别、引用来源和流式响应。
- **JWT 登录与权限控制**：员工、HR、管理员使用独立账号登录，工具调用按账号权限访问真实员工数据。
- **用户级前端体验**：Vue 3 工作台提供多能力入口、流式回答、知识库上传、MCP 状态展示、处理过程折叠卡片和参考来源折叠卡片。
- **会话记忆**：基于 Kryo 文件持久化对话历史，支持跨重启的上下文连续性。

---

## 已实现能力

| 能力 | 说明 |
|------|------|
| 综合办理 | 多 Agent 数字团队入口，自动路由和组合处理复杂事务 |
| 日常咨询 | 面向 HR、行政、办公场景的通用问答 |
| 制度查询 | 基于企业知识库进行 RAG 问答，并返回参考来源 |
| 工单整理 | 将员工诉求整理为结构化 `EmployeeTicket` |
| 业务工具 | 调用员工、假期、请假、报销、时间计算和知识库工具 |
| MCP 集成 | 暴露 MCP 状态、工具列表、对话和工具调用接口 |
| 知识库管理 | 支持文档列表、上传、删除和动态重载 |
| 流式输出 | 支持 SSE 流式响应，提升前端交互体验 |
| 登录权限 | 支持 JWT 登录、当前用户识别、员工数据权限和审计权限控制 |

---

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.11 |
| 运行时 | Java 21 |
| AI 集成 | Spring AI 1.0.0-M6 + Spring AI Alibaba 1.0.0-M6.1 |
| 大模型 | 阿里云 DashScope（Qwen 系列） |
| MCP | Spring AI MCP Server + 本地工具桥接 |
| 业务数据 | MySQL + MyBatis Plus |
| 鉴权 | 自定义 JWT + Spring MVC Interceptor |
| RAG | SimpleVectorStore、BM25、RRF、Rerank、Query Rewrite |
| 前端 | Vue 3、Vite、TypeScript、Axios |
| Markdown 渲染 | marked + DOMPurify |
| 会话存储 | Kryo 5.6.2 |
| API 文档 | Springdoc OpenAPI + Knife4j |
| 构建工具 | Maven、npm |

---

## 项目结构

```text
.
├── frontend/                              # Vue 3 前端工作台
│   └── src/
│       ├── components/                    # 知识库上传等组件
│       ├── config/capabilities.ts         # 前端能力入口配置
│       ├── services/enterpriseApi.ts      # 后端 API 与流式请求封装
│       ├── types/enterprise.ts            # 前端业务类型
│       └── views/AgentWorkbench.vue       # 智能助手主界面
├── src/main/java/com/shixi/
│   ├── app/
│   │   └── EnterpriseApp.java             # 核心对话、RAG、工具调用入口
│   ├── business/
│   │   ├── entity/                        # 员工、假期、请假、报销实体
│   │   ├── mapper/                        # MyBatis Plus Mapper
│   │   ├── service/                       # 真实业务数据服务
│   ├── agent/
│   │   ├── DigitalTeamService.java        # 数字团队编排
│   │   ├── McpIntegrationService.java     # MCP 集成服务
│   │   └── ToolOrchestrationService.java  # 确定性工具编排兜底
│   ├── controller/
│   │   ├── AuthController.java            # JWT 登录和当前用户接口
│   │   ├── EnterpriseController.java      # 企业助手接口
│   │   ├── McpController.java             # MCP 状态、工具和调用接口
│   │   ├── KnowledgeBaseController.java   # 知识库管理接口
│   │   └── EnhancedRagController.java     # 增强 RAG 接口
│   ├── mcp/
│   │   ├── EmployeeServiceTools.java      # 员工与业务工具
│   │   ├── KnowledgeBaseTools.java        # 知识库工具
│   │   ├── TimeTools.java                 # 时间与工作日工具
│   │   └── config/McpServerConfig.java    # MCP 工具注册配置
│   ├── rag/                               # 检索、改写、重排与 RAG 编排
│   ├── advisor/                           # 日志、敏感词、查询增强 Advisor
│   ├── memory/                            # 文件会话记忆
│   ├── security/                          # JWT、当前用户上下文、权限异常和拦截器
│   └── service/                           # 文档上传处理
└── src/main/resources/
    ├── application.yml                    # 服务、MCP、RAG 配置
    └── documents/                         # 预置企业知识库文档
```

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+
- npm 9+
- 阿里云 DashScope API Key（[获取地址](https://dashscope.console.aliyun.com/)）

### 后端配置

创建或修改 `src/main/resources/application-local.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
      chat-options:
        model: qwen-flash
```

默认后端端口为 `8123`，接口统一挂载在 `/api`：

```yaml
server:
  port: 8123
  servlet:
    context-path: /api
```

项目只保留 MySQL 作为业务数据库。需要先创建一个数据库（MySQL 里常说的 schema/database），表结构不用手动建：

```sql
CREATE DATABASE enterprise_ai_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

默认连接配置位于 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/enterprise_ai_agent?serverTimezone=Asia/Shanghai&useSSL=false
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 123456

enterprise:
  security:
    jwt:
      secret: ${JWT_SECRET:enterprise-ai-agent-local-secret}
      ttl-seconds: 86400
```

启动应用时，Spring Boot 会自动执行 `schema.sql` 和 `data.sql`：

- `schema.sql`：创建员工、登录账号、假期、请假、报销和工具审计表。
- `data.sql`：使用 `INSERT IGNORE` 初始化演示员工、登录账号和假期余额，不覆盖已有数据。

因此推荐流程是：

```text
1. 手动创建 enterprise_ai_agent 数据库
2. 启动后端应用
3. 应用自动建表并写入初始化数据
```

如果你想手动执行 SQL，也可以在创建数据库后执行：

```bash
mysql -u root -p enterprise_ai_agent < src/main/resources/schema.sql
mysql -u root -p enterprise_ai_agent < src/main/resources/data.sql
```

启动时仍然激活 `local` profile 读取 DashScope Key：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

或使用 jar 运行：

```bash
java -jar target/enterprise-ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 启动后端

```bash
./mvnw spring-boot:run
```

或构建后运行：

```bash
./mvnw clean package -DskipTests
java -jar target/enterprise-ai-agent-0.0.1-SNAPSHOT.jar
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173/
```

首次进入前端需要登录，内置演示账号如下，密码均为 `123456`：

| 账号 | 角色 | 数据权限 |
|------|------|----------|
| `zhangsan` | EMPLOYEE | 只能访问员工 `E001` 的个人数据 |
| `lisi` | EMPLOYEE | 只能访问员工 `E002` 的个人数据 |
| `hr_admin` | HR | 可访问所有员工业务数据和工具审计 |
| `admin` | ADMIN | 可访问所有员工业务数据和工具审计 |

如需修改前端请求后端地址，可配置：

```bash
VITE_API_BASE_PATH=/api
```

---

## 前端功能

前端主界面位于 `frontend/src/views/AgentWorkbench.vue`，当前提供：

- 左侧能力入口：综合办理、日常咨询、制度查询、工单整理、业务工具、MCP 集成。
- 登录页、当前账号展示和退出登录。
- 消息区 Markdown 渲染与安全过滤。
- 支持 SSE 流式回答。
- 数字团队处理过程折叠卡片。
- 参考来源折叠卡片，点击展开，再次点击收起。
- MCP 服务状态和工具数量展示。
- 知识库文档上传、删除和刷新。

---

## API 接口

除健康检查和登录接口外，业务接口默认需要携带 JWT：

```http
Authorization: Bearer <token>
```

### 登录鉴权

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 使用账号密码登录，返回 JWT 和用户信息 |
| GET | `/api/auth/me` | 读取当前登录用户 |

### 企业助手

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/enterprise/health` | 健康检查 |
| GET | `/api/enterprise/chat` | 基础对话（含记忆） |
| GET | `/api/enterprise/rag-chat` | 知识库增强对话 |
| GET | `/api/enterprise/ticket` | 生成结构化工单 |
| GET | `/api/enterprise/team-chat` | 数字团队综合办理 |
| GET | `/api/enterprise/tool-chat` | 企业业务工具对话 |
| GET | `/api/enterprise/chat/stream` | 基础流式对话 |
| GET | `/api/enterprise/rag-chat/stream` | RAG 流式对话 |
| GET | `/api/enterprise/tool-chat/stream` | 工具调用流式对话 |

### MCP 集成

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/mcp/status` | MCP 服务状态、工具数量和领域 |
| GET | `/api/mcp/tools` | MCP 工具列表 |
| GET | `/api/mcp/chat` | MCP 工具增强对话 |
| POST | `/api/mcp/call` | 按工具名称直接调用 MCP 工具 |

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

API 文档地址：

```text
http://localhost:8123/api/swagger-ui.html
```

---

## 示例请求

```bash
# 登录并取得 JWT
curl -X POST "http://localhost:8123/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"zhangsan\",\"password\":\"123456\"}"

# 数字团队综合办理
curl "http://localhost:8123/api/enterprise/team-chat?message=我想申请明天下午年假，帮我看看余额并处理" \
  -H "Authorization: Bearer <token>"

# 业务工具调用
curl "http://localhost:8123/api/enterprise/tool-chat?message=查询员工E001的假期余额" \
  -H "Authorization: Bearer <token>"

# MCP 服务状态
curl "http://localhost:8123/api/mcp/status" \
  -H "Authorization: Bearer <token>"

# MCP 工具增强对话
curl "http://localhost:8123/api/mcp/chat?message=查询员工E001的信息" \
  -H "Authorization: Bearer <token>"

# 知识库对话
curl "http://localhost:8123/api/enterprise/rag-chat?message=如何申请年假" \
  -H "Authorization: Bearer <token>"

# 上传知识库文档
curl -X POST "http://localhost:8123/api/knowledge/upload" \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/your/document.md"
```

---

## RAG 工作流程

```text
用户查询
  │
  ▼
QueryRewriter
  │
  ├─ 意图识别
  ├─ 同义词扩展
  ├─ 拼写纠正
  └─ 停用词过滤
  │
  ▼
混合检索
  ├─ 向量相似度检索
  └─ BM25 关键词检索
  │
  ▼
RRF 融合 + Rerank 重排序
  │
  ▼
LLM 生成回复
  │
  ▼
引用来源 / 流式响应 / 结构化结果
```

---

## MCP 与工具能力

项目将企业工具统一注册到 MCP 工具服务中，当前覆盖：

- 员工信息查询
- 假期余额查询
- 请假申请
- 报销申请
- 日期与工作日计算
- 知识库查询

`ToolOrchestrationService` 会根据用户输入进行确定性意图匹配和参数提取，在模型工具调用失败或不稳定时提供兜底执行路径。

---

## 业务数据持久化

第一阶段已将员工、假期、请假和报销从内存模拟数据迁移到数据库：

| 数据 | 存储表 | 说明 |
|------|--------|------|
| 员工信息 | `employees` | 员工 ID、姓名、部门、职位、邮箱、电话、入职日期 |
| 假期余额 | `leave_balances` | 年假、病假、婚假、产假余额 |
| 请假申请 | `leave_applications` | 申请编号、员工、假期类型、起止日期、状态 |
| 报销申请 | `reimbursement_applications` | 申请编号、员工、类型、金额、发票号、状态 |
| 登录账号 | `employee_users` | 用户名、密码摘要、关联员工、展示名、角色、启用状态 |
| 工具审计 | `tool_call_logs` | 工具名称、调用参数、目标员工、结果摘要、成功状态、调用时间 |

工具层调用链路：

```text
EmployeeServiceTools
  -> EmployeeBusinessService
  -> MyBatis Plus Mapper
  -> MySQL
```

MySQL 数据库 `enterprise_ai_agent` 需要先手动创建；表结构由 `schema.sql` 创建，初始化数据由 `data.sql` 写入。后续通过工具提交的请假、报销申请会真实写入 MySQL，应用重启后仍可查询。

工具调用审计已接入员工服务工具，每次查询员工、查询假期、提交请假、提交报销或查询申请状态都会写入 `tool_call_logs`。后端提供审计查询接口：

```text
GET /api/audit/tool-calls
GET /api/audit/tool-calls?employeeId=E001
GET /api/audit/tool-calls?toolName=applyLeave&limit=20
```

权限规则：

- `EMPLOYEE` 只能访问自己绑定员工编号的数据，例如 `zhangsan` 只能访问 `E001`。
- `HR` 和 `ADMIN` 可以访问所有员工业务数据。
- 工具审计日志仅允许 `HR` 和 `ADMIN` 查询。
- 审计日志会记录真实调用账号的 `user_id`，不再固定为 `system`。

会话记忆隔离：

- 对话历史由 `FileBasedChatMemory` 持久化到 `data/chat-memory/`。
- 后端会用登录用户生成实际记忆空间：`user-{userId}-{chatId}`。
- 前端传入的 `chatId` 只作为当前用户内部的会话标识，不能跨用户读取或写入别人的记忆。
- 记忆文件名会再次做安全字符过滤，避免路径注入。

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

## 会话存储

对话历史通过 Kryo 序列化存储于 `data/chat-memory/` 目录。每个会话对应一个独立 `.kryo` 文件，基于会话 ID 做线程安全隔离。

---

## 开发与验证

```bash
# 后端测试
./mvnw test

# 后端编译
./mvnw -DskipTests compile

# 前端类型检查与构建
cd frontend
npm run build
```

---

## License

Private Enterprise Project - All Rights Reserved
