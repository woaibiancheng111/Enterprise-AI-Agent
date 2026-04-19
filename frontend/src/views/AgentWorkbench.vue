<template>
  <div class="page">
    <div class="bg-orb orb-one"></div>
    <div class="bg-orb orb-two"></div>

    <aside class="panel left-panel">
      <div class="brand">
        <span class="brand-dot"></span>
        <div>
          <h1>Health AI Agent</h1>
          <p class="subtitle">企业助手工作台</p>
        </div>
      </div>

      <section class="card">
        <h2>会话参数</h2>
        <label class="field">
          <span>会话 ID</span>
          <input v-model.trim="chatId" type="text" placeholder="default-user" />
        </label>
        <label class="field">
          <span>引用数量</span>
          <input v-model.number="topK" type="number" min="1" max="10" placeholder="5" />
        </label>
        <div class="field">
          <span>当前能力</span>
          <div class="mode-grid">
            <button
              v-for="item in readyCapabilities"
              :key="item.key"
              type="button"
              :class="['mode-btn', { active: selectedMode === item.key }]"
              @click="selectedMode = item.key as ReadyCapability"
            >
              {{ item.name }}
            </button>
          </div>
        </div>
        <div class="health-row">
          <button type="button" class="secondary-btn" @click="onHealthCheck">
            健康检查
          </button>
          <p :class="['status-text', healthStatusClass]">{{ healthText }}</p>
        </div>
      </section>

      <!-- 知识库上传组件 -->
      <section class="card">
        <h2>知识库管理</h2>
        <KnowledgeBaseUpload />
      </section>

      <section class="card">
        <h2>能力清单</h2>
        <ul class="capability-list">
          <li v-for="item in capabilities" :key="item.key">
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.description }}</p>
            </div>
            <span :class="['badge', item.status]">
              {{ item.status === "ready" ? "可用" : "规划中" }}
            </span>
          </li>
        </ul>
      </section>
    </aside>

    <main class="panel chat-panel">
      <header class="chat-header">
        <div>
          <h2>{{ currentCapability?.name }}</h2>
          <p>{{ currentCapability?.description }}</p>
        </div>
        <div class="header-actions">
          <span class="mode-chip">{{ selectedMode }}</span>
          <span v-if="isStreaming" class="streaming-chip">
            <span class="streaming-dot"></span>
            流式输出中
          </span>
        </div>
      </header>

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
          <div
            v-else-if="message.role === 'assistant'"
            class="markdown-body"
            v-html="renderAssistantMarkdown(message.text)"
          ></div>
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
import { computed, ref, nextTick, onUnmounted } from "vue";
import { CAPABILITIES } from "../config/capabilities";
import {
  checkHealth,
  requestEnterprise,
  createStreamController
} from "../services/enterpriseApi";
import KnowledgeBaseUpload from "../components/KnowledgeBaseUpload.vue";
import type { ChatMessage, ReadyCapability } from "../types/enterprise";

const capabilities = CAPABILITIES;
const readyCapabilities = capabilities.filter((item) => item.status === "ready");
marked.setOptions({
  gfm: true,
  breaks: true
});

const selectedMode = ref<ReadyCapability>("chat");
const chatId = ref("default-user");
const inputMessage = ref("");
const isStreaming = ref(false);
const topK = ref(5);
const healthText = ref("后端状态未检查");
const messages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: "system",
    text: "欢迎使用企业 AI 助手控制台。请选择能力后发送问题。"
  }
]);

// DOM 引用
const messageListRef = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
let currentStreamController: ReturnType<typeof createStreamController> | null = null;

const roleLabelMap: Record<ChatMessage["role"], string> = {
  user: "我",
  assistant: "AI 助手",
  system: "系统"
};

const currentCapability = computed(() =>
  capabilities.find((item) => item.key === selectedMode.value)
);

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

function renderAssistantMarkdown(markdown: string): string {
  if (!markdown) return "";
  const rendered = marked.parse(markdown) as string;
  return DOMPurify.sanitize(rendered);
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

  // 工单模式使用非流式请求
  if (selectedMode.value === "ticket") {
    handleNonStreamRequest(currentInput);
    return;
  }

  // 普通问答和RAG问答使用流式请求
  handleStreamRequest(currentInput);
}

async function handleNonStreamRequest(currentInput: string): Promise<void> {
  isStreaming.value = true;
  try {
    const responseText = await requestEnterprise(
      selectedMode.value,
      currentInput,
      chatId.value || "default-user"
    );
    appendMessage({
      role: "assistant",
      text: responseText,
      isJson: true
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

  // 关闭之前的流连接
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
    topK.value
  );
}

function scrollToBottom(): void {
  const messageList = messageListRef.value;
  if (messageList) {
    messageList.scrollTop = messageList.scrollHeight;
  }
}

function extractErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "未知错误";
}

// 组件卸载时关闭所有流式连接
onUnmounted(() => {
  if (currentStreamController) {
    currentStreamController.close();
  }
});
</script>

<style scoped>
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
</style>
