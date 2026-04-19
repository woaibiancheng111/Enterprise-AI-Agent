import axios from "axios";
const API_BASE_PATH = import.meta.env.VITE_API_BASE_PATH || "/api";
const http = axios.create({
    baseURL: API_BASE_PATH,
    timeout: 60000
});
const endpointMap = {
    chat: "/enterprise/chat",
    "rag-chat": "/enterprise/rag-chat",
    ticket: "/enterprise/ticket"
};
export async function checkHealth() {
    const { data } = await http.get("/enterprise/health");
    return data;
}
export async function requestEnterprise(capability, message, chatId) {
    const endpoint = endpointMap[capability];
    const { data } = await http.get(endpoint, {
        params: {
            message,
            chatId
        }
    });
    if (typeof data === "string") {
        return data;
    }
    return JSON.stringify(data, null, 2);
}
const streamEndpointMap = {
    chat: "/enterprise/chat/stream",
    "rag-chat": "/enterprise/rag-chat/stream",
    ticket: "/enterprise/ticket"
};
export function createStreamController() {
    let eventSource = null;
    let fullContent = "";
    const stream = (capability, message, chatId, options = {}) => {
        const endpoint = streamEndpointMap[capability];
        if (!endpoint) {
            options.onError?.(new Error("流式输出暂不支持该能力"));
            return;
        }
        const url = `${API_BASE_PATH}${endpoint}?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`;
        eventSource = new EventSource(url);
        eventSource.addEventListener("chunk", (e) => {
            const content = e.data;
            if (content !== "[DONE]") {
                fullContent += content;
                options.onChunk?.(content);
            }
        });
        eventSource.addEventListener("done", () => {
            options.onDone?.(fullContent);
            close();
        });
        eventSource.onerror = (e) => {
            console.error("SSE Error:", e);
            options.onError?.(new Error("流式请求失败"));
            close();
        };
    };
    const close = () => {
        eventSource?.close();
        eventSource = null;
    };
    return { stream, close };
}
export async function listDocuments() {
    const { data } = await http.get("/knowledge/list");
    return data;
}
export async function uploadDocument(file) {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await http.post("/knowledge/upload", formData, {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    });
    return data;
}
export async function deleteDocument(filename) {
    const { data } = await http.delete(`/knowledge/file/${encodeURIComponent(filename)}`);
    return data;
}
export async function reloadKnowledgeBase() {
    const { data } = await http.post("/knowledge/reload");
    return data;
}
