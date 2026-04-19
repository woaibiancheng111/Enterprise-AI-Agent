import DOMPurify from "dompurify";
import { marked } from "marked";
import { computed, ref, nextTick, onUnmounted } from "vue";
import { CAPABILITIES } from "../config/capabilities";
import { checkHealth, requestEnterprise, createStreamController } from "../services/enterpriseApi";
import KnowledgeBaseUpload from "../components/KnowledgeBaseUpload.vue";
const capabilities = CAPABILITIES;
const readyCapabilities = capabilities.filter((item) => item.status === "ready");
marked.setOptions({
    gfm: true,
    breaks: true
});
const selectedMode = ref("chat");
const chatId = ref("default-user");
const inputMessage = ref("");
const isStreaming = ref(false);
const healthText = ref("后端状态未检查");
const messages = ref([
    {
        id: crypto.randomUUID(),
        role: "system",
        text: "欢迎使用企业 AI 助手控制台。请选择能力后发送问题。"
    }
]);
// DOM 引用
const messageListRef = ref(null);
const textareaRef = ref(null);
let currentStreamController = null;
const roleLabelMap = {
    user: "我",
    assistant: "AI 助手",
    system: "系统"
};
const currentCapability = computed(() => capabilities.find((item) => item.key === selectedMode.value));
const healthStatusClass = computed(() => {
    if (healthText.value.includes("在线")) {
        return "online";
    }
    if (healthText.value.includes("不可用")) {
        return "offline";
    }
    return "idle";
});
const quickPromptsMap = {
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
function appendMessage(message) {
    messages.value.push({
        id: crypto.randomUUID(),
        ...message
    });
}
function onUsePrompt(prompt) {
    inputMessage.value = prompt;
    textareaRef.value?.focus();
}
function renderAssistantMarkdown(markdown) {
    if (!markdown)
        return "";
    const rendered = marked.parse(markdown);
    return DOMPurify.sanitize(rendered);
}
async function onHealthCheck() {
    try {
        healthText.value = "检测中...";
        const result = await checkHealth();
        healthText.value = `后端在线：${result.trim() || "OK"}`;
    }
    catch (error) {
        healthText.value = `后端不可用：${extractErrorMessage(error)}`;
    }
}
function onSend() {
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
async function handleNonStreamRequest(currentInput) {
    isStreaming.value = true;
    try {
        const responseText = await requestEnterprise(selectedMode.value, currentInput, chatId.value || "default-user");
        appendMessage({
            role: "assistant",
            text: responseText,
            isJson: true
        });
        await nextTick();
        scrollToBottom();
    }
    catch (error) {
        appendMessage({
            role: "assistant",
            text: `调用失败：${extractErrorMessage(error)}`
        });
    }
    finally {
        isStreaming.value = false;
    }
}
function handleStreamRequest(currentInput) {
    const assistantMessageId = crypto.randomUUID();
    const assistantMessage = {
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
    currentStreamController.stream(selectedMode.value, currentInput, chatId.value || "default-user", {
        onChunk: (chunk) => {
            const msg = messages.value.find((m) => m.id === assistantMessageId);
            if (msg) {
                msg.text += chunk;
                // 流式更新时滚动
                nextTick(() => {
                    scrollToBottom();
                });
            }
        },
        onDone: (fullContent) => {
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
        onError: (error) => {
            const msg = messages.value.find((m) => m.id === assistantMessageId);
            if (msg) {
                msg.text = `流式请求失败：${error.message}`;
                msg.streaming = false;
            }
            isStreaming.value = false;
        }
    });
}
function scrollToBottom() {
    const messageList = messageListRef.value;
    if (messageList) {
        messageList.scrollTop = messageList.scrollHeight;
    }
}
function extractErrorMessage(error) {
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['thinking-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-dot']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "bg-orb orb-one" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "bg-orb orb-two" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "panel left-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "brand-dot" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "subtitle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.chatId),
    type: "text",
    placeholder: "default-user",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mode-grid" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.readyCapabilities))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectedMode = item.key;
            } },
        key: (item.key),
        type: "button",
        ...{ class: (['mode-btn', { active: __VLS_ctx.selectedMode === item.key }]) },
    });
    (item.name);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "health-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onHealthCheck) },
    type: "button",
    ...{ class: "secondary-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: (['status-text', __VLS_ctx.healthStatusClass]) },
});
(__VLS_ctx.healthText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
/** @type {[typeof KnowledgeBaseUpload, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(KnowledgeBaseUpload, new KnowledgeBaseUpload({}));
const __VLS_1 = __VLS_0({}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
    ...{ class: "capability-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.capabilities))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
        key: (item.key),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (item.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.description);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: (['badge', item.status]) },
    });
    (item.status === "ready" ? "可用" : "规划中");
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "panel chat-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "chat-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
(__VLS_ctx.currentCapability?.name);
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
(__VLS_ctx.currentCapability?.description);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "mode-chip" },
});
(__VLS_ctx.selectedMode);
if (__VLS_ctx.isStreaming) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "streaming-chip" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "streaming-dot" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "quick-prompts" },
});
for (const [prompt] of __VLS_getVForSourceType((__VLS_ctx.quickPrompts))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.onUsePrompt(prompt);
            } },
        key: (prompt),
        type: "button",
        ...{ class: "prompt-chip" },
    });
    (prompt);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "message-list" },
    ref: "messageListRef",
});
/** @type {typeof __VLS_ctx.messageListRef} */ ;
for (const [message] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
        key: (message.id),
        ...{ class: (['message', message.role, { streaming: message.streaming }]) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({});
    (__VLS_ctx.roleLabelMap[message.role]);
    if (message.isJson) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({});
        (message.text);
    }
    else if (message.role === 'assistant') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "markdown-body" },
        });
        __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.renderAssistantMarkdown(message.text)) }, null, null);
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        (message.text);
    }
    if (message.streaming) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "typing-cursor" },
        });
    }
}
if (__VLS_ctx.isStreaming) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "thinking-indicator" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "thinking-dot" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "thinking-dot" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "thinking-dot" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.footer, __VLS_intrinsicElements.footer)({
    ...{ class: "chat-input" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    ...{ onKeydown: (__VLS_ctx.onSend) },
    value: (__VLS_ctx.inputMessage),
    ref: "textareaRef",
    placeholder: "请输入你的问题，例如：我想申请年假需要什么流程？",
    disabled: (__VLS_ctx.isStreaming),
});
/** @type {typeof __VLS_ctx.textareaRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "input-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "input-hint" },
});
(__VLS_ctx.isStreaming ? '等待回复...' : 'Enter 发送，Shift + Enter 换行');
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onSend) },
    type: "button",
    ...{ class: "primary-btn" },
    disabled: (__VLS_ctx.isStreaming || !__VLS_ctx.inputMessage),
});
(__VLS_ctx.isStreaming ? '处理中...' : '发送');
/** @type {__VLS_StyleScopedClasses['page']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-orb']} */ ;
/** @type {__VLS_StyleScopedClasses['orb-one']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-orb']} */ ;
/** @type {__VLS_StyleScopedClasses['orb-two']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['left-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['health-row']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-list']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-header']} */ ;
/** @type {__VLS_StyleScopedClasses['header-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['streaming-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['streaming-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['quick-prompts']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['message-list']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['typing-cursor']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-input']} */ ;
/** @type {__VLS_StyleScopedClasses['input-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['input-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            KnowledgeBaseUpload: KnowledgeBaseUpload,
            capabilities: capabilities,
            readyCapabilities: readyCapabilities,
            selectedMode: selectedMode,
            chatId: chatId,
            inputMessage: inputMessage,
            isStreaming: isStreaming,
            healthText: healthText,
            messages: messages,
            messageListRef: messageListRef,
            textareaRef: textareaRef,
            roleLabelMap: roleLabelMap,
            currentCapability: currentCapability,
            healthStatusClass: healthStatusClass,
            quickPrompts: quickPrompts,
            onUsePrompt: onUsePrompt,
            renderAssistantMarkdown: renderAssistantMarkdown,
            onHealthCheck: onHealthCheck,
            onSend: onSend,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
