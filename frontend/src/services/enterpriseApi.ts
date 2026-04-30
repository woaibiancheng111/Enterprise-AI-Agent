import axios from "axios";
import type {
  DigitalTeamResponse,
  EmployeeListResponse,
  EmployeeOverviewResponse,
  LoginResponse,
  McpChatResponse,
  McpStatus,
  McpToolCard,
  ReviewRequest,
  ReviewResponse,
  ReadyCapability,
  TicketResponse,
  UserProfile,
  WorkflowApplicationListResponse
} from "../types/enterprise";

const API_BASE_PATH = import.meta.env.VITE_API_BASE_PATH || "/api";
const TOKEN_STORAGE_KEY = "enterprise_ai_agent_token";

const http = axios.create({
  baseURL: API_BASE_PATH,
  timeout: 60000
});

http.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getAuthToken(): string {
  return localStorage.getItem(TOKEN_STORAGE_KEY) || "";
}

export function setAuthToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearAuthToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>("/auth/login", { username, password });
  setAuthToken(data.token);
  return data;
}

export async function getCurrentUser(): Promise<UserProfile> {
  const { data } = await http.get<UserProfile>("/auth/me");
  return data;
}

const endpointMap: Record<ReadyCapability, string> = {
  "team-chat": "/enterprise/team-chat",
  chat: "/enterprise/chat",
  "rag-chat": "/enterprise/rag-chat",
  ticket: "/enterprise/ticket",
  "tool-chat": "/enterprise/tool-chat",
  mcp: "/mcp/chat"
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
  if (typeof data === "object" && data !== null && "content" in data) {
    const mcpData = data as unknown as McpChatResponse;
    return `${mcpData.content}\n\n---\n${mcpData.trace}`;
  }
  return JSON.stringify(data, null, 2);
}

export async function requestDigitalTeam(
  message: string,
  chatId: string,
  topK: number
): Promise<DigitalTeamResponse> {
  const { data } = await http.get<DigitalTeamResponse>("/enterprise/team-chat", {
    params: {
      message,
      chatId,
      topK
    }
  });
  return data;
}

export type StreamCallback = (chunk: string) => void;
export type StreamDoneCallback = (fullContent: string) => void;
export type StreamErrorCallback = (error: Error) => void;

const streamEndpointMap: Record<ReadyCapability, string> = {
  "team-chat": "/enterprise/team-chat",
  chat: "/enterprise/chat/stream",
  "rag-chat": "/enterprise/rag-chat/stream",
  ticket: "/enterprise/ticket",
  "tool-chat": "/enterprise/tool-chat/stream",
  mcp: "/mcp/chat"
};

export async function getMcpStatus(): Promise<McpStatus> {
  const { data } = await http.get<McpStatus>("/mcp/status");
  return data;
}

export async function listMcpTools(): Promise<McpToolCard[]> {
  const { data } = await http.get<{ success: boolean; tools: McpToolCard[] }>("/mcp/tools");
  return data.tools;
}

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
    const token = getAuthToken();
    if (token) {
      url += `&access_token=${encodeURIComponent(token)}`;
    }
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

// ==================== 审批管理 API ====================

export async function listWorkflowApplications(
  type: "all" | "leave" | "reimbursement",
  status: "all" | "PENDING" | "APPROVED" | "REJECTED"
): Promise<WorkflowApplicationListResponse> {
  const { data } = await http.get<WorkflowApplicationListResponse>("/workflow/applications", {
    params: { type, status }
  });
  return data;
}

export async function reviewLeaveApplication(
  applicationId: string,
  request: ReviewRequest
): Promise<ReviewResponse> {
  const { data } = await http.post<ReviewResponse>(`/workflow/leave/${encodeURIComponent(applicationId)}/review`, request);
  return data;
}

export async function reviewReimbursementApplication(
  applicationId: string,
  request: ReviewRequest
): Promise<ReviewResponse> {
  const { data } = await http.post<ReviewResponse>(
    `/workflow/reimbursement/${encodeURIComponent(applicationId)}/review`,
    request
  );
  return data;
}

// ==================== 员工视图 API ====================

export async function listEmployees(
  keyword: string = "",
  department: string = "all"
): Promise<EmployeeListResponse> {
  const { data } = await http.get<EmployeeListResponse>("/employees", {
    params: { keyword, department }
  });
  return data;
}

export async function getMyEmployeeOverview(): Promise<EmployeeOverviewResponse> {
  const { data } = await http.get<EmployeeOverviewResponse>("/employees/me/overview");
  return data;
}

export async function getEmployeeOverview(employeeId: string): Promise<EmployeeOverviewResponse> {
  const { data } = await http.get<EmployeeOverviewResponse>(`/employees/${encodeURIComponent(employeeId)}/overview`);
  return data;
}
