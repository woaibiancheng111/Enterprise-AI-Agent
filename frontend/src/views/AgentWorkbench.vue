<template>
  <div :class="['page', { 'auth-page': !currentUser }]">
    <div class="bg-orb orb-one"></div>
    <div class="bg-orb orb-two"></div>

    <section v-if="!currentUser" class="panel login-panel">
      <div class="brand login-brand">
        <span class="brand-dot"></span>
        <div>
          <h1>Enterprise AI Agent</h1>
          <p class="subtitle">请登录后使用企业 AI 助手</p>
        </div>
      </div>
      <form class="login-form" @submit.prevent="onLogin">
        <label class="field">
          <span>账号</span>
          <input v-model="loginForm.username" autocomplete="username" placeholder="zhangsan" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="123456" />
        </label>
        <button type="submit" class="primary-btn" :disabled="loginLoading">
          {{ loginLoading ? "登录中..." : "登录" }}
        </button>
        <p v-if="loginError" class="login-error">{{ loginError }}</p>
      </form>
      <div class="demo-users">
        <span>演示账号</span>
        <button type="button" @click="fillDemoUser('zhangsan')">张三</button>
        <button type="button" @click="fillDemoUser('hr_admin')">HR</button>
        <button type="button" @click="fillDemoUser('admin')">管理员</button>
      </div>
    </section>

    <aside v-else class="panel left-panel">
      <div class="brand">
        <span class="brand-dot"></span>
        <div>
          <h1>Enterprise AI Agent</h1>
          <p class="subtitle">企业 AI 数字团队工作台</p>
        </div>
      </div>

      <section class="card user-card">
        <h2>当前账号</h2>
        <div class="user-profile">
          <span>{{ currentUser.displayName.slice(0, 1) }}</span>
          <div>
            <strong>{{ currentUser.displayName }}</strong>
            <p>{{ currentUser.role }} · {{ currentUser.employeeId || "全局权限" }}</p>
          </div>
        </div>
        <button type="button" class="secondary-btn logout-btn" @click="onLogout">退出登录</button>
      </section>

      <section class="card">
        <h2>服务类型</h2>
        <div class="mode-grid">
          <button
            v-for="item in readyCapabilities"
            :key="item.key"
            type="button"
            :class="['mode-btn', { active: selectedMode === item.key }]"
            @click="onSelectMode(item.key)"
          >
            {{ item.name }}
          </button>
        </div>
      </section>

      <section class="card service-status-card">
        <h2>服务状态</h2>
        <div class="health-row">
          <button type="button" class="secondary-btn" @click="onHealthCheck">
            刷新状态
          </button>
          <p :class="['status-text', healthStatusClass]">{{ healthText }}</p>
        </div>
      </section>

      <section class="card">
        <h2>知识库管理</h2>
        <KnowledgeBaseUpload />
      </section>
    </aside>

    <main v-if="currentUser" class="panel chat-panel">
      <header class="chat-header">
        <div>
          <h2>{{ currentCapability?.name }}</h2>
          <p>{{ currentCapability?.description }}</p>
        </div>
        <div class="header-actions">
          <span class="mode-chip">{{ selectedMode }}</span>
          <span v-if="isStreaming" class="streaming-chip">
            <span class="streaming-dot"></span>
            处理中
          </span>
        </div>
      </header>

      <section class="team-cockpit">
        <div class="agent-roster">
          <article
            v-for="agent in agentRoster"
            :key="agent.code"
            :class="['agent-tile', agent.code, { active: activeAgentCodes.includes(agent.code) }]"
          >
            <span class="agent-avatar">{{ agent.avatar }}</span>
            <div>
              <strong>{{ agent.name }}</strong>
              <p>{{ agent.title }}</p>
            </div>
          </article>
        </div>
        <div v-if="activeTeamResponse" class="route-strip">
          <div>
            <span>意图</span>
            <strong>{{ activeTeamResponse.intentType }}</strong>
          </div>
          <div>
            <span>风险</span>
            <strong :class="['risk-text', activeTeamResponse.sentiment.riskLevel.toLowerCase()]">
              {{ activeTeamResponse.sentiment.riskLevel }} · {{ activeTeamResponse.sentiment.score }}
            </strong>
          </div>
          <div>
            <span>路由</span>
            <strong>{{ activeTeamResponse.route.priority }} / {{ activeTeamResponse.route.assignee }}</strong>
          </div>
          <div>
            <span>耗时</span>
            <strong>{{ activeTeamResponse.elapsedMs }}ms</strong>
          </div>
        </div>
        <div v-else-if="selectedMode === 'mcp'" class="mcp-strip">
          <div>
            <span>MCP Server</span>
            <strong>{{ mcpStatus?.serverName || "未连接" }}</strong>
          </div>
          <div>
            <span>工具数量</span>
            <strong>{{ mcpStatus?.toolCount ?? mcpTools.length }}</strong>
          </div>
          <button type="button" class="secondary-btn" @click="refreshMcpInfo">刷新 MCP</button>
        </div>
      </section>

      <div class="quick-prompts">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt"
          type="button"
          class="prompt-chip"
          @click="onUsePrompt(prompt)"
        >
          {{ prompt }}
        </button>
      </div>

      <div class="message-list" ref="messageListRef">
        <article
          v-for="message in messages"
          :key="message.id"
          :class="['message', message.role, { streaming: message.streaming }]"
        >
          <header>{{ roleLabelMap[message.role] }}</header>
          <pre v-if="message.isJson">{{ message.text }}</pre>
          <template v-else-if="message.role === 'assistant'">
            <div
              class="markdown-body"
              v-html="renderAssistantMarkdown(getAnswerText(message.text))"
            ></div>

            <details v-if="getReferenceText(message.text)" class="fold-card reference-card text-reference-card">
              <summary>
                <span>参考来源</span>
                <strong>查看引用依据</strong>
              </summary>
              <div
                class="markdown-body reference-markdown"
                v-html="renderAssistantMarkdown(getReferenceText(message.text))"
              ></div>
            </details>
          </template>
          <p v-else>{{ message.text }}</p>
          <span v-if="message.streaming" class="typing-cursor"></span>
        </article>
        <div v-if="isStreaming" class="thinking-indicator">
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
        </div>
      </div>

      <footer class="chat-input">
        <textarea
          v-model.trim="inputMessage"
          ref="textareaRef"
          placeholder="请输入你的问题，例如：我想申请年假需要什么流程？"
          :disabled="isStreaming"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="input-actions">
          <span class="input-hint">
            {{ isStreaming ? '等待回复...' : 'Enter 发送，Shift + Enter 换行' }}
          </span>
          <button
            type="button"
            class="primary-btn"
            :disabled="isStreaming || !inputMessage"
            @click="onSend"
          >
            {{ isStreaming ? '处理中...' : '发送' }}
          </button>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import DOMPurify from "dompurify";
import { marked } from "marked";
import { computed, ref, nextTick, onMounted, onUnmounted } from "vue";
import { CAPABILITIES } from "../config/capabilities";
import {
  checkHealth,
  clearAuthToken,
  getAuthToken,
  getCurrentUser,
  requestEnterprise,
  requestDigitalTeam,
  getMcpStatus,
  login,
  listMcpTools,
  createStreamController
} from "../services/enterpriseApi";
import KnowledgeBaseUpload from "../components/KnowledgeBaseUpload.vue";
import type { ChatMessage, DigitalTeamResponse, McpStatus, McpToolCard, ReadyCapability, UserProfile } from "../types/enterprise";

const capabilities = CAPABILITIES;
const readyCapabilities = capabilities.filter((item) => item.status === "ready");
marked.setOptions({
  gfm: true,
  breaks: true
});

const selectedMode = ref<ReadyCapability>("team-chat");
const chatId = ref("default-user");
const inputMessage = ref("");
const isStreaming = ref(false);
const topK = 5;
const healthText = ref("后端状态未检查");
const mcpStatus = ref<McpStatus | null>(null);
const mcpTools = ref<McpToolCard[]>([]);
const currentUser = ref<UserProfile | null>(null);
const loginLoading = ref(false);
const loginError = ref("");
const loginForm = ref({
  username: "zhangsan",
  password: "123456"
});
const messages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: "system",
    text: "欢迎使用企业 AI 助手。请选择服务类型后发送问题。"
  }
]);

const messageListRef = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
let currentStreamController: ReturnType<typeof createStreamController> | null = null;

const roleLabelMap: Record<ChatMessage["role"], string> = {
  user: "我",
  assistant: "AI 助手",
  system: "系统"
};

const agentRoster = [
  { code: "knowledge", avatar: "KB", name: "KNOWLEDGE", title: "知识库管理员" },
  { code: "responder", avatar: "RE", name: "RESPONDER", title: "智能响应员" },
  { code: "analyzer", avatar: "AZ", name: "ANALYZER", title: "风险分析官" },
  { code: "router", avatar: "RT", name: "ROUTER", title: "任务路由员" },
  { code: "tracker", avatar: "TK", name: "TRACKER", title: "满意度追踪员" }
] as const;

const currentCapability = computed(() =>
  capabilities.find((item) => item.key === selectedMode.value)
);

const activeTeamResponse = computed<DigitalTeamResponse | null>(() => {
  if (selectedMode.value !== "team-chat") {
    return null;
  }
  const latest = [...messages.value].reverse().find((item) => item.teamResponse);
  return latest?.teamResponse ?? null;
});

const activeAgentCodes = computed(() => {
  if (activeTeamResponse.value) {
    return activeTeamResponse.value.steps.map((item) => item.code);
  }
  if (selectedMode.value === "tool-chat") {
    return ["responder", "router"];
  }
  if (selectedMode.value === "mcp") {
    return ["knowledge", "responder", "router"];
  }
  if (selectedMode.value === "rag-chat") {
    return ["knowledge", "responder"];
  }
  if (selectedMode.value === "ticket") {
    return ["router", "tracker"];
  }
  return ["responder"];
});

const healthStatusClass = computed(() => {
  if (healthText.value.includes("在线")) {
    return "online";
  }
  if (healthText.value.includes("不可用")) {
    return "offline";
  }
  return "idle";
});

const quickPromptsMap: Record<ReadyCapability, string[]> = {
  "team-chat": [
    "我想申请年假，但不确定还剩几天，帮我处理一下",
    "报销一直没人处理，我有点着急，帮我看看该怎么办",
    "请按公司制度说明差旅报销流程，并判断是否需要转人工"
  ],
  chat: [
    "请介绍一下你能提供哪些帮助",
    "我想申请年假，流程是什么？",
    "报销打车费需要准备什么材料？"
  ],
  "rag-chat": [
    "请按公司制度说明请假审批流程",
    "差旅报销金额上限是多少？",
    "报销单据最晚什么时候提交？"
  ],
  ticket: [
    "帮我生成一个办公用品申领工单",
    "帮我整理请假申请的处理工单",
    "帮我创建报销异常处理工单"
  ],
  "tool-chat": [
    "帮我查询员工 E001 的基本信息",
    "帮员工 E001 申请 3 天年假，开始日期 2026-05-01",
    "今天是几号星期几？帮我算一下到月底还有多少个工作日"
  ],
  mcp: [
    "列出当前 MCP Server 上可用的工具",
    "通过 MCP 查询员工 E001 的基本信息",
    "通过 MCP 帮员工 E001 查询假期余额"
  ]
};
const quickPrompts = computed(() => quickPromptsMap[selectedMode.value]);

function appendMessage(message: Omit<ChatMessage, "id">): void {
  messages.value.push({
    id: crypto.randomUUID(),
    ...message
  });
}

function onUsePrompt(prompt: string): void {
  inputMessage.value = prompt;
  textareaRef.value?.focus();
}

function fillDemoUser(username: string): void {
  loginForm.value.username = username;
  loginForm.value.password = "123456";
}

async function onLogin(): Promise<void> {
  loginLoading.value = true;
  loginError.value = "";
  try {
    const result = await login(loginForm.value.username, loginForm.value.password);
    currentUser.value = result.user;
    chatId.value = result.user.username;
    messages.value = [
      {
        id: crypto.randomUUID(),
        role: "system",
        text: `欢迎回来，${result.user.displayName}。企业 AI 助手已连接你的账号权限。`
      }
    ];
    await onHealthCheck();
  } catch (error) {
    loginError.value = extractErrorMessage(error);
  } finally {
    loginLoading.value = false;
  }
}

function onLogout(): void {
  clearAuthToken();
  currentUser.value = null;
  chatId.value = "default-user";
  mcpStatus.value = null;
  mcpTools.value = [];
}

function onSelectMode(mode: ReadyCapability): void {
  selectedMode.value = mode;
  if (mode === "mcp") {
    refreshMcpInfo();
  }
}

async function refreshMcpInfo(): Promise<void> {
  try {
    const [status, tools] = await Promise.all([getMcpStatus(), listMcpTools()]);
    mcpStatus.value = status;
    mcpTools.value = tools;
  } catch (error) {
    appendMessage({
      role: "assistant",
      text: `MCP 信息加载失败：${extractErrorMessage(error)}`
    });
  }
}

function renderAssistantMarkdown(markdown: string): string {
  if (!markdown) return "";
  const rendered = marked.parse(markdown) as string;
  return DOMPurify.sanitize(rendered);
}

function splitReferenceSection(text: string): { answer: string; reference: string } {
  const referenceMarker = "【参考来源】";
  const markerIndex = text.lastIndexOf(referenceMarker);
  if (markerIndex === -1) {
    return { answer: text, reference: "" };
  }
  if (markerIndex === 0) {
    return splitLeadingReferenceSection(text, referenceMarker);
  }

  const beforeMarker = text.slice(0, markerIndex);
  const separatorIndex = beforeMarker.lastIndexOf("---");
  const answer = separatorIndex >= 0
    ? beforeMarker.slice(0, separatorIndex).trim()
    : beforeMarker.trim();

  if (!answer) {
    return { answer: text, reference: "" };
  }

  return {
    answer,
    reference: text.slice(markerIndex).trim()
  };
}

function splitLeadingReferenceSection(text: string, referenceMarker: string): { answer: string; reference: string } {
  const lines = text.split(/\r?\n/);
  let answerStartIndex = -1;
  let hasReferenceItem = false;

  for (let index = 1; index < lines.length; index += 1) {
    const line = lines[index].trim();
    if (!line) {
      continue;
    }
    if (isReferenceLine(line)) {
      hasReferenceItem = true;
      continue;
    }
    if (hasReferenceItem) {
      answerStartIndex = index;
      break;
    }
  }

  if (answerStartIndex === -1) {
    return { answer: "", reference: text.trim() };
  }

  const reference = lines.slice(0, answerStartIndex).join("\n").trim();
  const answer = lines.slice(answerStartIndex).join("\n").trim();
  return {
    answer,
    reference: reference || referenceMarker
  };
}

function isReferenceLine(line: string): boolean {
  return /^\[\d+\]\s+/.test(line)
    || /^\d+\.\s+\[/.test(line)
    || /^[-*]\s+/.test(line);
}

function getAnswerText(text: string): string {
  return splitReferenceSection(text).answer;
}

function getReferenceText(text: string): string {
  return splitReferenceSection(text).reference;
}

async function onHealthCheck(): Promise<void> {
  try {
    healthText.value = "检测中...";
    const result = await checkHealth();
    healthText.value = `后端在线：${result.trim() || "OK"}`;
  } catch (error) {
    healthText.value = `后端不可用：${extractErrorMessage(error)}`;
  }
}

function onSend(): void {
  if (!inputMessage.value || isStreaming.value) {
    return;
  }

  const currentInput = inputMessage.value;
  inputMessage.value = "";
  appendMessage({ role: "user", text: currentInput });

  if (selectedMode.value === "ticket"
      || selectedMode.value === "team-chat"
      || selectedMode.value === "tool-chat"
      || selectedMode.value === "mcp") {
    handleNonStreamRequest(currentInput);
    return;
  }

  handleStreamRequest(currentInput);
}

async function handleNonStreamRequest(currentInput: string): Promise<void> {
  isStreaming.value = true;
  try {
    if (selectedMode.value === "team-chat") {
      const teamResponse = await requestDigitalTeam(
        currentInput,
        chatId.value || "default-user",
        topK
      );
      appendMessage({
        role: "assistant",
        text: teamResponse.answer,
        teamResponse
      });
      await nextTick();
      scrollToBottom();
      return;
    }

    const responseText = await requestEnterprise(
      selectedMode.value,
      currentInput,
      chatId.value || "default-user"
    );
    appendMessage({
      role: "assistant",
      text: responseText,
      isJson: selectedMode.value === "ticket"
    });
    await nextTick();
    scrollToBottom();
  } catch (error) {
    appendMessage({
      role: "assistant",
      text: `调用失败：${extractErrorMessage(error)}`
    });
  } finally {
    isStreaming.value = false;
  }
}

function handleStreamRequest(currentInput: string): void {
  const assistantMessageId = crypto.randomUUID();
  const assistantMessage: ChatMessage = {
    id: assistantMessageId,
    role: "assistant",
    text: "",
    streaming: true
  };
  messages.value.push(assistantMessage);

  isStreaming.value = true;

  if (currentStreamController) {
    currentStreamController.close();
  }
  currentStreamController = createStreamController();

  currentStreamController.stream(
    selectedMode.value,
    currentInput,
    chatId.value || "default-user",
    {
      onChunk: (chunk: string) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text += chunk;
          nextTick(() => {
            scrollToBottom();
          });
        }
      },
      onDone: (fullContent: string) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text = fullContent;
          msg.streaming = false;
        }
        isStreaming.value = false;
        nextTick(() => {
          scrollToBottom();
        });
      },
      onError: (error: Error) => {
        const msg = messages.value.find((m) => m.id === assistantMessageId);
        if (msg) {
          msg.text = `流式请求失败：${error.message}`;
          msg.streaming = false;
        }
        isStreaming.value = false;
      }
    },
      topK
  );
}

function scrollToBottom(): void {
  const messageList = messageListRef.value;
  if (messageList) {
    messageList.scrollTop = messageList.scrollHeight;
  }
}

function extractErrorMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "未知错误";
}

onMounted(async () => {
  if (!getAuthToken()) {
    return;
  }
  try {
    currentUser.value = await getCurrentUser();
    chatId.value = currentUser.value.username;
    await onHealthCheck();
  } catch {
    clearAuthToken();
    currentUser.value = null;
  }
});

onUnmounted(() => {
  if (currentStreamController) {
    currentStreamController.close();
  }
});
</script>

<style scoped>
.auth-page {
  grid-template-columns: minmax(320px, 460px);
  align-items: center;
  justify-content: center;
}

.login-panel {
  z-index: 1;
  padding: 28px;
}

.login-brand {
  margin-bottom: 22px;
}

.login-form {
  display: grid;
  gap: 14px;
}

.login-form .primary-btn {
  width: 100%;
}

.login-error {
  margin: 0;
  color: #f87171;
  font-size: 13px;
  line-height: 1.5;
}

.demo-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.demo-users span {
  color: #64748b;
  font-size: 12px;
}

.demo-users button {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  padding: 7px 10px;
  color: #cbd5e1;
  background: rgba(30, 41, 59, 0.52);
  font-size: 12px;
}

.demo-users button:hover {
  border-color: rgba(96, 165, 250, 0.45);
  color: #f8fafc;
}

.user-card {
  display: grid;
  gap: 12px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-profile span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #10b981);
  font-weight: 700;
}

.user-profile strong {
  display: block;
  color: #f8fafc;
  font-size: 14px;
}

.user-profile p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}

.logout-btn {
  width: 100%;
}

.team-cockpit {
  display: grid;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(15, 23, 42, 0.28);
}

.agent-roster {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.agent-tile {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 12px;
  padding: 10px;
  background: rgba(30, 41, 59, 0.42);
  opacity: 0.72;
}

.agent-tile.active {
  opacity: 1;
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(37, 99, 235, 0.12);
}

.agent-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 10px;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
}

.agent-tile.knowledge .agent-avatar { background: #2563eb; }
.agent-tile.responder .agent-avatar { background: #7c3aed; }
.agent-tile.analyzer .agent-avatar { background: #db2777; }
.agent-tile.router .agent-avatar { background: #059669; }
.agent-tile.tracker .agent-avatar { background: #d97706; }

.agent-tile strong {
  display: block;
  overflow: hidden;
  color: #f8fafc;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-tile p {
  margin: 2px 0 0;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-strip,
.mcp-strip {
  display: grid;
  gap: 8px;
}

.route-strip {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mcp-strip {
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  align-items: stretch;
}

.route-strip div,
.mcp-strip div {
  border-radius: 10px;
  padding: 9px 10px;
  background: rgba(15, 23, 42, 0.58);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.route-strip span,
.mcp-strip span {
  display: block;
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.route-strip strong,
.mcp-strip strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: #e2e8f0;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-text.low { color: #34d399; }
.risk-text.medium { color: #fbbf24; }
.risk-text.high { color: #f87171; }

.fold-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.4);
  overflow: hidden;
}

.fold-card summary {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 48px;
  padding: 12px 14px;
  cursor: pointer;
  list-style: none;
  user-select: none;
}

.fold-card summary::-webkit-details-marker {
  display: none;
}

.fold-card summary::before {
  content: "+";
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  color: #93c5fd;
  background: rgba(59, 130, 246, 0.14);
  border: 1px solid rgba(96, 165, 250, 0.28);
  font-weight: 700;
}

.fold-card[open] summary::before {
  content: "-";
}

.fold-card summary span {
  color: #60a5fa;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.fold-card summary strong {
  color: #f8fafc;
  font-size: 13px;
}

.fold-card summary em {
  border-radius: 999px;
  padding: 5px 9px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.16);
  border: 1px solid rgba(96, 165, 250, 0.25);
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.text-reference-card {
  margin-top: 12px;
}

.reference-markdown {
  padding: 0 14px 14px;
}

.thinking-indicator {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 16px;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #60a5fa;
  animation: thinking 1.4s ease-in-out infinite;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinking {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .agent-roster,
  .route-strip,
  .mcp-strip {
    grid-template-columns: 1fr;
  }
}
</style>
