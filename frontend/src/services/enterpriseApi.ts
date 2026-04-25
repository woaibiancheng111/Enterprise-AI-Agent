import axios from "axios";
import type { ReadyCapability, TicketResponse } from "../types/enterprise";

const API_BASE_PATH = import.meta.env.VITE_API_BASE_PATH || "/api";

const http = axios.create({
  baseURL: API_BASE_PATH,
  timeout: 60000
});

const endpointMap: Record<ReadyCapability, string> = {
  chat: "/enterprise/chat",
  "rag-chat": "/enterprise/rag-chat",
  ticket: "/enterprise/ticket",
  "tool-chat": "/enterprise/tool-chat"
};

export async function checkHealth(): Promise<string> {
  const { data } = await http.get<string>("/enterprise/health");
  return data;
}

export async function requestEnterprise(
  capability: ReadyCapability,
  message: string,
  chatId: string
): Promise<string> {
  const endpoint = endpointMap[capability];
  const { data } = await http.get<string | TicketResponse>(endpoint, {
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

export type StreamCallback = (chunk: string) => void;
export type StreamDoneCallback = (fullContent: string) => void;
export type StreamErrorCallback = (error: Error) => void;

const streamEndpointMap: Record<ReadyCapability, string> = {
  chat: "/enterprise/chat/stream",
  "rag-chat": "/enterprise/rag-chat/stream",
  ticket: "/enterprise/ticket",
  "tool-chat": "/enterprise/tool-chat/stream"
};

export interface StreamOptions {
  onChunk?: StreamCallback;
  onDone?: StreamDoneCallback;
  onError?: StreamErrorCallback;
}

export function createStreamController() {
  let eventSource: EventSource | null = null;
  let fullContent = "";

  const stream = (
    capability: ReadyCapability,
    message: string,
    chatId: string,
    options: StreamOptions = {},
    topK: number = 5
  ): void => {
    const endpoint = streamEndpointMap[capability];
    if (!endpoint) {
      options.onError?.(new Error("流式输出暂不支持该能力"));
      return;
    }

    let url = `${API_BASE_PATH}${endpoint}?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`;
    // rag-chat 模式添加 topK 参数
    if (capability === "rag-chat") {
      url += `&topK=${topK}`;
    }
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

// ==================== 知识库管理 API ====================

export interface DocumentListResponse {
  success: boolean;
  documents: string[];
  count: number;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  filename?: string;
  chunks?: number;
}

export async function listDocuments(): Promise<DocumentListResponse> {
  const { data } = await http.get<DocumentListResponse>("/knowledge/list");
  return data;
}

export async function uploadDocument(file: File): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await http.post<UploadResponse>("/knowledge/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
  return data;
}

export async function deleteDocument(filename: string): Promise<{ success: boolean; message: string }> {
  const { data } = await http.delete<{ success: boolean; message: string }>(`/knowledge/file/${encodeURIComponent(filename)}`);
  return data;
}

export async function reloadKnowledgeBase(): Promise<{ success: boolean; message: string }> {
  const { data } = await http.post<{ success: boolean; message: string }>("/knowledge/reload");
  return data;
}
