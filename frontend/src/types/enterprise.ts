export type ReadyCapability = "team-chat" | "chat" | "rag-chat" | "ticket" | "tool-chat" | "mcp";

export type CapabilityStatus = "ready" | "planned";

export interface CapabilityDefinition {
  key: ReadyCapability;
  name: string;
  description: string;
  status: CapabilityStatus;
}

export interface TicketResponse {
  employeeName: string;
  department: string;
  requirementType: string;
  actionItems: string[];
}

export interface SentimentSignal {
  score: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | string;
  signals: string[];
}

export interface RouteDecision {
  routeType: string;
  priority: string;
  assignee: string;
  reason: string;
}

export interface CitationCard {
  sourceFile: string;
  sourceType: string;
  score: number;
  highlight: string;
  chunkIndex: number;
}

export interface AgentStep {
  code: string;
  name: string;
  title: string;
  status: string;
  summary: string;
  output: string;
}

export interface TrackerInsight {
  knowledgeGap: boolean;
  actions: string[];
}

export interface DigitalTeamResponse {
  success: boolean;
  chatId: string;
  query: string;
  rewrittenQuery: string;
  intentType: string;
  sentiment: SentimentSignal;
  route: RouteDecision;
  citations: CitationCard[];
  steps: AgentStep[];
  tracker: TrackerInsight;
  answer: string;
  elapsedMs: number;
  createdAt: string;
}

export interface McpStatus {
  enabled: boolean;
  serverName: string;
  serverType: string;
  sseEndpoint: string;
  messageEndpoint: string;
  toolCount: number;
  domains: string[];
  checkedAt: string;
}

export interface McpToolCard {
  name: string;
  domain: string;
  description: string;
  inputSchema: string;
}

export interface McpChatResponse {
  success: boolean;
  serverName: string;
  selectedTool: string | null;
  content: string;
  trace: string;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant" | "system";
  text: string;
  isJson?: boolean;
  streaming?: boolean;
  teamResponse?: DigitalTeamResponse;
}

export interface UserProfile {
  userId: string;
  username: string;
  employeeId: string | null;
  displayName: string;
  role: "EMPLOYEE" | "HR" | "ADMIN" | string;
}

export interface LoginResponse {
  token: string;
  expiresAt: number;
  user: UserProfile;
}
